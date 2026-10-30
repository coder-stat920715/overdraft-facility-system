package com.bank.overdraft.service.impl;

import com.bank.overdraft.domain.OverdraftAccount;
import com.bank.overdraft.domain.Transaction;
import com.bank.overdraft.dto.JobExecutionSummary;
import com.bank.overdraft.enums.TransactionType;
import com.bank.overdraft.repository.DailyInterestLedgerRepository;
import com.bank.overdraft.repository.OverdraftAccountRepository;
import com.bank.overdraft.repository.TransactionRepository;
import com.bank.overdraft.service.BillingEngine;
import com.bank.overdraft.service.InterestCalculationService;
import com.bank.overdraft.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingEngineImpl implements BillingEngine {

    static final String JOB_NAME = "MONTHLY_INTEREST_BILLING";

    private final OverdraftAccountRepository accountRepository;
    private final DailyInterestLedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final InterestCalculationService interestCalculationService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Override
    public JobExecutionSummary runMonthlyBilling(LocalDate runDate, boolean forceAllCycles) {
        LocalDate cutoff = runDate.minusDays(1);
        List<Long> accountIds = forceAllCycles
                ? ledgerRepository.findAccountIdsWithUnbilledInterest(cutoff)
                : ledgerRepository.findAccountIdsWithUnbilledInterestForCycleDay(cutoff, runDate.getDayOfMonth());

        int processed = 0;
        int skipped = 0;
        int failed = 0;
        BigDecimal total = BigDecimal.ZERO.setScale(MoneyUtil.SCALE_BILLING);

        for (Long accountId : accountIds) {
            try {
                BigDecimal billed = transactionTemplate.execute(status -> billAccount(accountId, cutoff));
                if (billed == null) {
                    skipped++;
                } else {
                    processed++;
                    total = total.add(billed);
                }
            } catch (Exception ex) {
                failed++;
                log.error("Monthly billing failed for account id {} (cutoff {})", accountId, cutoff, ex);
            }
        }
        log.info("Monthly billing run {} done: processed={}, skipped={}, failed={}, totalBilled={}",
                runDate, processed, skipped, failed, total);
        return new JobExecutionSummary(JOB_NAME, runDate, processed, skipped, failed, total);
    }

    /** Returns the billed (2 dp) amount, or null if there was nothing to bill. */
    private BigDecimal billAccount(Long accountId, LocalDate cutoff) {
        OverdraftAccount account = accountRepository.findByIdForUpdate(accountId).orElse(null);
        if (account == null) {
            return null;
        }
        BigDecimal rawTotal = ledgerRepository.sumUnbilledInterest(accountId, cutoff);
        if (rawTotal == null) {
            return null;
        }
        ledgerRepository.markBilled(accountId, cutoff);
        account.settleUnbilledInterest(rawTotal);

        BigDecimal billed = interestCalculationService.roundForBilling(rawTotal);
        if (billed.signum() > 0) {
            account.capitalizeInterest(billed);
            transactionRepository.save(Transaction.of(account, TransactionType.INTEREST_CHARGED, billed, null,
                    "Interest charged for accruals up to " + cutoff, LocalDateTime.now(clock)));
        }
        accountRepository.save(account);
        return billed;
    }
}
