package org.neo_ledger_transaction.domain.model;

import java.util.List;

public record RawPaymentFile<T>(FileHeader header, List<T> transactions) {
    public RawPaymentFile {
        transactions = List.copyOf(transactions);
    }
}
