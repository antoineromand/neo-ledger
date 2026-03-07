package org.neo_ledger_transaction.infrastructure.transport.publisher;

import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements TransactionEventPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(RawTransaction transaction) {

    }
}
