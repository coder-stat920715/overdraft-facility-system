package com.bank.overdraft.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends OverdraftException {
    public AccountNotFoundException(String accountNumber) {
        super(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Overdraft account not found: " + accountNumber);
    }
}
