package com.bank.overdraft.service.impl;

import com.bank.overdraft.service.InterestCalculationService;
import com.bank.overdraft.util.MoneyUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class InterestCalculationServiceImpl implements InterestCalculationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int DEFAULT_DAY_COUNT = 365;

    private final boolean leapYearAware;

    public InterestCalculationServiceImpl(@Value("${overdraft.interest.leap-year-aware:false}") boolean leapYearAware) {
        this.leapYearAware = leapYearAware;
    }

    @Override
    public int daysInYear(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return leapYearAware && date.isLeapYear() ? 366 : DEFAULT_DAY_COUNT;
    }

    @Override
    public BigDecimal calculateDailyInterest(BigDecimal utilizedBalance, BigDecimal annualRatePercent, LocalDate accrualDate) {
        Objects.requireNonNull(utilizedBalance, "utilizedBalance");
        Objects.requireNonNull(annualRatePercent, "annualRatePercent");
        if (utilizedBalance.signum() <= 0 || annualRatePercent.signum() <= 0) {
            return MoneyUtil.zero();
        }
        BigDecimal denominator = HUNDRED.multiply(BigDecimal.valueOf(daysInYear(accrualDate)));
        // single division => single rounding step, exact numerator
        return utilizedBalance.multiply(annualRatePercent)
                .divide(denominator, MoneyUtil.SCALE_INTERNAL, MoneyUtil.ROUNDING);
    }

    @Override
    public BigDecimal calculateInterestForPeriod(BigDecimal utilizedBalance, BigDecimal annualRatePercent, int days) {
        Objects.requireNonNull(utilizedBalance, "utilizedBalance");
        Objects.requireNonNull(annualRatePercent, "annualRatePercent");
        if (utilizedBalance.signum() <= 0 || annualRatePercent.signum() <= 0 || days <= 0) {
            return MoneyUtil.zero();
        }
        BigDecimal denominator = HUNDRED.multiply(BigDecimal.valueOf(DEFAULT_DAY_COUNT));
        return utilizedBalance.multiply(annualRatePercent).multiply(BigDecimal.valueOf(days))
                .divide(denominator, MoneyUtil.SCALE_INTERNAL, MoneyUtil.ROUNDING);
    }

    @Override
    public BigDecimal roundForBilling(BigDecimal amount) {
        return MoneyUtil.billing(amount);
    }
}
