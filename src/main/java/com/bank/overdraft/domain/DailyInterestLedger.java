package com.bank.overdraft.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_interest_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyInterestLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private OverdraftAccount account;

    @Column(name = "accrual_date", nullable = false, updatable = false)
    private LocalDate accrualDate;

    @Column(name = "utilized_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal utilizedBalance;

    @Column(name = "interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "accrued_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal accruedAmount;

    @Column(name = "billed", nullable = false)
    private boolean billed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
