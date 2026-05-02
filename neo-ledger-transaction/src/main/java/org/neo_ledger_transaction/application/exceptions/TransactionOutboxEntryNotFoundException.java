package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.TechnicalException;

import java.util.UUID;

public class TransactionOutboxEntryNotFoundException extends TechnicalException {
    public TransactionOutboxEntryNotFoundException(UUID id) {
        super("Transaction outbox entry not found: " + id, null);
    }
}
