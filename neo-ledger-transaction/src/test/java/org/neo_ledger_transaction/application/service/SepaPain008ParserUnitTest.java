package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.application.service.sepa.SepaPain008Parser;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SepaPain008ParserUnitTest {
    private final SepaPain008Parser parser = new SepaPain008Parser();

    @Test
    @DisplayName("Should parse SEPA PAIN.008 file")
    void shouldParseSepaPain008() throws FileNotFoundException, XMLStreamException {
        File initialFile = new File("src/test/resources/sepa-008-sample.xml");
        InputStream targetStream = new FileInputStream(initialFile);

        var result = this.parser.parse(targetStream);

        assertEquals("Message-ID", result.header().msgId());
        assertEquals(2.0, result.header().expectedNbTxs());
        assertEquals(LocalDateTime.parse("2010-11-21T09:30:47.000Z", DateTimeFormatter.ISO_DATE_TIME), result.header().creationDateTime());

        assertEquals(2, result.transactions().size());
        assertEquals(result.header().expectedNbTxs(), result.transactions().size());
        var firstTransaction = result.transactions().getFirst();
        assertEquals("DE21500500009876543210", firstTransaction.debtorIban());
        assertEquals("EUR", firstTransaction.currency());
        assertEquals(BigDecimal.valueOf(6543.14), firstTransaction.amount());

        var secondTransaction = result.transactions().get(1);
        assertEquals("DE21500500001234567897", secondTransaction.debtorIban());
        assertEquals("EUR", secondTransaction.currency());
        assertEquals(BigDecimal.valueOf(112.72), secondTransaction.amount());
    }
}
