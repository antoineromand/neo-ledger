package org.neo_ledger_transaction.infrastructure.job;

import org.neo_ledger_transaction.application.port.in.TransactionOutboxRelayUseCasePort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled trigger for the outbox relay use case.
 * <p>
 * This component does not implement the business flow itself; it only
 * delegates to the application layer on a fixed delay.
 * </p>
 */
@Component
public class TransactionOutboxProcessor {
    private final TransactionOutboxRelayUseCasePort transactionOutboxRelayUseCasePort;

    public TransactionOutboxProcessor(TransactionOutboxRelayUseCasePort transactionOutboxRelayUseCasePort) {
        this.transactionOutboxRelayUseCasePort = transactionOutboxRelayUseCasePort;
    }

    /**
     * Executes one relay cycle.
     */
    @Scheduled(fixedDelay = 60000)
    public void execute() {
        this.transactionOutboxRelayUseCasePort.execute();
    }
}
