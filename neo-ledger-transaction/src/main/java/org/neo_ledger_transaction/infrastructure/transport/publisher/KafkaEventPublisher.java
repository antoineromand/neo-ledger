package org.neo_ledger_transaction.infrastructure.transport.publisher;

import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionEventPublisher;
import org.neo_ledger_transaction.infrastructure.transport.mapper.TransactionMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaEventPublisher implements TransactionEventPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final List<TransactionMapper<?>> mappers;

    public KafkaEventPublisher(KafkaTemplate<String, byte[]> kafkaTemplate, List<TransactionMapper<?>> transactionMappers) {
        this.kafkaTemplate = kafkaTemplate;
        this.mappers = transactionMappers;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void publish(RawTransaction transaction, String topic) {
        TransactionMapper mapper = mappers.stream()
                .filter(m -> m.supports(transaction))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No mapper found for: " + transaction.getClass()));

        byte[] payload = mapper.toBinary(transaction);

        this.kafkaTemplate.send(topic, transaction.endToEndId(), payload);
    }
}
