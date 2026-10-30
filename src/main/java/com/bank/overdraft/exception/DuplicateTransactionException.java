package com.bank.overdraft.exception;

import org.springframework.http.HttpStatus;

public class DuplicateTransactionException extends OverdraftException {
    public DuplicateTransactionException(String accountNumber, String referenceId) {
        super(HttpStatus.CONFLICT, "DUPLICATE_TRANSACTION",
                "Transaction with referenceId '" + referenceId + "' already exists on account " + accountNumber);
    }
}
