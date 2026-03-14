package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.neo_ledger_transaction.application.service.factory.PaymentParserFactory;
import org.neo_ledger_transaction.application.service.factory.XmlValidatorFactory;
import org.neo_ledger_transaction.application.service.sepa.SepaPain008Parser;
import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionEventPublisher;
import org.neo_ledger_transaction.infrastructure.validator.XsdSepaValidator;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IngestionServiceUnitTest {

    // Initialisation des Factories avec les implémentations réelles
    private final PaymentParserFactory paymentFactory = new PaymentParserFactory(List.of(new SepaPain008Parser()));

    // Pour le validateur, on utilise l'implémentation réelle que tu as fixée
    private final XsdSepaValidator xsdSepaValidator = new XsdSepaValidator();
    private final XmlValidatorFactory xmlValidatorFactory = new XmlValidatorFactory(List.of(xsdSepaValidator));

    // Le publisher reste un mock car c'est une sortie (Port Out)
    private final TransactionEventPublisher transactionEventPublisher = mock(TransactionEventPublisher.class);

    private final IngestionService ingestionService =
            new IngestionService(paymentFactory, transactionEventPublisher, xmlValidatorFactory);

    public IngestionServiceUnitTest() throws SAXException {
        // Constructeur nécessaire pour gérer l'exception du XsdSepaValidator
    }

    @Test
    @DisplayName("Should validate, parse SEPA PAIN.008 and publish all transactions")
    void should_validate_parse_and_publish_transactions() throws IOException, XMLStreamException {
        try (InputStream targetStream = getClass().getResourceAsStream("/sepa-008-sample.xml")) {
            assertNotNull(targetStream, "Test file sepa-008-sample.xml not found");

            ingestionService.executeIngestion(targetStream);

            // On vérifie que le publisher a bien reçu les données mappées
            ArgumentCaptor<RawTransaction> captor = ArgumentCaptor.forClass(RawTransaction.class);
            verify(transactionEventPublisher, atLeastOnce()).publish(captor.capture());

            List<RawTransaction> publishedTransactions = captor.getAllValues();
            assertFalse(publishedTransactions.isEmpty());

            RawSepaTransaction firstTx = (RawSepaTransaction) publishedTransactions.get(0);

            // Les assertions dépendent des valeurs exactes dans ton sepa-008-sample.xml
            assertNotNull(firstTx.endToEndId());
            assertNotNull(firstTx.amount());
        }
    }

    @Test
    @DisplayName("Should stop process if validation fails (Inversion 001/008)")
    void should_stop_when_validation_fails() {
        // Ici, on teste la sécurité que tu viens de coder !
        // On simule un XML qui se prétend être du 008 mais on utilise un XSD de 001 (ou vice versa)
        // En fonction de ta logique de factory, on passe un XML dont le namespace ne correspondra pas

        String invalidPain008Xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03">
                    <CstmrDrctDbtInitn>
                        <GrpHdr><MsgId>INVALID</MsgId></GrpHdr>
                    </CstmrDrctDbtInitn>
                </Document>
                """;

        InputStream is = new ByteArrayInputStream(invalidPain008Xml.getBytes(StandardCharsets.UTF_8));

        // L'exception levée sera une ValidationException (ton wrapper)
        assertThrows(Exception.class, () -> ingestionService.executeIngestion(is));

        // On vérifie que rien n'a été publié à cause de l'erreur
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

        // C'est ta Factory qui va lancer une IllegalArgumentException ou une erreur de détection
        assertThrows(Exception.class, () -> ingestionService.executeIngestion(is));

        verifyNoInteractions(transactionEventPublisher);
    }
}