package org.neo_ledger_transaction.domain.port.out;

import java.util.UUID;

public interface TransactionOutboxPort {
    void save(UUID aggregateId, String eventType, byte[] payload);
}
