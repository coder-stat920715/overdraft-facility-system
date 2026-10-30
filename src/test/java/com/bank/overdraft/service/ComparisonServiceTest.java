package com.bank.overdraft.service;

import com.bank.overdraft.dto.ComparisonRequest;
import com.bank.overdraft.dto.ComparisonResponse;
import com.bank.overdraft.exception.InvalidRequestException;
import com.bank.overdraft.service.impl.ComparisonServiceImpl;
import com.bank.overdraft.service.impl.InterestCalculationServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparisonServiceTest {

    private final ComparisonService service = new ComparisonServiceImpl(new InterestCalculationServiceImpl(false));

    @Test
    void overdraftOnPartialUsageIsCheaperThanLumpSumLoan() {
        ComparisonResponse response = service.compare(new ComparisonRequest(
                new BigDecimal("1500000"), new BigDecimal("300000"), new BigDecimal("12"), 360));

        assertThat(response.personalLoan().tenureMonths()).isEqualTo(12);
        assertThat(response.personalLoan().monthlyEmi()).isBetween(new BigDecimal("133270.00"), new BigDecimal("133280.00"));
        assertThat(response.overdraft().totalInterest()).isEqualByComparingTo("35506.85");
        assertThat(response.netSavings())
                .isEqualByComparingTo(response.personalLoan().totalInterest().subtract(response.overdraft().totalInterest()));
        assertThat(response.netSavings().signum()).isPositive();
        assertThat(response.cheaperOption()).isEqualTo("OVERDRAFT");
    }

    @Test
    void zeroUtilizationCostsNothing() {
        ComparisonResponse response = service.compare(new ComparisonRequest(
                new BigDecimal("1500000"), BigDecimal.ZERO, new BigDecimal("12"), 90));
        assertThat(response.overdraft().totalInterest()).isEqualByComparingTo("0.00");
        assertThat(response.netSavings()).isEqualByComparingTo(response.personalLoan().totalInterest());
    }

    @Test
    void utilizationAboveLimitIsRejected() {
        assertThatThrownBy(() -> service.compare(new ComparisonRequest(
                new BigDecimal("100000"), new BigDecimal("200000"), new BigDecimal("12"), 30)))
                .isInstanceOf(InvalidRequestException.class);
    }
}
