package org.neo_ledger_transaction.domain.port.out;

import org.neo_ledger_transaction.domain.model.RawTransaction;

/**
 * Output port for publishing transaction-related events.
 */
public interface TransactionEventPublisher {

    /**
     * Publishes a transaction in a format-agnostic manner.
     * <p>
     * This method handles transactions regardless of their origin or standard
     * (e.g., SEPA, SWIFT, or others).
     * </p>
     *
     * @param transaction The raw transaction data to be published.
     * @param topic       The destination topic or channel where the event should be sent.
     */
    void publish(RawTransaction transaction, String topic);
}