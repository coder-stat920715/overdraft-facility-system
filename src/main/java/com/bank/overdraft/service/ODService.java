package com.bank.overdraft.service;

import com.bank.overdraft.dto.AccountResponse;
import com.bank.overdraft.dto.OpenAccountRequest;
import com.bank.overdraft.dto.StatementResponse;
import com.bank.overdraft.dto.TransactionRequest;
import com.bank.overdraft.dto.TransactionResponse;

public interface ODService {

    AccountResponse openAccount(OpenAccountRequest request);

    TransactionResponse drawdown(String accountNumber, TransactionRequest request);

    TransactionResponse repay(String accountNumber, TransactionRequest request);

    StatementResponse getStatement(String accountNumber, int limit);
}
