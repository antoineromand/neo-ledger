package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.TechnicalException;

public class EventPublishingException extends TechnicalException {
    public EventPublishingException(Throwable cause) {
        super("Error while publishing transaction to its topic.", cause);
    }
}
