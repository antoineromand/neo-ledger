package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.TechnicalException;

public class MissingParserException extends TechnicalException {
    public MissingParserException(Throwable cause) {
        super("Parser not implemented", cause);
    }
}
