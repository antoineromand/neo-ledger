package org.neo_ledger_transaction.infrastructure.publisher;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.neo_ledger_common.AbstractKafkaContainer;
import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.neo_ledger_transaction.infrastructure.transport.publisher.KafkaEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class KafkaTransactionEventPublisherIntTest extends AbstractKafkaContainer {

    @Autowired
    private KafkaEventPublisher publisher;

    @Test
    void should_send_binary_protobuf_to_kafka() throws Exception {
        try (Consumer<String, byte[]> testConsumer = createTestConsumer()) {

            testConsumer.subscribe(Collections.singletonList("sepa-transaction-topic"));
            KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(500));

            RawSepaTransaction tx = new RawSepaTransaction(
                    "E2E-12345", "IBAN-DE-123", "IBAN-FR-456",
                    new BigDecimal("100.50"), "EUR", LocalDate.now(),
                    true, "Test Payment", "MAND-1", "SCHEME-1"
            );

            publisher.publish(tx);

            ConsumerRecord<String, byte[]> received = KafkaTestUtils.getSingleRecord(
                    testConsumer,
                    "sepa-transaction-topic",
                    Duration.ofSeconds(10));

            org.neo_ledger.common.event.RawSepaTransaction proto =
                    org.neo_ledger.common.event.RawSepaTransaction.parseFrom(received.value());

            assertThat(proto.getEndToEndId()).isEqualTo(tx.endToEndId());
            assertThat(proto.getAmount()).isEqualTo("100.50");
            assertThat(proto.getDebtorIban()).isEqualTo("IBAN-DE-123");
        }
    }

    private Consumer<String, byte[]> createTestConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(KAFKA.getBootstrapServers(), "test-group", "true");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        DefaultKafkaConsumerFactory<String, byte[]> cf = new DefaultKafkaConsumerFactory<>(props);
        Consumer<String, byte[]> consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList("sepa-transaction-topic"));
        return consumer;
    }
}
