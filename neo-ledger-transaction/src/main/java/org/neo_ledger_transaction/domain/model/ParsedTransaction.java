package org.neo_ledger_transaction.domain.model;

import java.math.BigDecimal;

public interface ParsedTransaction {
    String endToEndId();
    BigDecimal amount();
    String currency();
    String debtorIdentifier();
    String creditorIdentifier();
}
