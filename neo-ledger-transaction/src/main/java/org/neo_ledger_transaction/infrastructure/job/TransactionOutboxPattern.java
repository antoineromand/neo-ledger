package org.neo_ledger_transaction.infrastructure.job;

import org.neo_ledger_transaction.application.exceptions.TransactionOutboxClaimException;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxEntryNotFoundException;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxPublishException;
import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.neo_ledger_transaction.infrastructure.repository.TransactionOutboxJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
public class TransactionOutboxPattern {
    private static final String PENDING = "PENDING";
    private static final String PROCESSING = "PROCESSING";
    private static final String PROCESSED = "PROCESSED";
    private static final String DEAD_LETTER = "DEAD_LETTER";
    private static final int BATCH_SIZE = 20;
    private static final int MAX_RETRY_COUNT = 5;
    private static final Duration BASE_BACKOFF = Duration.ofMinutes(1);

    private final TransactionOutboxJpaRepository transactionOutboxJpaRepository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public TransactionOutboxPattern(
            TransactionOutboxJpaRepository transactionOutboxJpaRepository,
            KafkaTemplate<String, byte[]> kafkaTemplate,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.transactionOutboxJpaRepository = transactionOutboxJpaRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public void execute() {
        List<OutboxEntry> transactions = this.claimPendingTransactions();

        for (OutboxEntry transaction : transactions) {
            try {
                this.publish(transaction);
                this.markTransactionAsProcessed(transaction);
            } catch (TransactionOutboxPublishException exception) {
                this.markTransactionAsFailed(transaction, exception);
            }
        }
    }

    public List<OutboxEntry> claimPendingTransactions() {
        try {
            return this.transactionTemplate.execute(status -> {
                List<OutboxEntry> entries = this.transactionOutboxJpaRepository.findBatchByStatusForUpdate(
                        PENDING,
                        LocalDateTime.now(this.clock),
                        PageRequest.of(0, BATCH_SIZE)
                );

                entries.forEach(entry -> entry.setStatus(PROCESSING));
                this.transactionOutboxJpaRepository.saveAll(entries);
                return entries;
            });
        } catch (RuntimeException exception) {
            throw new TransactionOutboxClaimException(exception);
        }
    }

    public void publish(OutboxEntry transaction) {
        String messageKey = Objects.requireNonNull(transaction.getId(), "Outbox id is required for idempotent publishing").toString();
        try {
            this.kafkaTemplate.send(transaction.getRoutingKey(), messageKey, transaction.getPayload()).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TransactionOutboxPublishException(exception);
        } catch (ExecutionException exception) {
            throw new TransactionOutboxPublishException(exception);
        }
    }

    public void markTransactionAsProcessed(OutboxEntry transaction) {
        this.transactionTemplate.executeWithoutResult(status -> {
            OutboxEntry current = this.transactionOutboxJpaRepository.findById(transaction.getId())
                    .orElseThrow(() -> new TransactionOutboxEntryNotFoundException(transaction.getId()));

            current.setStatus(PROCESSED);
            current.setProcessedAt(LocalDateTime.now(this.clock));
            current.setNextAttemptAt(null);
            current.setLastError(null);
            this.transactionOutboxJpaRepository.save(current);
        });
    }

    public void markTransactionAsFailed(OutboxEntry transaction, Exception exception) {
        this.transactionTemplate.executeWithoutResult(status -> {
            OutboxEntry current = this.transactionOutboxJpaRepository.findById(transaction.getId())
                    .orElseThrow(() -> new TransactionOutboxEntryNotFoundException(transaction.getId()));

            int nextRetryCount = current.getRetryCount() + 1;
            current.setRetryCount(nextRetryCount);
            current.setProcessedAt(null);
            current.setLastError(exception.getMessage());

            if (nextRetryCount >= MAX_RETRY_COUNT) {
                current.setStatus(DEAD_LETTER);
                current.setNextAttemptAt(null);
            } else {
                current.setStatus(PENDING);
                current.setNextAttemptAt(computeNextAttemptAt(nextRetryCount));
            }

            this.transactionOutboxJpaRepository.save(current);
        });
    }

    private LocalDateTime computeNextAttemptAt(int retryCount) {
        long delayMinutes = Math.min(BASE_BACKOFF.toMinutes() * (1L << Math.max(0, retryCount - 1)), 30L);
        return LocalDateTime.now(this.clock).plusMinutes(delayMinutes);
    }
}
