package org.neo_ledger_transaction.domain.port.out;

public interface TransactionOutboxPort {
    void save(String endToEndId, String aggregateType, String eventType, byte[] payload);
}
