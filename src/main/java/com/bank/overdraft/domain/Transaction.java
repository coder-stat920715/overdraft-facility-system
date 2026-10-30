package com.bank.overdraft.domain;

import com.bank.overdraft.enums.TransactionType;
import com.bank.overdraft.util.MoneyUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "od_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private OverdraftAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 24)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "utilized_balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal utilizedBalanceAfter;

    @Column(name = "available_limit_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableLimitAfter;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Records the post-transaction snapshot of the account. Call AFTER the account state has been mutated. */
    public static Transaction of(OverdraftAccount account, TransactionType type, BigDecimal amount,
                                 String referenceId, String description, LocalDateTime createdAt) {
        return Transaction.builder()
                .account(account)
                .transactionType(type)
                .amount(MoneyUtil.internal(amount))
                .utilizedBalanceAfter(account.getUtilizedAmount())
                .availableLimitAfter(account.getAvailableLimit())
                .referenceId(referenceId)
                .description(description)
                .createdAt(createdAt)
                .build();
    }
}
