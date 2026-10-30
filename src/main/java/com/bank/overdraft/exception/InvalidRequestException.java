package com.bank.overdraft.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends OverdraftException {
    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }
}
