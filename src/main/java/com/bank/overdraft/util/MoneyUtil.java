package com.bank.overdraft.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Central place for monetary scale and rounding rules.
 * Internal precision: 4 decimal places. Billing precision: 2 decimal places. Rounding: HALF_EVEN (banker's rounding).
 */
public final class MoneyUtil {

    public static final int SCALE_INTERNAL = 4;
    public static final int SCALE_BILLING = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private MoneyUtil() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE_INTERNAL, ROUNDING);
    }

    /** Null-safe rounding to internal precision (4 dp). */
    public static BigDecimal internal(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE_INTERNAL, ROUNDING);
    }

    /** Null-safe rounding to billing precision (2 dp). */
    public static BigDecimal billing(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE_BILLING, ROUNDING);
    }
}
