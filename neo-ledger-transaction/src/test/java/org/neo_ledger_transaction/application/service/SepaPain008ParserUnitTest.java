package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.application.service.sepa.SepaPain008Parser;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SepaPain008ParserUnitTest {
    private final SepaPain008Parser parser = new SepaPain008Parser();

    @Test
    @DisplayName("Should parse SEPA PAIN.008 file")
    void shouldParseSepaPain008() throws XMLStreamException {
        InputStream targetStream = getClass().getResourceAsStream("/sepa-008-sample.xml");
        assertNotNull(targetStream);

        var result = this.parser.parse(targetStream);

        assertNotNull(result);
        assertNotNull(result.header());

        assertEquals("MSG-001", result.header().msgId());
        assertEquals(1, result.header().expectedNbTxs());
        assertEquals(LocalDateTime.parse("2026-03-13T10:30:00"), result.header().creationDateTime());

        assertEquals(1, result.transactions().size());
        assertEquals(result.header().expectedNbTxs(), result.transactions().size());

        var firstTransaction = result.transactions().getFirst();

        assertEquals("E2E-001", firstTransaction.endToEndId());
        assertEquals("DE21500500009876543210", firstTransaction.debtorIban());
        assertEquals("DE87200500001234567890", firstTransaction.creditorIban());
        assertEquals(new BigDecimal("100.00"), firstTransaction.amount());
        assertEquals("EUR", firstTransaction.currency());
        assertEquals(LocalDate.parse("2026-03-20"), firstTransaction.requestedDate());
        assertEquals("MANDATE-001", firstTransaction.mandateId());
        assertEquals("Test remittance", firstTransaction.remittanceInfo());

    }
}
