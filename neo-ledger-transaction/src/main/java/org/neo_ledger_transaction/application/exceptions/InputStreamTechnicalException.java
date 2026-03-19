package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.TechnicalException;

public class InputStreamTechnicalException extends TechnicalException {
    public InputStreamTechnicalException(Throwable cause) {
        super("Error while reading this input stream", cause);
    }
}
