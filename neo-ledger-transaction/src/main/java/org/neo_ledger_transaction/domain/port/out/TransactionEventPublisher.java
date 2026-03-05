package org.neo_ledger_transaction.domain.port.out;

import org.neo_ledger_transaction.domain.model.RawTransaction;

public interface TransactionEventPublisher {
    /**
     * Publie une transaction de manière agnostique.
     * Peu importe que ce soit SEPA, Swift ou autre.
     */
    void publish(RawTransaction transaction);
}
