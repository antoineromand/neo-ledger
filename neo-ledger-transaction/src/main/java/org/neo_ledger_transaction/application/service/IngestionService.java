package org.neo_ledger_transaction.application.service;

import org.neo_ledger_transaction.application.PaymentFileType;
import org.neo_ledger_transaction.application.port.in.IngestionUseCasePort;
import org.neo_ledger_transaction.domain.model.RawPaymentFile;
import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.service.PaymentParser;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
public class IngestionService implements IngestionUseCasePort {

    private final PaymentParserFactory factory;
    public IngestionService(PaymentParserFactory paymentParserFactory) {
        this.factory = paymentParserFactory;
    }

    @Override
    public void executeIngestion(InputStream file) throws XMLStreamException, IOException {
        BufferedInputStream bis = new BufferedInputStream(file);
        bis.mark(4096);
        String paymentType = this.detectPaymentType(bis);
        bis.reset();

        PaymentParser<RawPaymentFile<? extends RawTransaction>> parser =
                (PaymentParser<RawPaymentFile<? extends RawTransaction>>) this.factory.getParser(paymentType);

        RawPaymentFile<? extends RawTransaction> res = parser.parse(bis);

        res.transactions().forEach(tx -> {
            System.out.println("Processing TX: " + tx.endToEndId() + " Amount: " + tx.amount());
        });

    }

    private String detectPaymentType(InputStream stream) throws XMLStreamException {
        XMLInputFactory xif = XMLInputFactory.newFactory();
        XMLStreamReader r = xif.createXMLStreamReader(stream);

        while (r.hasNext()) {
            if (r.next() == XMLStreamConstants.START_ELEMENT && "Document".equals(r.getLocalName())) {
                return PaymentFileType.fromNamespace(r.getNamespaceURI()).name();
            }
        }
        throw new IllegalArgumentException("Unknown payment type");
    }
}
