package org.neo_ledger_transaction.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParsedSepaTransactionTest {

    @Test
    void should_throw_error_when_payment_type_is_null() {
        assertThrows(IllegalArgumentException.class, () -> new ParsedSepaTransaction(
                "E2E-123",
                "FR7612345678901234567890185",
                "FR7612345678901234567890186",
                new BigDecimal("42.50"),
                "EUR",
                LocalDate.of(2026, 5, 11),
                true,
                null,
                null,
                null,
                null
        ));
    }

    @Test
    void should_throw_error_when_payment_type_is_blank() {
        assertThrows(IllegalArgumentException.class, () -> new ParsedSepaTransaction(
                "E2E-123",
                "FR7612345678901234567890185",
                "FR7612345678901234567890186",
                new BigDecimal("42.50"),
                "EUR",
                LocalDate.of(2026, 5, 11),
                true,
                null,
                null,
                null,
                "   "
        ));
    }
}
