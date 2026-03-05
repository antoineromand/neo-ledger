package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.neo_ledger_transaction.application.service.sepa.SepaPain008Parser;
import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionEventPublisher;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class IngestionServiceUnitTest {

    private final PaymentParserFactory factory = new PaymentParserFactory(List.of(new SepaPain008Parser()));
    private final TransactionEventPublisher transactionEventPublisher = mock(TransactionEventPublisher.class);
    private final IngestionService ingestionService = new IngestionService(factory, transactionEventPublisher);

    @Test
    @DisplayName("Should parse SEPA PAIN.008 file and publish all transactions to the event publisher")
    public void should_parse_and_publish_transactions() throws IOException, XMLStreamException {
        try (InputStream targetStream = getClass().getClassLoader().getResourceAsStream("sepa-008-sample.xml")) {
            if (targetStream == null) {
                throw new IllegalStateException("Test file sepa-008-sample.xml not found in src/test/resources");
            }

            ArgumentCaptor<RawTransaction> captor = ArgumentCaptor.forClass(RawTransaction.class);

            ingestionService.executeIngestion(targetStream);

            verify(transactionEventPublisher, times(2)).publish(captor.capture());

            List<RawTransaction> publishedTransactions = captor.getAllValues();
            RawSepaTransaction firstTx = (RawSepaTransaction) publishedTransactions.getFirst();

            assertEquals(new BigDecimal("6543.14"), firstTx.amount());
            assertEquals("EUR", firstTx.currency());
            assertEquals("DE00ZZZ00099999999", firstTx.creditorSchemeId()); // Vérifie que ton parseSpecificId marche !
        }
    }

    @Test
    @DisplayName("Should throw exception when payment type is unknown")
    public void should_throw_exception_on_unknown_namespace() throws Exception {
        String invalidXml = "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:unknown.format\"></Document>";
        InputStream is = new java.io.ByteArrayInputStream(invalidXml.getBytes());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ingestionService.executeIngestion(is);
        });

        verifyNoInteractions(transactionEventPublisher);
    }
}