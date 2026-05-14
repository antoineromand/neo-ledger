package org.neo_ledger_transaction.application.port.in;

/**
 * Application use case that drives the outbox relay loop.
 *
 * <p>The scheduler triggers this port, but the implementation stays free of infrastructure concerns
 * such as JPA or Kafka details.
 */
public interface TransactionOutboxRelayUseCasePort {
  void execute();
}
