package com.bank.overdraft.service;

import com.bank.overdraft.dto.JobExecutionSummary;

import java.time.LocalDate;

public interface BillingEngine {

    /**
     * Bills and capitalizes all unbilled daily interest accrued up to (runDate - 1 day).
     *
     * @param runDate        the date the billing job runs on
     * @param forceAllCycles when false only accounts whose billingCycleDay equals runDate's day-of-month are billed;
     *                       when true every account with unbilled interest is billed
     */
    JobExecutionSummary runMonthlyBilling(LocalDate runDate, boolean forceAllCycles);
}
