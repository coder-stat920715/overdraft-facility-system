package com.bank.overdraft.service;

import com.bank.overdraft.dto.JobExecutionSummary;

import java.time.LocalDate;

public interface InterestEngine {

    /** Accrues one day of interest on the closing utilized balance of every ACTIVE account. Idempotent per account/date. */
    JobExecutionSummary runEndOfDayAccrual(LocalDate businessDate);
}
