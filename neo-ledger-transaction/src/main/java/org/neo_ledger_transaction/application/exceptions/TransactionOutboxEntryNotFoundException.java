package org.neo_ledger_transaction.application.exceptions;

import java.util.UUID;
import org.neo_ledger.common.exceptions.TechnicalException;

public class TransactionOutboxEntryNotFoundException extends TechnicalException {
  public TransactionOutboxEntryNotFoundException(UUID id) {
    super("Transaction outbox entry not found: " + id, null);
  }
}
