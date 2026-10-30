package com.bank.overdraft.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InterestCalculationService {

    /** Days in the year used as denominator: 365, or 366 in leap years when leap-year-aware mode is enabled. */
    int daysInYear(LocalDate date);

    /** Daily interest = utilized * annualRatePercent / (100 * daysInYear), rounded HALF_EVEN to 4 dp. */
    BigDecimal calculateDailyInterest(BigDecimal utilizedBalance, BigDecimal annualRatePercent, LocalDate accrualDate);

    /** Simple (non-compounding) interest for N days on a constant balance, 365 day basis, 4 dp. */
    BigDecimal calculateInterestForPeriod(BigDecimal utilizedBalance, BigDecimal annualRatePercent, int days);

    /** Rounds an internal 4 dp amount to the 2 dp billing amount using HALF_EVEN. */
    BigDecimal roundForBilling(BigDecimal amount);
}
