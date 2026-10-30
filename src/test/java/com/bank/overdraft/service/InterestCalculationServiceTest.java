package com.bank.overdraft.service;

import com.bank.overdraft.service.impl.InterestCalculationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InterestCalculationServiceTest {

    private final InterestCalculationService fixed365 = new InterestCalculationServiceImpl(false);
    private final InterestCalculationService leapAware = new InterestCalculationServiceImpl(true);
    private static final LocalDate NON_LEAP_DAY = LocalDate.of(2026, 3, 1);

    @ParameterizedTest(name = "{0} @ {1}% => {2} per day")
    @CsvSource({
            "300000.00, 12.00, 98.6301",
            "200000.00, 12.00, 65.7534",
            "1500000.00, 12.00, 493.1507",
            "250000.00, 12.00, 82.1918",
            "100.00, 12.00, 0.0329",
            "0.00, 12.00, 0.0000",
            "300000.00, 0.00, 0.0000"
    })
    @DisplayName("daily interest is exact to 4 decimal places")
    void dailyInterest(String utilized, String rate, String expected) {
        BigDecimal result = fixed365.calculateDailyInterest(new BigDecimal(utilized), new BigDecimal(rate), NON_LEAP_DAY);
        assertThat(result).isEqualByComparingTo(expected);
        assertThat(result.scale()).isEqualTo(4);
    }

    @Test
    @DisplayName("three-day scenario accumulates 263.0136 and bills 263.01")
    void threeDayScenario() {
        BigDecimal day1 = fixed365.calculateDailyInterest(new BigDecimal("300000"), new BigDecimal("12"), NON_LEAP_DAY);
        BigDecimal day2 = fixed365.calculateDailyInterest(new BigDecimal("300000"), new BigDecimal("12"), NON_LEAP_DAY.plusDays(1));
        BigDecimal day3 = fixed365.calculateDailyInterest(new BigDecimal("200000"), new BigDecimal("12"), NON_LEAP_DAY.plusDays(2));
        BigDecimal total = day1.add(day2).add(day3);

        assertThat(total).isEqualByComparingTo("263.0136");
        assertThat(fixed365.roundForBilling(total)).isEqualByComparingTo("263.01");
    }

    @Test
    @DisplayName("fixed 365 basis ignores leap years by default")
    void leapYearIgnoredByDefault() {
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        assertThat(fixed365.daysInYear(leapDay)).isEqualTo(365);
        assertThat(fixed365.calculateDailyInterest(new BigDecimal("300000"), new BigDecimal("12"), leapDay))
                .isEqualByComparingTo("98.6301");
    }

    @Test
    @DisplayName("leap-year-aware mode divides by 366 in leap years only")
    void leapYearAware() {
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        assertThat(leapAware.daysInYear(leapDay)).isEqualTo(366);
        assertThat(leapAware.calculateDailyInterest(new BigDecimal("300000"), new BigDecimal("12"), leapDay))
                .isEqualByComparingTo("98.3607");
        assertThat(leapAware.daysInYear(NON_LEAP_DAY)).isEqualTo(365);
    }

    @Test
    @DisplayName("billing rounding is HALF_EVEN (banker's rounding)")
    void bankersRounding() {
        assertThat(fixed365.roundForBilling(new BigDecimal("2.5050"))).isEqualByComparingTo("2.50");
        assertThat(fixed365.roundForBilling(new BigDecimal("2.5150"))).isEqualByComparingTo("2.52");
        assertThat(fixed365.roundForBilling(new BigDecimal("2.5051"))).isEqualByComparingTo("2.51");
    }

    @Test
    @DisplayName("period interest on a constant balance")
    void periodInterest() {
        // 300000 * 12% * 360 / 365 = 35506.8493
        assertThat(fixed365.calculateInterestForPeriod(new BigDecimal("300000"), new BigDecimal("12"), 360))
                .isEqualByComparingTo("35506.8493");
    }
}
