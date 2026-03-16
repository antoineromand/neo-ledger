package org.neo_ledger_transaction.application.service;

import org.neo_ledger_transaction.application.PaymentFileType;
import org.neo_ledger_transaction.application.port.in.IngestionUseCasePort;
import org.neo_ledger_transaction.application.service.factory.PaymentParserFactory;
import org.neo_ledger_transaction.application.service.factory.XmlValidatorFactory;
import org.neo_ledger_transaction.domain.model.RawPaymentFile;
import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionEventPublisher;
import org.neo_ledger_transaction.domain.port.out.XmlValidator;
import org.neo_ledger_transaction.domain.service.PaymentParser;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Application service responsible for the ingestion of payment files.
 * <p>
 * This service orchestrates the format detection process, data parsing,
 * and transaction publication to third-party systems via an output port.
 * </p>
 */
@Service
public class IngestionService implements IngestionUseCasePort {

    private final PaymentParserFactory paymentFactory;
    private final TransactionEventPublisher eventPublisher;
    private final XmlValidatorFactory xmlValidatorFactory;

    /**
     * Dependency injection constructor.
     *
     * @param paymentParserFactory Factory to retrieve the appropriate parser for the file type.
     * @param eventPublisher       Output port for publishing transaction events (e.g., Kafka).
     * @param xmlValidatorFactory  Factory to retrieve the appropriate validator.
     */
    public IngestionService(PaymentParserFactory paymentParserFactory, TransactionEventPublisher eventPublisher, XmlValidatorFactory xmlValidatorFactory) {
        this.paymentFactory = paymentParserFactory;
        this.eventPublisher = eventPublisher;
        this.xmlValidatorFactory = xmlValidatorFactory;
    }

    /**
     * Executes the complete ingestion workflow for a payment file.
     * <p>
     * The process uses a {@link ByteArrayInputStream} to allow three consecutive reads:
     * 1. A full read to detect the file type (XML Namespace).
     * 2. A full read to validate the XML structure.
     * 3. A full read for parsing and transaction extraction.
     * </p>
     *
     * @param file The binary stream (InputStream) of the file to process.
     * @throws XMLStreamException If the XML content is invalid or corrupted.
     * @throws IOException        In case of stream reading issues.
     */
    @Override
    public void executeIngestion(InputStream file) throws XMLStreamException, IOException {
        byte[] xmlContent = file.readAllBytes();

        String paymentType = this.detectPaymentType(new ByteArrayInputStream(xmlContent));

        XmlValidator xmlValidator = this.xmlValidatorFactory.getValidator(paymentType);

        xmlValidator.validate(new ByteArrayInputStream(xmlContent), paymentType);

        PaymentParser<RawPaymentFile<? extends RawTransaction>> parser =
                (PaymentParser<RawPaymentFile<? extends RawTransaction>>) this.paymentFactory.getParser(paymentType);
        RawPaymentFile<? extends RawTransaction> res = parser.parse(new ByteArrayInputStream(xmlContent));

        res.transactions().forEach(transaction -> this.eventPublisher.publish(transaction, paymentType));
    }

    /**
     * Analyzes the beginning of the XML stream to identify the document's Namespace.
     * <p>
     * This method looks for the {@code <Document>} root tag and uses its
     * namespace URI to map it to a {@link PaymentFileType}.
     * </p>
     *
     * @param stream The stream to analyze.
     * @return The name (String) of the detected payment type.
     * @throws XMLStreamException       If the XML structure does not allow detection.
     * @throws IllegalArgumentException If the detected namespace is unknown.
     */
    private String detectPaymentType(InputStream stream) throws XMLStreamException {
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xif.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        XMLStreamReader r = xif.createXMLStreamReader(stream);

        while (r.hasNext()) {
            if (r.next() == XMLStreamConstants.START_ELEMENT && "Document".equals(r.getLocalName())) {
                return PaymentFileType.fromNamespace(r.getNamespaceURI()).name();
            }
        }
        throw new IllegalArgumentException("Unknown payment type");
    }
}