package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.TechnicalException;

public class WritingTransactionException extends TechnicalException {
    public WritingTransactionException(Throwable cause) {
        super("Error while writing a transaction in the outbox table.", cause);
    }
}
