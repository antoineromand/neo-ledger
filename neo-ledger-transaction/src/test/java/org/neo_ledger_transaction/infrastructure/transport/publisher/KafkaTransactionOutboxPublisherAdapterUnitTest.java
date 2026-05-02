package org.neo_ledger_transaction.infrastructure.transport.publisher;

import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxPublishException;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class KafkaTransactionOutboxPublisherAdapterUnitTest {

    private final KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
    private final KafkaTransactionOutboxPublisherAdapter adapter =
            new KafkaTransactionOutboxPublisherAdapter(kafkaTemplate);

    @Test
    void publish_should_send_message_key_and_payload() {
        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publish("SEPA_PAIN_001", "message-key", new byte[]{1, 2, 3});

        verify(kafkaTemplate).send(anyString(), anyString(), any(byte[].class));
    }

    @Test
    void publish_should_wrap_kafka_failure() {
        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        assertThrows(TransactionOutboxPublishException.class,
                () -> adapter.publish("SEPA_PAIN_001", "message-key", new byte[]{1, 2, 3}));
    }
}
