package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.application.service.sepa.SepaPain001Parser;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SepaPain001ParserUnitTest {
    private final SepaPain001Parser parser = new SepaPain001Parser();

    @Test
    public void shouldParseXml() throws XMLStreamException {
        InputStream targetStream = getClass().getResourceAsStream("/sepa-001-sample.xml");
        assertNotNull(targetStream);

        var result = this.parser.parse(targetStream);

        assertNotNull(result);

        assertEquals(1, result.transactions().size());
        assertEquals("MSG-TX-REF-2026-001", result.header().msgId());
        assertEquals(LocalDateTime.parse("2026-03-14T17:15:00.000"), result.header().creationDateTime());
    }
}
