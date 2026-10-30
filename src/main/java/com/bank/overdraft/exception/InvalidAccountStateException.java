package com.bank.overdraft.exception;

import com.bank.overdraft.enums.AccountStatus;
import org.springframework.http.HttpStatus;

public class InvalidAccountStateException extends OverdraftException {
    public InvalidAccountStateException(String accountNumber, AccountStatus status, String operation) {
        super(HttpStatus.CONFLICT, "INVALID_ACCOUNT_STATE",
                "Operation '" + operation + "' is not allowed on account " + accountNumber + " in status " + status);
    }
}
