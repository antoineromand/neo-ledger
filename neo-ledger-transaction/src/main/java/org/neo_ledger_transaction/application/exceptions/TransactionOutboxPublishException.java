package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.TechnicalException;

public class TransactionOutboxPublishException extends TechnicalException {
  public TransactionOutboxPublishException(Throwable cause) {
    super("Error while publishing a transaction from the outbox.", cause);
  }
}
