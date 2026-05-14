package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.TechnicalException;

public class TransactionOutboxClaimException extends TechnicalException {
  public TransactionOutboxClaimException(Throwable cause) {
    super("Error while claiming transactions from the outbox.", cause);
  }
}
