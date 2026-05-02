package org.neo_ledger_transaction.application.service;

import org.neo_ledger_transaction.application.exceptions.TransactionOutboxClaimException;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxStateException;
import org.neo_ledger_transaction.application.model.TransactionOutboxMessage;
import org.neo_ledger_transaction.application.port.in.TransactionOutboxRelayUseCasePort;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxPublisherPort;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxQueryPort;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxStatePort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
/**
 * Application service that orchestrates the outbox relay.
 * <p>
 * It claims due messages, delegates publication, and updates the outbox
 * state through output ports. The class intentionally contains no JPA or
 * Kafka details.
 * </p>
 */
public class TransactionOutboxRelayService implements TransactionOutboxRelayUseCasePort {
    private static final int BATCH_SIZE = 20;
    private static final int MAX_RETRY_COUNT = 5;
    private static final Duration BASE_BACKOFF = Duration.ofMinutes(1);

    private final TransactionOutboxQueryPort transactionOutboxQueryPort;
    private final TransactionOutboxStatePort transactionOutboxStatePort;
    private final TransactionOutboxPublisherPort transactionOutboxPublisherPort;
    private final Clock clock;

    public TransactionOutboxRelayService(
            TransactionOutboxQueryPort transactionOutboxQueryPort,
            TransactionOutboxStatePort transactionOutboxStatePort,
            TransactionOutboxPublisherPort transactionOutboxPublisherPort,
            Clock clock) {
        this.transactionOutboxQueryPort = transactionOutboxQueryPort;
        this.transactionOutboxStatePort = transactionOutboxStatePort;
        this.transactionOutboxPublisherPort = transactionOutboxPublisherPort;
        this.clock = clock;
    }

    @Override
    public void execute() {
        List<TransactionOutboxMessage> messages;
        try {
            messages = this.transactionOutboxQueryPort.claimDueMessages(LocalDateTime.now(this.clock), BATCH_SIZE);
        } catch (RuntimeException exception) {
            throw new TransactionOutboxClaimException(exception);
        }

        for (TransactionOutboxMessage message : messages) {
            try {
                this.transactionOutboxPublisherPort.publish(
                        message.routingKey(),
                        message.id().toString(),
                        message.payload()
                );
            } catch (RuntimeException exception) {
                this.handleFailure(message, exception);
                continue;
            }

            try {
                this.transactionOutboxStatePort.markAsProcessed(message.id(), LocalDateTime.now(this.clock));
            } catch (RuntimeException stateException) {
                throw new TransactionOutboxStateException(stateException);
            }
        }
    }

    private void handleFailure(TransactionOutboxMessage message, RuntimeException exception) {
        int nextRetryCount = message.retryCount() + 1;
        String error = Objects.toString(exception.getMessage(), exception.getClass().getSimpleName());

        if (nextRetryCount >= MAX_RETRY_COUNT) {
            try {
                this.transactionOutboxStatePort.markAsDeadLetter(message.id(), nextRetryCount, error);
            } catch (RuntimeException stateException) {
                throw new TransactionOutboxStateException(stateException);
            }
            return;
        }

        LocalDateTime nextAttemptAt = computeNextAttemptAt(nextRetryCount);
        try {
            this.transactionOutboxStatePort.scheduleRetry(message.id(), nextRetryCount, nextAttemptAt, error);
        } catch (RuntimeException stateException) {
            throw new TransactionOutboxStateException(stateException);
        }
    }

    private LocalDateTime computeNextAttemptAt(int retryCount) {
        long delayMinutes = Math.min(BASE_BACKOFF.toMinutes() * (1L << Math.max(0, retryCount - 1)), 30L);
        return LocalDateTime.now(this.clock).plusMinutes(delayMinutes);
    }
}
