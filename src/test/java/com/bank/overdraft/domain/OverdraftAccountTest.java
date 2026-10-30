package com.bank.overdraft.domain;

import com.bank.overdraft.enums.AccountStatus;
import com.bank.overdraft.enums.BankType;
import com.bank.overdraft.exception.InsufficientLimitException;
import com.bank.overdraft.exception.InvalidAccountStateException;
import com.bank.overdraft.exception.RepaymentExceedsOutstandingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OverdraftAccountTest {

    private OverdraftAccount account;

    @BeforeEach
    void setUp() {
        account = OverdraftAccount.open("HDFCOD1", "CUST-1", BankType.HDFC, new BigDecimal("1500000"),
                new BigDecimal("12"), 1, LocalDateTime.now());
    }

    @Test
    void newAccountStartsWithZeroUtilization() {
        assertThat(account.getUtilizedAmount()).isEqualByComparingTo("0.00");
        assertThat(account.getAvailableLimit()).isEqualByComparingTo("1500000.00");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void invariantAvailableEqualsSanctionedMinusUtilized() {
        account.drawdown(new BigDecimal("300000"));
        assertThat(account.getAvailableLimit()).isEqualByComparingTo("1200000");
        account.repay(new BigDecimal("100000"));
        assertThat(account.getUtilizedAmount()).isEqualByComparingTo("200000");
        assertThat(account.getAvailableLimit())
                .isEqualByComparingTo(account.getSanctionedLimit().subtract(account.getUtilizedAmount()));
    }

    @Test
    void drawdownBeyondAvailableLimitIsRejected() {
        assertThatThrownBy(() -> account.drawdown(new BigDecimal("1500000.01")))
                .isInstanceOf(InsufficientLimitException.class);
        account.drawdown(new BigDecimal("1500000.00"));
        assertThat(account.getAvailableLimit()).isEqualByComparingTo("0");
    }

    @Test
    void repaymentAboveOutstandingIsRejected() {
        account.drawdown(new BigDecimal("1000"));
        assertThatThrownBy(() -> account.repay(new BigDecimal("1000.01")))
                .isInstanceOf(RepaymentExceedsOutstandingException.class);
    }

    @Test
    void frozenAccountCanRepayButNotDraw() {
        account.drawdown(new BigDecimal("1000"));
        account.setStatus(AccountStatus.FROZEN);
        assertThatThrownBy(() -> account.drawdown(new BigDecimal("1")))
                .isInstanceOf(InvalidAccountStateException.class);
        account.repay(new BigDecimal("1000"));
        assertThat(account.getUtilizedAmount()).isEqualByComparingTo("0");
    }

    @Test
    void closedAccountRejectsRepayment() {
        account.setStatus(AccountStatus.CLOSED);
        assertThatThrownBy(() -> account.repay(new BigDecimal("1")))
                .isInstanceOf(InvalidAccountStateException.class);
    }

    @Test
    void accrualDoesNotChangePrincipalButCapitalizationDoes() {
        account.drawdown(new BigDecimal("300000"));
        account.addUnbilledInterest(new BigDecimal("98.6301"));
        assertThat(account.getUtilizedAmount()).isEqualByComparingTo("300000");
        assertThat(account.getAccumulatedUnbilledInterest()).isEqualByComparingTo("98.6301");

        account.settleUnbilledInterest(new BigDecimal("98.6301"));
        account.capitalizeInterest(new BigDecimal("98.63"));
        assertThat(account.getAccumulatedUnbilledInterest()).isEqualByComparingTo("0");
        assertThat(account.getUtilizedAmount()).isEqualByComparingTo("300098.63");
    }
}
