package com.bank.overdraft.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobExecutionSummary(
        String jobName,
        LocalDate businessDate,
        int accountsProcessed,
        int accountsSkipped,
        int accountsFailed,
        BigDecimal totalAmount) {
}
