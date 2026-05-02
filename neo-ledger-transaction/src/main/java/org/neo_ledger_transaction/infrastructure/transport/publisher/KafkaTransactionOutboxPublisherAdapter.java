package org.neo_ledger_transaction.infrastructure.transport.publisher;

import org.neo_ledger_transaction.application.exceptions.TransactionOutboxPublishException;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxPublisherPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class KafkaTransactionOutboxPublisherAdapter implements TransactionOutboxPublisherPort {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaTransactionOutboxPublisherAdapter(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String routingKey, String messageKey, byte[] payload) {
        try {
            this.kafkaTemplate.send(routingKey, messageKey, payload).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TransactionOutboxPublishException(exception);
        } catch (ExecutionException exception) {
            throw new TransactionOutboxPublishException(exception);
        }
    }
}
