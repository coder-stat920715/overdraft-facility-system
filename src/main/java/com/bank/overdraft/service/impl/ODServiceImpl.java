package com.bank.overdraft.service.impl;

import com.bank.overdraft.domain.OverdraftAccount;
import com.bank.overdraft.domain.Transaction;
import com.bank.overdraft.dto.AccountResponse;
import com.bank.overdraft.dto.OpenAccountRequest;
import com.bank.overdraft.dto.StatementResponse;
import com.bank.overdraft.dto.TransactionRequest;
import com.bank.overdraft.dto.TransactionResponse;
import com.bank.overdraft.dto.UtilizationBreakdown;
import com.bank.overdraft.enums.TransactionType;
import com.bank.overdraft.exception.AccountNotFoundException;
import com.bank.overdraft.exception.DuplicateTransactionException;
import com.bank.overdraft.mapper.OverdraftMapper;
import com.bank.overdraft.repository.DailyInterestLedgerRepository;
import com.bank.overdraft.repository.OverdraftAccountRepository;
import com.bank.overdraft.repository.TransactionRepository;
import com.bank.overdraft.service.InterestCalculationService;
import com.bank.overdraft.service.ODService;
import com.bank.overdraft.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ODServiceImpl implements ODService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final OverdraftAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DailyInterestLedgerRepository ledgerRepository;
    private final InterestCalculationService interestCalculationService;
    private final OverdraftMapper mapper;
    private final Clock clock;

    @Override
    @Transactional
    public AccountResponse openAccount(OpenAccountRequest request) {
        int cycleDay = request.billingCycleDay() == null ? 1 : request.billingCycleDay();
        OverdraftAccount account = OverdraftAccount.open(
                generateAccountNumber(request),
                request.customerId(),
                request.bankName(),
                request.sanctionedLimit(),
                request.annualInterestRate(),
                cycleDay,
                LocalDateTime.now(clock));
        OverdraftAccount saved = accountRepository.save(account);
        log.info("Opened OD account {} for customer {} with limit {}", saved.getAccountNumber(),
                saved.getCustomerId(), saved.getSanctionedLimit());
        return mapper.toAccountResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse drawdown(String accountNumber, TransactionRequest request) {
        OverdraftAccount account = lockAccount(accountNumber);
        guardDuplicate(account, request.referenceId());
        account.drawdown(request.amount());
        Transaction txn = Transaction.of(account, TransactionType.DRAWDOWN, request.amount(), request.referenceId(),
                descriptionOrDefault(request.description(), "Overdraft drawdown"), LocalDateTime.now(clock));
        return mapper.toTransactionResponse(transactionRepository.save(txn));
    }

    @Override
    @Transactional
    public TransactionResponse repay(String accountNumber, TransactionRequest request) {
        OverdraftAccount account = lockAccount(accountNumber);
        guardDuplicate(account, request.referenceId());
        account.repay(request.amount());
        Transaction txn = Transaction.of(account, TransactionType.REPAYMENT, request.amount(), request.referenceId(),
                descriptionOrDefault(request.description(), "Overdraft repayment"), LocalDateTime.now(clock));
        return mapper.toTransactionResponse(transactionRepository.save(txn));
    }

    @Override
    @Transactional(readOnly = true)
    public StatementResponse getStatement(String accountNumber, int limit) {
        OverdraftAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        Pageable newestFirst = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        Pageable latestDates = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "accrualDate"));

        LocalDate today = LocalDate.now(clock);
        BigDecimal utilizationPercent = account.getSanctionedLimit().signum() == 0
                ? BigDecimal.ZERO.setScale(MoneyUtil.SCALE_BILLING)
                : account.getUtilizedAmount().multiply(HUNDRED)
                .divide(account.getSanctionedLimit(), MoneyUtil.SCALE_BILLING, MoneyUtil.ROUNDING);

        UtilizationBreakdown breakdown = new UtilizationBreakdown(
                MoneyUtil.billing(account.getSanctionedLimit()),
                MoneyUtil.billing(account.getUtilizedAmount()),
                MoneyUtil.billing(account.getAvailableLimit()),
                utilizationPercent,
                account.getAccumulatedUnbilledInterest(),
                interestCalculationService.calculateDailyInterest(account.getUtilizedAmount(),
                        account.getAnnualInterestRate(), today),
                nextBillingDate(today, account.getBillingCycleDay()));

        return new StatementResponse(
                mapper.toAccountResponse(account),
                breakdown,
                mapper.toTransactionResponses(transactionRepository.findByAccountId(account.getId(), newestFirst)),
                mapper.toDailyInterestResponses(ledgerRepository.findByAccountId(account.getId(), latestDates)));
    }

    private OverdraftAccount lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void guardDuplicate(OverdraftAccount account, String referenceId) {
        if (referenceId != null && !referenceId.isBlank()
                && transactionRepository.existsByAccountIdAndReferenceId(account.getId(), referenceId)) {
            throw new DuplicateTransactionException(account.getAccountNumber(), referenceId);
        }
    }

    private String descriptionOrDefault(String description, String fallback) {
        return description == null || description.isBlank() ? fallback : description;
    }

    private LocalDate nextBillingDate(LocalDate today, int cycleDay) {
        LocalDate candidate = today.withDayOfMonth(cycleDay);
        return candidate.isAfter(today) ? candidate : candidate.plusMonths(1);
    }

    private String generateAccountNumber(OpenAccountRequest request) {
        for (int attempt = 0; attempt < 10; attempt++) {
            long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
            String candidate = request.bankName().getCode() + "OD" + suffix;
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique account number");
    }
}
