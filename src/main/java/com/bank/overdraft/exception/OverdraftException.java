package com.bank.overdraft.exception;

import org.springframework.http.HttpStatus;

public abstract class OverdraftException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected OverdraftException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
