package org.neo_ledger_transaction.infrastructure.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.time.LocalDateTime;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.infrastructure.parser.sepa.SepaPain001Parser;

public class SepaPain001ParserUnitTest {
  private final SepaPain001Parser parser = new SepaPain001Parser();

  @Test
  public void shouldParseXml() throws XMLStreamException {
    InputStream targetStream = getClass().getResourceAsStream("/sepa-001-sample.xml");
    assertNotNull(targetStream);

    var result = this.parser.parse(targetStream);

    assertNotNull(result);

    assertEquals(1, result.transactions().size());
    assertEquals("MSG-001", result.header().msgId());
    assertEquals(
        LocalDateTime.parse("2026-04-17T10:15:30.000"), result.header().creationDateTime());
    assertEquals(1, result.transactions().size());

    var firstTransaction = result.transactions().getFirst();

    assertEquals("FR7630006000011234567890189", firstTransaction.debtorIdentifier());
    assertEquals("DE89370400440532013000", firstTransaction.creditorIdentifier());
    assertEquals("FAC-2026-00452", firstTransaction.remittanceInfo());
    assertEquals("E2E-001", firstTransaction.endToEndId());

    assertEquals("SEPA_PAIN_001", firstTransaction.paymentType());
  }
}
