package com.bank.overdraft.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientLimitException extends OverdraftException {
    public InsufficientLimitException(String accountNumber, BigDecimal requested, BigDecimal available) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_LIMIT",
                "Drawdown of " + requested.toPlainString() + " exceeds available limit of "
                        + available.toPlainString() + " on account " + accountNumber);
    }
}
