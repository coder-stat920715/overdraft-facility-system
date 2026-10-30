package com.bank.overdraft.dto;

import com.bank.overdraft.enums.AccountStatus;
import com.bank.overdraft.enums.BankType;
import com.bank.overdraft.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        String accountNumber,
        String customerId,
        BankType bankName,
        BigDecimal sanctionedLimit,
        BigDecimal utilizedAmount,
        BigDecimal availableLimit,
        BigDecimal annualInterestRate,
        BigDecimal accumulatedUnbilledInterest,
        AccountStatus status,
        int billingCycleDay,
        LocalDateTime openedAt) {

    public AccountResponse {
        sanctionedLimit = MoneyUtil.billing(sanctionedLimit);
        utilizedAmount = MoneyUtil.billing(utilizedAmount);
        availableLimit = MoneyUtil.billing(availableLimit);
        annualInterestRate = MoneyUtil.billing(annualInterestRate);
        // accumulatedUnbilledInterest is intentionally kept at internal 4 dp precision
    }
}
