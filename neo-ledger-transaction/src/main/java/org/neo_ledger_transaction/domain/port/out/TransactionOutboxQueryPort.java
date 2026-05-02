package org.neo_ledger_transaction.domain.port.out;

import org.neo_ledger_transaction.application.model.TransactionOutboxMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Output port used by the relay use case to claim outbox messages that are
 * due for processing.
 */
public interface TransactionOutboxQueryPort {
    /**
     * Claims the next batch of eligible outbox messages.
     *
     * @param now       current time used to filter delayed retries
     * @param batchSize maximum number of rows to claim
     * @return messages claimed for processing
     */
    List<TransactionOutboxMessage> claimDueMessages(LocalDateTime now, int batchSize);
}
