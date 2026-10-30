package com.bank.overdraft.domain;

import com.bank.overdraft.enums.AccountStatus;
import com.bank.overdraft.enums.BankType;
import com.bank.overdraft.exception.InsufficientLimitException;
import com.bank.overdraft.exception.InvalidAccountStateException;
import com.bank.overdraft.exception.RepaymentExceedsOutstandingException;
import com.bank.overdraft.util.MoneyUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Overdraft account aggregate. Invariant: availableLimit = sanctionedLimit - utilizedAmount.
 */
@Entity
@Table(name = "overdraft_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverdraftAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, updatable = false, length = 32)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_name", nullable = false, length = 20)
    private BankType bankName;

    @Column(name = "sanctioned_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal sanctionedLimit;

    @Column(name = "utilized_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal utilizedAmount;

    @Column(name = "available_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableLimit;

    /** Annual interest rate expressed in percent, e.g. 12.0000 means 12% p.a. */
    @Column(name = "annual_interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal annualInterestRate;

    @Column(name = "accumulated_unbilled_interest", nullable = false, precision = 19, scale = 4)
    private BigDecimal accumulatedUnbilledInterest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AccountStatus status;

    @Column(name = "billing_cycle_day", nullable = false)
    private int billingCycleDay;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Creates a fresh, ACTIVE account with zero utilization. */
    public static OverdraftAccount open(String accountNumber, String customerId, BankType bank, BigDecimal sanctionedLimit,
                                        BigDecimal annualInterestRate, int billingCycleDay, LocalDateTime now) {
        BigDecimal limit = MoneyUtil.internal(sanctionedLimit);
        return OverdraftAccount.builder()
                .accountNumber(accountNumber)
                .customerId(customerId)
                .bankName(bank)
                .sanctionedLimit(limit)
                .utilizedAmount(MoneyUtil.zero())
                .availableLimit(limit)
                .annualInterestRate(MoneyUtil.internal(annualInterestRate))
                .accumulatedUnbilledInterest(MoneyUtil.zero())
                .status(AccountStatus.ACTIVE)
                .billingCycleDay(billingCycleDay)
                .openedAt(now)
                .updatedAt(now)
                .build();
    }

    /** Debits the credit line. Only allowed on ACTIVE accounts and within the available limit. */
    public void drawdown(BigDecimal amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException(accountNumber, status, "drawdown");
        }
        if (amount.compareTo(availableLimit) > 0) {
            throw new InsufficientLimitException(accountNumber, amount, availableLimit);
        }
        utilizedAmount = MoneyUtil.internal(utilizedAmount.add(amount));
        recalculateAvailableLimit();
    }

    /** Partial or full repayment with no prepayment penalty. Allowed on ACTIVE and FROZEN accounts. */
    public void repay(BigDecimal amount) {
        if (status == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException(accountNumber, status, "repayment");
        }
        if (amount.compareTo(utilizedAmount) > 0) {
            throw new RepaymentExceedsOutstandingException(accountNumber, amount, utilizedAmount);
        }
        utilizedAmount = MoneyUtil.internal(utilizedAmount.subtract(amount));
        recalculateAvailableLimit();
    }

    /** Adds one day's accrual to the unbilled bucket. Principal (utilizedAmount) is NOT changed. */
    public void addUnbilledInterest(BigDecimal interest) {
        accumulatedUnbilledInterest = MoneyUtil.internal(accumulatedUnbilledInterest.add(interest));
    }

    /** Removes an already-billed raw (4 dp) amount from the unbilled bucket; never goes below zero. */
    public void settleUnbilledInterest(BigDecimal billedRaw) {
        BigDecimal remaining = accumulatedUnbilledInterest.subtract(billedRaw);
        accumulatedUnbilledInterest = remaining.signum() < 0 ? MoneyUtil.zero() : MoneyUtil.internal(remaining);
    }

    /** Monthly billing: capitalizes interest into the utilized balance. May push availability below zero (over-limit). */
    public void capitalizeInterest(BigDecimal billedInterest) {
        utilizedAmount = MoneyUtil.internal(utilizedAmount.add(billedInterest));
        recalculateAvailableLimit();
    }

    public void recalculateAvailableLimit() {
        availableLimit = MoneyUtil.internal(sanctionedLimit.subtract(utilizedAmount));
    }
}
