package org.neo_ledger.common.exceptions;

public abstract class TechnicalException extends ApplicationException {
    protected TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
