package com.bank.overdraft.service.impl;

import com.bank.overdraft.dto.ComparisonRequest;
import com.bank.overdraft.dto.ComparisonResponse;
import com.bank.overdraft.exception.InvalidRequestException;
import com.bank.overdraft.service.ComparisonService;
import com.bank.overdraft.service.InterestCalculationService;
import com.bank.overdraft.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Personal loan model: a lump sum equal to the sanctioned limit, repaid via reducing-balance EMIs over
 * ceil(holdingDays / 30) months at the same nominal annual rate.
 * Overdraft model: simple daily interest on the utilized amount only for the holding period.
 */
@Service
@RequiredArgsConstructor
public class ComparisonServiceImpl implements ComparisonService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MONTHLY_DIVISOR = BigDecimal.valueOf(1200);

    private final InterestCalculationService interestCalculationService;

    @Override
    public ComparisonResponse compare(ComparisonRequest request) {
        BigDecimal limit = request.sanctionedLimit();
        BigDecimal utilized = request.utilizedAmount();
        BigDecimal rate = request.interestRate();
        int days = request.holdingDays();

        if (utilized.compareTo(limit) > 0) {
            throw new InvalidRequestException("utilizedAmount cannot exceed sanctionedLimit");
        }

        // ---- Personal loan (lump sum on the whole limit) ----
        int tenureMonths = Math.max(1, (days + 29) / 30);
        BigDecimal emi = computeEmi(limit, rate, tenureMonths);
        BigDecimal loanPayable = MoneyUtil.billing(emi.multiply(BigDecimal.valueOf(tenureMonths)));
        BigDecimal loanInterest = MoneyUtil.billing(loanPayable.subtract(limit));

        // ---- Overdraft (utilization based) ----
        BigDecimal odInterest = MoneyUtil.billing(
                interestCalculationService.calculateInterestForPeriod(utilized, rate, days));
        BigDecimal odDaily = interestCalculationService.calculateDailyInterest(utilized, rate, java.time.LocalDate.of(2001, 1, 1));
        BigDecimal odPayable = MoneyUtil.billing(utilized.add(odInterest));

        BigDecimal savings = MoneyUtil.billing(loanInterest.subtract(odInterest));
        BigDecimal savingsPercent = loanInterest.signum() > 0
                ? savings.multiply(HUNDRED).divide(loanInterest, MoneyUtil.SCALE_BILLING, MoneyUtil.ROUNDING)
                : BigDecimal.ZERO.setScale(MoneyUtil.SCALE_BILLING);

        String cheaper = savings.signum() > 0 ? "OVERDRAFT" : savings.signum() < 0 ? "PERSONAL_LOAN" : "EQUAL";
        String summary = "Over " + days + " days the overdraft costs INR " + odInterest.toPlainString()
                + " in interest versus INR " + loanInterest.toPlainString() + " for a personal loan of INR "
                + MoneyUtil.billing(limit).toPlainString() + " (net savings INR " + savings.toPlainString() + ").";

        return new ComparisonResponse(
                new ComparisonResponse.PersonalLoanCost(MoneyUtil.billing(limit), tenureMonths, emi, loanInterest, loanPayable),
                new ComparisonResponse.OverdraftCost(MoneyUtil.billing(limit), MoneyUtil.billing(utilized), days,
                        odDaily, odInterest, odPayable),
                savings, savingsPercent, cheaper, summary);
    }

    /** EMI = P * r * (1+r)^n / ((1+r)^n - 1), r = annualRate / 1200; rounded to 2 dp as banks do. */
    private BigDecimal computeEmi(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        BigDecimal monthlyRate = annualRatePercent.divide(MONTHLY_DIVISOR, MathContext.DECIMAL128);
        if (monthlyRate.signum() == 0) {
            return MoneyUtil.billing(principal.divide(BigDecimal.valueOf(months), MathContext.DECIMAL128));
        }
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months, MathContext.DECIMAL128);
        BigDecimal emi = principal.multiply(monthlyRate).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), MathContext.DECIMAL128);
        return MoneyUtil.billing(emi);
    }
}
