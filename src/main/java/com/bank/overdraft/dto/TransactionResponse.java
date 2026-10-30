package com.bank.overdraft.dto;

import com.bank.overdraft.enums.TransactionType;
import com.bank.overdraft.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String accountNumber,
        TransactionType transactionType,
        BigDecimal amount,
        BigDecimal utilizedBalanceAfter,
        BigDecimal availableLimitAfter,
        String referenceId,
        String description,
        LocalDateTime createdAt) {

    public TransactionResponse {
        amount = MoneyUtil.billing(amount);
        utilizedBalanceAfter = MoneyUtil.billing(utilizedBalanceAfter);
        availableLimitAfter = MoneyUtil.billing(availableLimitAfter);
    }
}
