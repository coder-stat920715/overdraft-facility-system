package com.bank.overdraft.integration;

import com.bank.overdraft.domain.DailyInterestLedger;
import com.bank.overdraft.domain.OverdraftAccount;
import com.bank.overdraft.domain.Transaction;
import com.bank.overdraft.dto.AccountResponse;
import com.bank.overdraft.dto.JobExecutionSummary;
import com.bank.overdraft.dto.OpenAccountRequest;
import com.bank.overdraft.dto.TransactionRequest;
import com.bank.overdraft.enums.BankType;
import com.bank.overdraft.enums.TransactionType;
import com.bank.overdraft.repository.DailyInterestLedgerRepository;
import com.bank.overdraft.repository.OverdraftAccountRepository;
import com.bank.overdraft.repository.TransactionRepository;
import com.bank.overdraft.service.BillingEngine;
import com.bank.overdraft.service.InterestEngine;
import com.bank.overdraft.service.ODService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OverdraftAccrualScenarioTest {

    @Autowired ODService odService;
    @Autowired InterestEngine interestEngine;
    @Autowired BillingEngine billingEngine;
    @Autowired OverdraftAccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired DailyInterestLedgerRepository ledgerRepository;

    @BeforeEach
    void cleanDatabase() {
        ledgerRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void multiDayScenarioAccruesAndBillsExactly() {
        LocalDate day1 = LocalDate.of(2026, 3, 29);
        LocalDate day2 = day1.plusDays(1);
        LocalDate day3 = day1.plusDays(2);

        AccountResponse opened = odService.openAccount(new OpenAccountRequest(
                "CUST-1001", BankType.HDFC, new BigDecimal("1500000.00"), new BigDecimal("12.00"), 1));
        String accountNumber = opened.accountNumber();
        assertThat(opened.availableLimit()).isEqualByComparingTo("1500000.00");

        // ---- Day 1: withdraw 300,000 and run EOD
        odService.drawdown(accountNumber, new TransactionRequest(new BigDecimal("300000.00"), "DD-1", "Working capital"));
        JobExecutionSummary eod1 = interestEngine.runEndOfDayAccrual(day1);
        assertThat(eod1.accountsProcessed()).isEqualTo(1);

        // ---- Day 2: balance unchanged
        interestEngine.runEndOfDayAccrual(day2);

        // ---- Day 3: repay 100,000 -> balance 200,000
        odService.repay(accountNumber, new TransactionRequest(new BigDecimal("100000.00"), "RP-1", "Part repayment"));
        interestEngine.runEndOfDayAccrual(day3);

        OverdraftAccount account = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        List<DailyInterestLedger> ledger = ledgerRepository.findByAccountIdOrderByAccrualDateAsc(account.getId());

        assertThat(ledger).hasSize(3);
        assertThat(ledger.get(0).getUtilizedBalance()).isEqualByComparingTo("300000");
        assertThat(ledger.get(0).getAccruedAmount()).isEqualByComparingTo("98.6301");
        assertThat(ledger.get(1).getAccruedAmount()).isEqualByComparingTo("98.6301");
        assertThat(ledger.get(2).getUtilizedBalance()).isEqualByComparingTo("200000");
        assertThat(ledger.get(2).getAccruedAmount()).isEqualByComparingTo("65.7534");

        BigDecimal cumulative = ledger.stream().map(DailyInterestLedger::getAccruedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(cumulative).isEqualByComparingTo("263.0136");
        assertThat(account.getAccumulatedUnbilledInterest()).isEqualByComparingTo("263.0136");

        // accrual never touches principal
        assertThat(account.getUtilizedAmount()).isEqualByComparingTo("200000.00");
        assertThat(account.getAvailableLimit()).isEqualByComparingTo("1300000.00");

        // ---- EOD is idempotent for a given date
        JobExecutionSummary rerun = interestEngine.runEndOfDayAccrual(day3);
        assertThat(rerun.accountsProcessed()).isZero();
        assertThat(rerun.accountsSkipped()).isEqualTo(1);
        assertThat(ledgerRepository.findByAccountIdOrderByAccrualDateAsc(account.getId())).hasSize(3);

        // ---- Monthly billing on 1 April capitalizes 263.01 (HALF_EVEN of 263.0136)
        JobExecutionSummary billing = billingEngine.runMonthlyBilling(LocalDate.of(2026, 4, 1), false);
        assertThat(billing.accountsProcessed()).isEqualTo(1);
        assertThat(billing.totalAmount()).isEqualByComparingTo("263.01");

        OverdraftAccount billed = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(billed.getUtilizedAmount()).isEqualByComparingTo("200263.01");
        assertThat(billed.getAvailableLimit()).isEqualByComparingTo("1299736.99");
        assertThat(billed.getAccumulatedUnbilledInterest()).isEqualByComparingTo("0");
        assertThat(ledgerRepository.findByAccountIdOrderByAccrualDateAsc(billed.getId()))
                .allMatch(DailyInterestLedger::isBilled);

        List<Transaction> interestTxns = transactionRepository
                .findByAccountIdAndTransactionType(billed.getId(), TransactionType.INTEREST_CHARGED);
        assertThat(interestTxns).hasSize(1);
        assertThat(interestTxns.get(0).getAmount()).isEqualByComparingTo("263.01");

        // ---- Billing is idempotent
        JobExecutionSummary secondBilling = billingEngine.runMonthlyBilling(LocalDate.of(2026, 4, 1), false);
        assertThat(secondBilling.accountsProcessed()).isZero();
        assertThat(accountRepository.findByAccountNumber(accountNumber).orElseThrow().getUtilizedAmount())
                .isEqualByComparingTo("200263.01");
    }

    @Test
    void billingHonoursCycleDayUnlessForced() {
        AccountResponse opened = odService.openAccount(new OpenAccountRequest(
                "CUST-2002", BankType.AXIS, new BigDecimal("500000.00"), new BigDecimal("12.00"), 15));
        odService.drawdown(opened.accountNumber(), new TransactionRequest(new BigDecimal("100000.00"), null, null));
        interestEngine.runEndOfDayAccrual(LocalDate.of(2026, 5, 10));

        // run date day-of-month (1) != cycle day (15) -> nothing billed
        assertThat(billingEngine.runMonthlyBilling(LocalDate.of(2026, 6, 1), false).accountsProcessed()).isZero();
        // matching cycle day -> billed
        assertThat(billingEngine.runMonthlyBilling(LocalDate.of(2026, 6, 15), false).accountsProcessed()).isEqualTo(1);

        OverdraftAccount account = accountRepository.findByAccountNumber(opened.accountNumber()).orElseThrow();
        // 100000 * 12 / 36500 = 32.8767 -> 32.88
        assertThat(account.getUtilizedAmount()).isEqualByComparingTo("100032.88");
    }
}
