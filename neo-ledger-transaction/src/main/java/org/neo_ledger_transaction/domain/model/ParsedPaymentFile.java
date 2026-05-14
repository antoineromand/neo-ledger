package org.neo_ledger_transaction.domain.model;

import java.util.List;

public record ParsedPaymentFile<T>(ParsedFileHeader header, List<T> transactions) {
  public ParsedPaymentFile {
    transactions = List.copyOf(transactions);
  }
}
