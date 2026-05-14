package org.neo_ledger_transaction.domain.port.out;

import org.neo_ledger_transaction.domain.model.ParsedTransaction;

/** Output port for publishing transaction-related events. */
public interface TransactionMapperFactoryPort {

  /**
   * Publishes a transaction in a format-agnostic manner.
   *
   * <p>This method handles transactions regardless of their origin or standard (e.g., SEPA, SWIFT,
   * or others).
   *
   * @param transaction The parsed transaction data to be published.
   */
  byte[] toBinary(ParsedTransaction transaction);
}
