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
        String creditorSchemeId,
        String paymentType
) implements ParsedTransaction {
    public ParsedSepaTransaction {
        if (paymentType == null || paymentType.isBlank()) {
            throw new IllegalArgumentException("paymentType must not be null or blank");
        }
    }
    @Override public String debtorIdentifier() { return debtorIban; }
    @Override public String creditorIdentifier() { return creditorIban; }
}
