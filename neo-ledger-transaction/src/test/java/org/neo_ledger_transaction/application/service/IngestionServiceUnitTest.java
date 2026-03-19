package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.neo_ledger_transaction.application.service.factory.PaymentParserFactory;
import org.neo_ledger_transaction.application.service.factory.XmlValidatorFactory;
import org.neo_ledger_transaction.application.service.sepa.SepaPain001Parser;
import org.neo_ledger_transaction.application.service.sepa.SepaPain008Parser;
import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionEventPublisher;
import org.neo_ledger_transaction.infrastructure.validator.XsdSepaValidator;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IngestionServiceUnitTest {

    private final SepaPain008Parser sepaPain008parser = new SepaPain008Parser();
    private final SepaPain001Parser sepaPain001parser = new SepaPain001Parser();
    private final PaymentParserFactory paymentFactory = new PaymentParserFactory(List.of(sepaPain008parser, sepaPain001parser));

    private final XsdSepaValidator xsdSepaValidator = new XsdSepaValidator();
    private final XmlValidatorFactory xmlValidatorFactory = new XmlValidatorFactory(List.of(xsdSepaValidator));

    private final TransactionEventPublisher transactionEventPublisher = mock(TransactionEventPublisher.class);

    private final IngestionService ingestionService =
            new IngestionService(paymentFactory, transactionEventPublisher, xmlValidatorFactory);

    public IngestionServiceUnitTest() throws SAXException {
    }

    @Test
    @DisplayName("Should validate, parse SEPA PAIN.008 and publish all transactions")
    void should_validate_parse_pain_008_and_publish_transactions() throws IOException, ParserConfigurationException {
        try (InputStream targetStream = getClass().getResourceAsStream("/sepa-008-sample.xml")) {
            assertNotNull(targetStream, "Test file sepa-008-sample.xml not found");

            ingestionService.executeIngestion(targetStream);

            ArgumentCaptor<RawTransaction> captor = ArgumentCaptor.forClass(RawTransaction.class);
            verify(transactionEventPublisher, atLeastOnce()).publish(captor.capture(), any(String.class));

            List<RawTransaction> publishedTransactions = captor.getAllValues();
            assertFalse(publishedTransactions.isEmpty());

            RawSepaTransaction firstTx = (RawSepaTransaction) publishedTransactions.getFirst();

            assertNotNull(firstTx.endToEndId());
            assertNotNull(firstTx.amount());
        }
    }

    @Test
    @DisplayName("Should validate, parse SEPA PAIN.001 and publish all transactions")
    void should_validate_parse_pain_001_and_publish_transactions() throws IOException, ParserConfigurationException {
        try (InputStream targetStream = getClass().getResourceAsStream("/sepa-001-sample.xml")) {
            assertNotNull(targetStream, "Test file sepa-001-sample.xml not found");

            ingestionService.executeIngestion(targetStream);

            ArgumentCaptor<RawTransaction> captor = ArgumentCaptor.forClass(RawTransaction.class);
            verify(transactionEventPublisher, atLeastOnce()).publish(captor.capture(), any(String.class));

            List<RawTransaction> publishedTransactions = captor.getAllValues();
            assertFalse(publishedTransactions.isEmpty());

            RawSepaTransaction firstTx = (RawSepaTransaction) publishedTransactions.getFirst();

            assertNotNull(firstTx.endToEndId());
            assertNotNull(firstTx.amount());
        }
    }


    @Test
    @DisplayName("Should stop process if validation fails (Inversion 001/008)")
    void should_stop_when_validation_fails() {
        String invalidPain008Xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03">
                    <CstmrDrctDbtInitn>
                        <GrpHdr><MsgId>INVALID</MsgId></GrpHdr>
                    </CstmrDrctDbtInitn>
                </Document>
                """;

        InputStream is = new ByteArrayInputStream(invalidPain008Xml.getBytes(StandardCharsets.UTF_8));

        assertThrows(Exception.class, () -> ingestionService.executeIngestion(is));

        verifyNoInteractions(transactionEventPublisher);
    }

    @Test
    @DisplayName("Should throw exception when payment type is unknown")
    void should_throw_exception_on_unknown_namespace() {
        String invalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:unknown.format"></Document>
                """;

        InputStream is = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));

        assertThrows(Exception.class, () -> ingestionService.executeIngestion(is));

        verifyNoInteractions(transactionEventPublisher);
    }
}