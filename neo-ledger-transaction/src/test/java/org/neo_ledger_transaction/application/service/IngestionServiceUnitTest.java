package org.neo_ledger_transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import jakarta.validation.ValidationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.application.exceptions.InconsistentPaymentFileException;
import org.neo_ledger_transaction.application.exceptions.InputStreamTechnicalException;
import org.neo_ledger_transaction.application.exceptions.UnsupportedPaymentFormatException;
import org.neo_ledger_transaction.application.port.out.PaymentFileParser;
import org.neo_ledger_transaction.application.service.factory.PaymentParserFactory;
import org.neo_ledger_transaction.application.service.factory.XmlValidatorFactory;
import org.neo_ledger_transaction.domain.model.ParsedFileHeader;
import org.neo_ledger_transaction.domain.model.ParsedPaymentFile;
import org.neo_ledger_transaction.domain.model.ParsedSepaTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxPort;
import org.neo_ledger_transaction.domain.port.out.XmlValidator;
import org.neo_ledger_transaction.infrastructure.parser.sepa.SepaPain001Parser;
import org.neo_ledger_transaction.infrastructure.parser.sepa.SepaPain008Parser;
import org.neo_ledger_transaction.infrastructure.transport.publisher.TransactionMapperFactory;
import org.neo_ledger_transaction.infrastructure.validator.XsdSepaValidator;
import org.xml.sax.SAXException;

public class IngestionServiceUnitTest {

  private final TransactionMapperFactory transactionMapperFactory =
      mock(TransactionMapperFactory.class);
  private final XmlValidatorFactory xmlValidatorFactory = mock(XmlValidatorFactory.class);
  private final PaymentParserFactory paymentParserFactory = mock(PaymentParserFactory.class);
  private final TransactionOutboxPort transactionOutboxPort = mock(TransactionOutboxPort.class);

  private final SepaPain008Parser sepaPain008parser = new SepaPain008Parser();
  private final SepaPain001Parser sepaPain001parser = new SepaPain001Parser();
  private final XsdSepaValidator xsdSepaValidator = new XsdSepaValidator();

  private final IngestionService ingestionService =
      new IngestionService(
          paymentParserFactory,
          transactionMapperFactory,
          xmlValidatorFactory,
          transactionOutboxPort);

  public IngestionServiceUnitTest() throws SAXException {}

  @Test
  void should_parse_and_publish_pain_008() throws Exception {
    try (InputStream is = getClass().getResourceAsStream("/sepa-008-sample.xml")) {
      assertNotNull(is);
      when(xmlValidatorFactory.getValidator("SEPA_PAIN_008")).thenReturn(xsdSepaValidator);
      when(paymentParserFactory.getParser("SEPA_PAIN_008"))
          .thenReturn((PaymentFileParser) sepaPain008parser);

      ingestionService.executeIngestion(is);

      verify(transactionMapperFactory, atLeastOnce()).toBinary(any());
    }
  }

  @Test
  @DisplayName("Should validate, parse SEPA PAIN.001 and publish all transactions")
  void should_validate_parse_pain_001_and_publish_transactions()
      throws IOException, ParserConfigurationException {
    try (InputStream is = getClass().getResourceAsStream("/sepa-001-sample.xml")) {
      assertNotNull(is);
      when(xmlValidatorFactory.getValidator("SEPA_PAIN_001")).thenReturn(xsdSepaValidator);
      when(paymentParserFactory.getParser("SEPA_PAIN_001"))
          .thenReturn((PaymentFileParser) sepaPain001parser);

      ingestionService.executeIngestion(is);

      verify(transactionMapperFactory, atLeastOnce()).toBinary(any());
    }
  }

  @Test
  @DisplayName("Should stop process if parser not found")
  void should_stop_process_if_parser_not_found() throws IOException, ParserConfigurationException {
    try (InputStream targetStream = getClass().getResourceAsStream("/sepa-008-sample.xml")) {
      assertNotNull(targetStream, "Test file sepa-008-sample.xml not found");

      XmlValidator mockValidator = mock(XmlValidator.class);
      when(this.xmlValidatorFactory.getValidator(anyString())).thenReturn(mockValidator);
      when(paymentParserFactory.getParser("SEPA_PAIN_008"))
          .thenThrow(new ParserConfigurationException("Parser not found"));
      assertThrows(
          ParserConfigurationException.class,
          () -> ingestionService.executeIngestion(targetStream));

      verify(mockValidator).validate(any(InputStream.class), eq("SEPA_PAIN_008"));
      verifyNoInteractions(transactionMapperFactory);
    }
  }

