package org.neo_ledger_transaction.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxEntryNotFoundException;
import org.neo_ledger_transaction.application.model.TransactionOutboxMessage;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxQueryPort;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxStatePort;
import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for the outbox relay ports.
 *
 * <p>It claims due entries and persists relay state transitions in short transactions.
 */
@Service
public class TransactionOutboxRelayJpaAdapter
    implements TransactionOutboxQueryPort, TransactionOutboxStatePort {
  private static final String PENDING = "PENDING";
  private static final String PROCESSING = "PROCESSING";
  private static final String PROCESSED = "PROCESSED";
  private static final String DEAD_LETTER = "DEAD_LETTER";

  private final TransactionOutboxJpaRepository transactionOutboxJpaRepository;

  public TransactionOutboxRelayJpaAdapter(
      TransactionOutboxJpaRepository transactionOutboxJpaRepository) {
    this.transactionOutboxJpaRepository = transactionOutboxJpaRepository;
  }

  /** Claims the next batch of due outbox rows and converts them to application messages. */
  @Override
  @Transactional
  public List<TransactionOutboxMessage> claimDueMessages(LocalDateTime now, int batchSize) {
    List<OutboxEntry> entries =
        this.transactionOutboxJpaRepository.findBatchByStatusForUpdate(
            PENDING, now, PageRequest.of(0, batchSize));

    entries.forEach(entry -> entry.setStatus(PROCESSING));
    this.transactionOutboxJpaRepository.saveAll(entries);

    return entries.stream().map(this::toMessage).toList();
  }

  @Override
  @Transactional
  /** Marks the given entry as successfully processed. */
  public void markAsProcessed(UUID id, LocalDateTime processedAt) {
    OutboxEntry current =
        this.transactionOutboxJpaRepository
            .findById(id)
            .orElseThrow(() -> new TransactionOutboxEntryNotFoundException(id));

    current.setStatus(PROCESSED);
    current.setProcessedAt(processedAt);
    current.setNextAttemptAt(null);
    current.setLastError(null);
    this.transactionOutboxJpaRepository.save(current);
  }

  @Override
  @Transactional
  /** Re-schedules the given entry for another attempt. */
  public void scheduleRetry(
      UUID id, int retryCount, LocalDateTime nextAttemptAt, String lastError) {
    OutboxEntry current =
        this.transactionOutboxJpaRepository
            .findById(id)
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
  /** Marks the given entry as terminally failed. */
  public void markAsDeadLetter(UUID id, int retryCount, String lastError) {
    OutboxEntry current =
        this.transactionOutboxJpaRepository
            .findById(id)
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
        entry.getId(), entry.getRoutingKey(), entry.getPayload(), entry.getRetryCount());
  }
}
