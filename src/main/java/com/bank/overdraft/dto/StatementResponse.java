package com.bank.overdraft.dto;

import java.util.List;

/** Transactions and daily interest log are returned newest first. */
public record StatementResponse(
        AccountResponse account,
        UtilizationBreakdown utilization,
        List<TransactionResponse> transactions,
        List<DailyInterestResponse> dailyInterestLog) {
}
