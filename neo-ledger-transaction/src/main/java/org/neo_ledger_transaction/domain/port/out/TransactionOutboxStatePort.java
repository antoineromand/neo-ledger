package org.neo_ledger_transaction.domain.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TransactionOutboxStatePort {
    void markAsProcessed(UUID id, LocalDateTime processedAt);

    void scheduleRetry(UUID id, int retryCount, LocalDateTime nextAttemptAt, String lastError);

    void markAsDeadLetter(UUID id, int retryCount, String lastError);
}
