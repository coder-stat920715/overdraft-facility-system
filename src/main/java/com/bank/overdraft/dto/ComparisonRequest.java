package com.bank.overdraft.dto;

import java.math.BigDecimal;

/** Validated at the controller boundary; cross-field rules are enforced in ComparisonService. */
public record ComparisonRequest(
        BigDecimal sanctionedLimit,
        BigDecimal utilizedAmount,
        BigDecimal interestRate,
        int holdingDays) {
}
