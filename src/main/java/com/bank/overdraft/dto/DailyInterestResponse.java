package com.bank.overdraft.dto;

import com.bank.overdraft.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

/** accruedAmount is exposed at the internal 4 dp precision; billing rounds it to 2 dp. */
public record DailyInterestResponse(
        LocalDate accrualDate,
        BigDecimal utilizedBalance,
        BigDecimal interestRate,
        BigDecimal accruedAmount,
        boolean billed) {

    public DailyInterestResponse {
        utilizedBalance = MoneyUtil.billing(utilizedBalance);
        interestRate = MoneyUtil.billing(interestRate);
    }
}
