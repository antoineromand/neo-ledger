package org.neo_ledger_transaction.application.model;

import java.util.UUID;

/**
 * Application-level representation of an outbox entry ready to be relayed.
 * <p>
 * It exposes only the data needed by the use case: identity, routing key,
 * payload, and retry count.
 * </p>
 */
public record TransactionOutboxMessage(
        UUID id,
        String routingKey,
        byte[] payload,
        int retryCount
) {
}
