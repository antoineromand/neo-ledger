package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.ApplicationException;

public class UnsupportedPaymentFormatException extends ApplicationException {

    public UnsupportedPaymentFormatException() {
        super("Unsupported payment format");
    }

    public UnsupportedPaymentFormatException(Throwable cause) {
        super("Unsupported payment format", cause);
    }
}
