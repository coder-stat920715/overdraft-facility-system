package com.bank.overdraft.scheduler;

import com.bank.overdraft.service.BillingEngine;
import com.bank.overdraft.service.InterestEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Cron-driven batch jobs. In a multi-instance deployment run these on a single node (or add a distributed lock
 * such as ShedLock); both engines are idempotent per account/date so an accidental double run is harmless.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "overdraft.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OverdraftScheduledJobs {

    private final InterestEngine interestEngine;
    private final BillingEngine billingEngine;
    private final Clock clock;

    /** End of day accrual, every day at 23:59. */
    @Scheduled(cron = "0 59 23 * * *", zone = "${overdraft.timezone:Asia/Kolkata}")
    public void endOfDayAccrual() {
        log.info("Starting scheduled EOD interest accrual");
        interestEngine.runEndOfDayAccrual(LocalDate.now(clock));
    }

    /** Monthly billing at 01:00 on the 1st of every month (accounts with billingCycleDay = 1). */
    @Scheduled(cron = "0 0 1 1 * *", zone = "${overdraft.timezone:Asia/Kolkata}")
    public void monthlyBilling() {
        log.info("Starting scheduled monthly interest billing");
        billingEngine.runMonthlyBilling(LocalDate.now(clock), false);
    }

    /** Billing for accounts whose billingCycleDay is 2..28: runs at 01:00 on days 2-28 and bills only matching accounts. */
    @Scheduled(cron = "0 0 1 2-28 * *", zone = "${overdraft.timezone:Asia/Kolkata}")
    public void cycleDayBilling() {
        log.info("Starting scheduled cycle-day interest billing");
        billingEngine.runMonthlyBilling(LocalDate.now(clock), false);
    }
}
