package com.bank.overdraft.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Disable with overdraft.scheduler.enabled=false (e.g. in tests or on non-leader nodes). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "overdraft.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
