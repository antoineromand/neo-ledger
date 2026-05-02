package org.neo_ledger_transaction.application.model;

import java.util.UUID;

public record TransactionOutboxMessage(
        UUID id,
        String routingKey,
        byte[] payload,
        int retryCount
) {
}
