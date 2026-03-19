package org.neo_ledger.common.exceptions;

public abstract class BusinessException extends ApplicationException {
    protected BusinessException(String message) {
        super(message);
    }
}
