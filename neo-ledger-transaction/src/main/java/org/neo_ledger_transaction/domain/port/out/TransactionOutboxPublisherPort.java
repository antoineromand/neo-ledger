package org.neo_ledger_transaction.domain.port.out;

public interface TransactionOutboxPublisherPort {
    void publish(String routingKey, String messageKey, byte[] payload);
}
