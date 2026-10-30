package com.bank.overdraft.service.impl;

import com.bank.overdraft.domain.DailyInterestLedger;
import com.bank.overdraft.domain.OverdraftAccount;
import com.bank.overdraft.dto.JobExecutionSummary;
import com.bank.overdraft.enums.AccountStatus;
import com.bank.overdraft.repository.DailyInterestLedgerRepository;
import com.bank.overdraft.repository.OverdraftAccountRepository;
import com.bank.overdraft.service.InterestCalculationService;
import com.bank.overdraft.service.InterestEngine;
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
public class InterestEngineImpl implements InterestEngine {

    static final String JOB_NAME = "EOD_INTEREST_ACCRUAL";

    private final OverdraftAccountRepository accountRepository;
    private final DailyInterestLedgerRepository ledgerRepository;
    private final InterestCalculationService interestCalculationService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Override
    public JobExecutionSummary runEndOfDayAccrual(LocalDate businessDate) {
        List<Long> accountIds = accountRepository.findIdsByStatus(AccountStatus.ACTIVE);
        int processed = 0;
        int skipped = 0;
        int failed = 0;
        BigDecimal total = MoneyUtil.zero();

        for (Long accountId : accountIds) {
            try {
                // one transaction per account: a failure never blocks or rolls back other accounts
                BigDecimal accrued = transactionTemplate.execute(status -> accrueForAccount(accountId, businessDate));
                if (accrued == null) {
                    skipped++;
                } else {
                    processed++;
                    total = total.add(accrued);
                }
            } catch (Exception ex) {
                failed++;
                log.error("EOD accrual failed for account id {} on {}", accountId, businessDate, ex);
            }
        }
        log.info("EOD accrual {} done: processed={}, skipped={}, failed={}, totalAccrued={}",
                businessDate, processed, skipped, failed, total);
        return new JobExecutionSummary(JOB_NAME, businessDate, processed, skipped, failed, MoneyUtil.internal(total));
    }

    /** Returns the accrued amount, or null when the account was skipped (not active / already accrued for the date). */
    private BigDecimal accrueForAccount(Long accountId, LocalDate businessDate) {
        OverdraftAccount account = accountRepository.findByIdForUpdate(accountId).orElse(null);
        if (account == null || account.getStatus() != AccountStatus.ACTIVE) {
            return null;
        }
        if (ledgerRepository.existsByAccountIdAndAccrualDate(accountId, businessDate)) {
            return null;
        }
        BigDecimal interest = interestCalculationService.calculateDailyInterest(
                account.getUtilizedAmount(), account.getAnnualInterestRate(), businessDate);

        ledgerRepository.save(DailyInterestLedger.builder()
                .account(account)
                .accrualDate(businessDate)
                .utilizedBalance(account.getUtilizedAmount())
                .interestRate(account.getAnnualInterestRate())
                .accruedAmount(interest)
                .billed(false)
                .createdAt(LocalDateTime.now(clock))
                .build());

        // principal (utilizedAmount) is deliberately untouched: no intra-month compounding
        account.addUnbilledInterest(interest);
        accountRepository.save(account);
        return interest;
    }
}
