package org.neo_ledger_transaction.domain.port.out;

public interface TransactionOutboxPort {
    void save(String endToEndId, String eventType, byte[] payload);
}
