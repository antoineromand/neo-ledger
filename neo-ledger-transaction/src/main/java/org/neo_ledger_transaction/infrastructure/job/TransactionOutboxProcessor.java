package org.neo_ledger_transaction.infrastructure.job;

import org.neo_ledger_transaction.application.port.in.TransactionOutboxRelayUseCasePort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransactionOutboxProcessor {
    private final TransactionOutboxRelayUseCasePort transactionOutboxRelayUseCasePort;

    public TransactionOutboxProcessor(TransactionOutboxRelayUseCasePort transactionOutboxRelayUseCasePort) {
        System.out.println("In TransactionOutboxProcessor");
        this.transactionOutboxRelayUseCasePort = transactionOutboxRelayUseCasePort;
    }

    @Scheduled(fixedDelay = 60000)
    public void execute() {
        this.transactionOutboxRelayUseCasePort.execute();
    }
}
