package org.neo_ledger_transaction.infrastructure.repository;

import org.neo_ledger_transaction.application.exceptions.TransactionOutboxEntryNotFoundException;
import org.neo_ledger_transaction.application.model.TransactionOutboxMessage;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxQueryPort;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxStatePort;
import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionOutboxRelayJpaAdapter implements TransactionOutboxQueryPort, TransactionOutboxStatePort {
    private static final String PENDING = "PENDING";
    private static final String PROCESSING = "PROCESSING";
    private static final String PROCESSED = "PROCESSED";
    private static final String DEAD_LETTER = "DEAD_LETTER";

    private final TransactionOutboxJpaRepository transactionOutboxJpaRepository;

    public TransactionOutboxRelayJpaAdapter(TransactionOutboxJpaRepository transactionOutboxJpaRepository) {
        this.transactionOutboxJpaRepository = transactionOutboxJpaRepository;
    }

    @Override
    @Transactional
    public List<TransactionOutboxMessage> claimDueMessages(LocalDateTime now, int batchSize) {
        List<OutboxEntry> entries = this.transactionOutboxJpaRepository.findBatchByStatusForUpdate(
                PENDING,
                now,
                PageRequest.of(0, batchSize)
        );

        entries.forEach(entry -> entry.setStatus(PROCESSING));
        this.transactionOutboxJpaRepository.saveAll(entries);

        return entries.stream()
                .map(this::toMessage)
                .toList();
    }

    @Override
    @Transactional
    public void markAsProcessed(UUID id, LocalDateTime processedAt) {
        OutboxEntry current = this.transactionOutboxJpaRepository.findById(id)
                .orElseThrow(() -> new TransactionOutboxEntryNotFoundException(id));

        current.setStatus(PROCESSED);
        current.setProcessedAt(processedAt);
        current.setNextAttemptAt(null);
        current.setLastError(null);
        this.transactionOutboxJpaRepository.save(current);
    }

    @Override
    @Transactional
    public void scheduleRetry(UUID id, int retryCount, LocalDateTime nextAttemptAt, String lastError) {
        OutboxEntry current = this.transactionOutboxJpaRepository.findById(id)
                .orElseThrow(() -> new TransactionOutboxEntryNotFoundException(id));

        current.setStatus(PENDING);
        current.setRetryCount(retryCount);
        current.setProcessedAt(null);
        current.setNextAttemptAt(nextAttemptAt);
        current.setLastError(lastError);
        this.transactionOutboxJpaRepository.save(current);
    }

    @Override
    @Transactional
    public void markAsDeadLetter(UUID id, int retryCount, String lastError) {
        OutboxEntry current = this.transactionOutboxJpaRepository.findById(id)
                .orElseThrow(() -> new TransactionOutboxEntryNotFoundException(id));

        current.setStatus(DEAD_LETTER);
        current.setRetryCount(retryCount);
        current.setProcessedAt(null);
        current.setNextAttemptAt(null);
        current.setLastError(lastError);
        this.transactionOutboxJpaRepository.save(current);
    }

    private TransactionOutboxMessage toMessage(OutboxEntry entry) {
        return new TransactionOutboxMessage(
                entry.getId(),
                entry.getRoutingKey(),
                entry.getPayload(),
                entry.getRetryCount()
        );
    }
}
