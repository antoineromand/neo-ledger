package org.neo_ledger_transaction.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.neo_ledger_transaction.application.port.in.IngestionUseCasePort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            summary = "Ingest payment file",
            description = "Ingest a payment to ingest and process transactions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Your payment file has been ingested")
    })
    public ResponseEntity<String> ingestPaymentFile(@RequestBody String paymentFile) throws XMLStreamException, IOException {
        InputStream inputStream = new ByteArrayInputStream(paymentFile.getBytes(StandardCharsets.UTF_8));
        this.ingestionUseCasePort.executeIngestion(inputStream);
        return ResponseEntity.ok().body("Your payment file has been ingested");
    }
}
