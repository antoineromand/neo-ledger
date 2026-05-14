package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.TechnicalException;

public class TransactionOutboxStateException extends TechnicalException {
  public TransactionOutboxStateException(Throwable cause) {
    super("Error while updating the outbox state.", cause);
  }
}
