package com.bank.overdraft.controller;

import com.bank.overdraft.dto.JobExecutionSummary;
import com.bank.overdraft.service.BillingEngine;
import com.bank.overdraft.service.InterestEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/** Manual batch triggers for testing/demo. Secure these endpoints (role ADMIN / network policy) in production. */
@RestController
@RequestMapping("/api/v1/overdraft/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Manual triggers for the EOD accrual and monthly billing batch jobs")
public class AdminController {

    private final InterestEngine interestEngine;
    private final BillingEngine billingEngine;
    private final Clock clock;

    @PostMapping("/trigger-eod-accrual")
    @Operation(summary = "Run the EOD interest accrual batch now")
    public JobExecutionSummary triggerEodAccrual(
            @Parameter(description = "Business date to accrue for (ISO yyyy-MM-dd); defaults to today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return interestEngine.runEndOfDayAccrual(businessDate != null ? businessDate : LocalDate.now(clock));
    }

    @PostMapping("/trigger-monthly-billing")
    @Operation(summary = "Run the monthly interest billing batch now",
            description = "Bills all interest accrued up to (businessDate - 1 day). By default every account with unbilled "
                    + "interest is billed; set forceAllCycles=false to honour each account's billingCycleDay.")
    public JobExecutionSummary triggerMonthlyBilling(
            @Parameter(description = "Run date (ISO yyyy-MM-dd); defaults to today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(defaultValue = "true") boolean forceAllCycles) {
        return billingEngine.runMonthlyBilling(businessDate != null ? businessDate : LocalDate.now(clock), forceAllCycles);
    }
}
