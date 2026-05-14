package org.neo_ledger_transaction.domain.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

/** Output port used by the relay use case to update the lifecycle state of an outbox entry. */
public interface TransactionOutboxStatePort {
  /** Marks the outbox entry as successfully processed. */
  void markAsProcessed(UUID id, LocalDateTime processedAt);

  /** Re-schedules the outbox entry for a later retry. */
  void scheduleRetry(UUID id, int retryCount, LocalDateTime nextAttemptAt, String lastError);

  /** Moves the outbox entry to a terminal dead-letter state. */
  void markAsDeadLetter(UUID id, int retryCount, String lastError);
}
