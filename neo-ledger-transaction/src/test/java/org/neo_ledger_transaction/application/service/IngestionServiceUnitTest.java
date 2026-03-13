package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.neo_ledger_transaction.application.service.sepa.SepaPain008Parser;
import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionEventPublisher;
import org.neo_ledger_transaction.domain.port.out.XmlValidator;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class IngestionServiceUnitTest {

    private final PaymentParserFactory factory = new PaymentParserFactory(List.of(new SepaPain008Parser()));
    private final TransactionEventPublisher transactionEventPublisher = mock(TransactionEventPublisher.class);
    private final XmlValidator xmlValidator = mock(XmlValidator.class);
    private final IngestionService ingestionService =
            new IngestionService(factory, transactionEventPublisher, xmlValidator);

    @Test
    @DisplayName("Should validate, parse SEPA PAIN.008 and publish all transactions")
    void should_validate_parse_and_publish_transactions() throws IOException, XMLStreamException {
        try (InputStream targetStream = getClass().getResourceAsStream("/sepa-008-sample.xml")) {
            assertNotNull(targetStream, "Test file sepa-008-sample.xml not found");

            ingestionService.executeIngestion(targetStream);

            verify(xmlValidator, times(1))
                    .validate(any(InputStream.class), eq("SEPA_PAIN_008"));

            ArgumentCaptor<RawTransaction> captor = ArgumentCaptor.forClass(RawTransaction.class);
            verify(transactionEventPublisher, times(1)).publish(captor.capture());

            List<RawTransaction> publishedTransactions = captor.getAllValues();
            assertEquals(1, publishedTransactions.size());

            RawSepaTransaction firstTx = (RawSepaTransaction) publishedTransactions.get(0);

            assertEquals("E2E-001", firstTx.endToEndId());
            assertEquals(new BigDecimal("100.00"), firstTx.amount());
            assertEquals("EUR", firstTx.currency());
            assertEquals("DE21500500009876543210", firstTx.debtorIban());
            assertEquals("DE87200500001234567890", firstTx.creditorIban());
            assertEquals("MANDATE-001", firstTx.mandateId());
            assertEquals("Test remittance", firstTx.remittanceInfo());
        }
    }

    @Test
    @DisplayName("Should stop process if validation fails")
    void should_stop_when_validation_fails() {
        doThrow(new RuntimeException("XSD Validation Error"))
                .when(xmlValidator).validate(any(InputStream.class), anyString());

        String validPain008Xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.008.001.11">
                    <CstmrDrctDbtInitn>
                        <GrpHdr>
                            <MsgId>MSG-001</MsgId>
                            <CreDtTm>2026-03-13T10:30:00</CreDtTm>
                            <NbOfTxs>1</NbOfTxs>
                            <InitgPty>
                                <Nm>Initiator Name</Nm>
                            </InitgPty>
                        </GrpHdr>
                        <PmtInf>
                            <PmtInfId>PMTINF-001</PmtInfId>
                            <PmtMtd>DD</PmtMtd>
                            <ReqdColltnDt>2026-03-20</ReqdColltnDt>
                            <Cdtr>
                                <Nm>Creditor Name</Nm>
                            </Cdtr>
                            <CdtrAcct>
                                <Id>
                                    <IBAN>DE87200500001234567890</IBAN>
                                </Id>
                            </CdtrAcct>
                            <CdtrAgt>
                                <FinInstnId>
                                    <BICFI>BANKDEFFXXX</BICFI>
                                </FinInstnId>
                            </CdtrAgt>
                            <DrctDbtTxInf>
                                <PmtId>
                                    <EndToEndId>E2E-001</EndToEndId>
                                </PmtId>
                                <InstdAmt Ccy="EUR">100.00</InstdAmt>
                                <DbtrAgt>
                                    <FinInstnId>
                                        <BICFI>SPUEDE2UXXX</BICFI>
                                    </FinInstnId>
                                </DbtrAgt>
                                <Dbtr>
                                    <Nm>Debtor Name</Nm>
                                </Dbtr>
                                <DbtrAcct>
                                    <Id>
                                        <IBAN>DE21500500009876543210</IBAN>
                                    </Id>
                                </DbtrAcct>
                            </DrctDbtTxInf>
                        </PmtInf>
                    </CstmrDrctDbtInitn>
                </Document>
                """;

        InputStream is = new ByteArrayInputStream(validPain008Xml.getBytes(StandardCharsets.UTF_8));

        assertThrows(RuntimeException.class, () -> ingestionService.executeIngestion(is));

        verify(xmlValidator, times(1)).validate(any(InputStream.class), eq("SEPA_PAIN_008"));
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

        assertThrows(IllegalArgumentException.class, () -> ingestionService.executeIngestion(is));

        verifyNoInteractions(xmlValidator);
        verifyNoInteractions(transactionEventPublisher);
    }
}