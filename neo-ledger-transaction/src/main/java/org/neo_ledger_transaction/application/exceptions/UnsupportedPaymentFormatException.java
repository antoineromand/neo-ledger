package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.BusinessException;

public class UnsupportedPaymentFormatException extends BusinessException {

    public UnsupportedPaymentFormatException() {
        super("Unsupported payment format");
    }
}
