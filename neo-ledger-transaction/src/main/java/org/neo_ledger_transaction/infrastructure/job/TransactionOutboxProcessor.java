package org.neo_ledger_transaction.infrastructure.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransactionOutboxProcessor {
    private final TransactionOutboxPattern transactionOutboxPattern;

    public TransactionOutboxProcessor(TransactionOutboxPattern transactionOutboxPattern) {
        this.transactionOutboxPattern = transactionOutboxPattern;
    }

    @Scheduled(fixedDelay = 60000)
    public void execute() {
        System.out.println("Executing TransactionOutboxProcessor");
        this.transactionOutboxPattern.execute();
    }
}
