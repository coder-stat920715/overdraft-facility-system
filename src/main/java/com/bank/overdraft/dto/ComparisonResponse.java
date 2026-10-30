package com.bank.overdraft.dto;

import java.math.BigDecimal;

public record ComparisonResponse(
        PersonalLoanCost personalLoan,
        OverdraftCost overdraft,
        BigDecimal netSavings,
        BigDecimal savingsPercentOfLoanInterest,
        String cheaperOption,
        String summary) {

    public record PersonalLoanCost(
            BigDecimal principal,
            int tenureMonths,
            BigDecimal monthlyEmi,
            BigDecimal totalInterest,
            BigDecimal totalPayable) {
    }

    public record OverdraftCost(
            BigDecimal sanctionedLimit,
            BigDecimal utilizedAmount,
            int holdingDays,
            BigDecimal dailyInterest,
            BigDecimal totalInterest,
            BigDecimal totalPayable) {
    }
}
