package org.neo_ledger_transaction.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedSepaTransaction(
        String endToEndId,
        String debtorIban,
        String creditorIban,
        BigDecimal amount,
        String currency,
        LocalDate requestedDate,
        boolean isInstant,
        String remittanceInfo,
        String mandateId,
        String creditorSchemeId
) implements ParsedTransaction {

    @Override public String debtorIdentifier() { return debtorIban; }
    @Override public String creditorIdentifier() { return creditorIban; }
}
