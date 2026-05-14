package org.neo_ledger_transaction.application.exceptions;

import org.neo_ledger.common.exceptions.BusinessException;

/**
 * Raised when a parsed payment file declares a number of transactions that does not match the
 * actual parsed transaction count.
 */
public class InconsistentPaymentFileException extends BusinessException {

  public InconsistentPaymentFileException(int expectedNbTxs, int actualNbTxs) {
    super(
        "Inconsistent payment file: expected "
            + expectedNbTxs
            + " transactions but parsed "
            + actualNbTxs
            + ".");
  }
}
