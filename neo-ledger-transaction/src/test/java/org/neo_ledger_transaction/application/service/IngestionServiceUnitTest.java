package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.application.service.sepa.SepaPain008Parser;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.List;

public class IngestionServiceUnitTest {
    private final PaymentParserFactory factory = new PaymentParserFactory(List.of(new SepaPain008Parser()));
    private final IngestionService ingestionService = new IngestionService(factory);

    @Test
    public void shouldReturnPaymentType() throws IOException, XMLStreamException {
        File initialFile = new File("src/test/resources/sepa-008-sample.xml");
        InputStream targetStream = new FileInputStream(initialFile);
        ingestionService.executeIngestion(targetStream);
    }
}
