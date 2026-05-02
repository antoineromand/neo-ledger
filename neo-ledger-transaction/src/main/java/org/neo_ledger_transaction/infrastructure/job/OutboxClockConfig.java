package org.neo_ledger_transaction.infrastructure.job;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class OutboxClockConfig {
    @Bean
    public Clock outboxClock() {
        return Clock.systemUTC();
    }
}
