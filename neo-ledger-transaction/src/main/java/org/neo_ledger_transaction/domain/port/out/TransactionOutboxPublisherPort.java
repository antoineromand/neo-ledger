package org.neo_ledger_transaction.domain.port.out;

/** Output port used by the relay use case to publish a message to the broker. */
public interface TransactionOutboxPublisherPort {
  /** Publishes a message with a stable routing key and idempotent message key. */
  void publish(String routingKey, String messageKey, byte[] payload);
}
