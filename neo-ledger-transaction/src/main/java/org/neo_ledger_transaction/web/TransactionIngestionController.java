package org.neo_ledger_transaction.web;

import org.neo_ledger_transaction.application.port.in.IngestionUseCasePort;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController()
@RequestMapping("private/api/v1/payment")
public class TransactionIngestionController {
    private final IngestionUseCasePort ingestionUseCasePort;

    public TransactionIngestionController(
            IngestionUseCasePort ingestionUseCasePort
    ) {
        this.ingestionUseCasePort = ingestionUseCasePort;
    }

    @PostMapping()
    public void ingestPaymentFile(@RequestBody String paymentFile) throws XMLStreamException, IOException {
        InputStream inputStream = new ByteArrayInputStream(paymentFile.getBytes(StandardCharsets.UTF_8));

        this.ingestionUseCasePort.executeIngestion(inputStream);
    }
}