  @Test
  @DisplayName("Should stop process if validation fails")
  void should_stop_when_validation_fails() {
    String invalidXml =
        """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03">
                    <CstmrDrctDbtInitn>
                        <GrpHdr><MsgId>INVALID</MsgId></GrpHdr>
                    </CstmrDrctDbtInitn>
                </Document>
                """;

    InputStream is = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));
    XmlValidator mockValidator = mock(XmlValidator.class);
    when(this.xmlValidatorFactory.getValidator(anyString())).thenReturn(mockValidator);
    doThrow(new ValidationException())
        .when(mockValidator)
        .validate(any(InputStream.class), anyString());
    assertThrows(ValidationException.class, () -> ingestionService.executeIngestion(is));
    verifyNoInteractions(transactionMapperFactory, paymentParserFactory);
  }

  @Test
  @DisplayName("Should throw exception when payment type is unknown")
  void should_throw_exception_on_unknown_namespace() {
    String invalidXml =
        """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:unknown.format"></Document>
                """;

    InputStream is = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        UnsupportedPaymentFormatException.class, () -> ingestionService.executeIngestion(is));

    verifyNoInteractions(transactionMapperFactory, xmlValidatorFactory, paymentParserFactory);
  }

  @Test
  @DisplayName("Should throw input stream technical exception when input stream is malformed")
  void should_throw_input_stream_technical_exception() throws IOException {
    InputStream is = mock(InputStream.class);
    when(is.readAllBytes()).thenThrow(new IOException("Issue while reading"));

    assertThrows(InputStreamTechnicalException.class, () -> ingestionService.executeIngestion(is));
    verifyNoInteractions(transactionMapperFactory, xmlValidatorFactory, paymentParserFactory);
  }

  @Test
  @DisplayName(
      "Should throw input stream technical exception when input cannot be read with XMLReader")
  void should_throw_input_stream_technical_exception_with_xml_reader()
      throws IOException, XMLStreamException {
    String corruptedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Document xmlns=\"urn:iso:";
    InputStream is = new ByteArrayInputStream(corruptedXml.getBytes(StandardCharsets.UTF_8));
    assertThrows(InputStreamTechnicalException.class, () -> ingestionService.executeIngestion(is));
    verifyNoInteractions(transactionMapperFactory, xmlValidatorFactory, paymentParserFactory);
  }

  @Test
  @DisplayName(
      "Should stop process when declared transaction count does not match parsed transactions")
  void should_stop_when_expected_transaction_count_does_not_match_parsed_transactions()
      throws Exception {
    try (InputStream is = getClass().getResourceAsStream("/sepa-008-sample.xml")) {
      assertNotNull(is, "Test file sepa-008-sample.xml not found");

      XmlValidator mockValidator = mock(XmlValidator.class);
      @SuppressWarnings("unchecked")
      PaymentFileParser<ParsedPaymentFile<ParsedSepaTransaction>> mockParser =
          mock(PaymentFileParser.class);

      when(this.xmlValidatorFactory.getValidator("SEPA_PAIN_008")).thenReturn(mockValidator);
      when(this.paymentParserFactory.getParser("SEPA_PAIN_008"))
          .thenReturn((PaymentFileParser) mockParser);
      doNothing().when(mockValidator).validate(any(InputStream.class), eq("SEPA_PAIN_008"));

      when(mockParser.parse(any(InputStream.class)))
          .thenReturn(
              new ParsedPaymentFile<>(
                  new ParsedFileHeader("MSG-1", 2, LocalDateTime.parse("2026-05-02T10:00:00")),
                  List.of(
                      new ParsedSepaTransaction(
                          "E2E-1",
                          "FR7612345678901234567890185",
                          "FR7612345678901234567890186",
                          BigDecimal.ONE,
                          "EUR",
                          LocalDate.parse("2026-05-03"),
                          false,
                          "info",
                          "MANDATE-1",
                          "SCHEME-1",
                          "SEPA_PAIN_008"))));

      assertThrows(
          InconsistentPaymentFileException.class, () -> ingestionService.executeIngestion(is));

      verify(mockValidator).validate(any(InputStream.class), eq("SEPA_PAIN_008"));
      verify(mockParser).parse(any(InputStream.class));
      verifyNoInteractions(transactionMapperFactory, transactionOutboxPort);
    }
  }
}
