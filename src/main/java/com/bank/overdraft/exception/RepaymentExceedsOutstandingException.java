package com.bank.overdraft.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class RepaymentExceedsOutstandingException extends OverdraftException {
    public RepaymentExceedsOutstandingException(String accountNumber, BigDecimal requested, BigDecimal outstanding) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "REPAYMENT_EXCEEDS_OUTSTANDING",
                "Repayment of " + requested.toPlainString() + " exceeds outstanding utilized amount of "
                        + outstanding.toPlainString() + " on account " + accountNumber);
    }
}
