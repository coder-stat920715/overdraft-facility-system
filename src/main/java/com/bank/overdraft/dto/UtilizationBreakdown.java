package com.bank.overdraft.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UtilizationBreakdown(
        BigDecimal sanctionedLimit,
        BigDecimal utilizedAmount,
        BigDecimal availableLimit,
        BigDecimal utilizationPercent,
        BigDecimal accumulatedUnbilledInterest,
        BigDecimal projectedDailyInterest,
        LocalDate nextBillingDate) {
}
