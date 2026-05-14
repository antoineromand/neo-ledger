package org.neo_ledger_transaction.infrastructure.transport.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.neo_ledger.common.event.RawSepaTransaction;
import org.neo_ledger_transaction.domain.model.ParsedSepaTransaction;

public class SepaTransactionMapperUnitTest {
  private final SepaTransactionMapper mapper = new SepaTransactionMapper();

  @Test
  public void should_serialize_all_fields_to_protobuf() throws InvalidProtocolBufferException {
    ParsedSepaTransaction transaction =
        new ParsedSepaTransaction(
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
            "SEPA_PAIN_008");

    byte[] bytes = mapper.toBinary(transaction);
    RawSepaTransaction protobuf = RawSepaTransaction.parseFrom(bytes);

    assertEquals("E2E-123", protobuf.getEndToEndId());
    assertEquals("FR7612345678901234567890185", protobuf.getDebtorIban());
    assertEquals("FR7612345678901234567890186", protobuf.getCreditorIban());
    assertEquals("42.50", protobuf.getAmount());
    assertEquals("EUR", protobuf.getCurrency());
    assertEquals("2026-05-11", protobuf.getRequestedDate());
    assertTrue(protobuf.getIsInstant());
    assertEquals("", protobuf.getRemittanceInfo());
    assertEquals("", protobuf.getMandateId());
    assertEquals("", protobuf.getCreditorSchemeId());
    assertEquals("SEPA_PAIN_008", protobuf.getPaymentType());
  }
}
