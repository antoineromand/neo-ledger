package org.neo_ledger_common;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractKafkaContainer {
    protected static final KafkaContainer KAFKA;

    static {
        KAFKA = new KafkaContainer(DockerImageName.parse(("apache/kafka:3.7.0")));
        KAFKA.start();
        Runtime.getRuntime().addShutdownHook(new Thread(KAFKA::stop));
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
