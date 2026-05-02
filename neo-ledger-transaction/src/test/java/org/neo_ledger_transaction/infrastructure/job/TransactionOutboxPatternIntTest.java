package org.neo_ledger_transaction.infrastructure.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo_ledger_common.AbstractPostgresContainer;
import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.neo_ledger_transaction.infrastructure.repository.TransactionOutboxJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class TransactionOutboxPatternIntTest extends AbstractPostgresContainer {

    @Autowired
    private TransactionOutboxPattern transactionOutboxPattern;

    @Autowired
    private TransactionOutboxJpaRepository transactionOutboxJpaRepository;

    @MockitoBean
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @MockitoBean
    private Clock outboxClock;

    private final AtomicReference<Instant> currentInstant = new AtomicReference<>(
            Instant.parse("2026-05-02T10:00:00Z")
    );

    @BeforeEach
    void setUp() {
        transactionOutboxJpaRepository.deleteAllInBatch();
        when(outboxClock.getZone()).thenReturn(ZoneOffset.UTC);
        when(outboxClock.instant()).thenAnswer(invocation -> currentInstant.get());
    }

    @Test
    void execute_should_retry_and_reuse_the_same_message_key() {
        OutboxEntry entry = new OutboxEntry();
        entry.setEndToEndId("E2E-456");
        entry.setRoutingKey("SEPA_PAIN_008");
        entry.setEventType("TRANSACTION_INGESTED");
        entry.setPayload(new byte[]{4, 5, 6});

        OutboxEntry saved = transactionOutboxJpaRepository.saveAndFlush(entry);
        String messageKey = saved.getId().toString();

        when(kafkaTemplate.send(eq("SEPA_PAIN_008"), eq(messageKey), any(byte[].class)))
                .thenReturn(
                        CompletableFuture.failedFuture(new RuntimeException("broker down")),
                        CompletableFuture.completedFuture(null)
                );

        transactionOutboxPattern.execute();

        OutboxEntry afterFirstRun = transactionOutboxJpaRepository.findById(saved.getId()).orElseThrow();
        assertEquals("PENDING", afterFirstRun.getStatus());
        assertEquals(1, afterFirstRun.getRetryCount());
        assertNotNull(afterFirstRun.getNextAttemptAt());

        currentInstant.set(afterFirstRun.getNextAttemptAt().plusSeconds(1).atZone(ZoneOffset.UTC).toInstant());

        transactionOutboxPattern.execute();

        OutboxEntry afterSecondRun = transactionOutboxJpaRepository.findById(saved.getId()).orElseThrow();
        assertEquals("PROCESSED", afterSecondRun.getStatus());
        assertEquals(1, afterSecondRun.getRetryCount());
        assertNotNull(afterSecondRun.getProcessedAt());

        verify(kafkaTemplate, times(2)).send(eq("SEPA_PAIN_008"), eq(messageKey), any(byte[].class));
    }

    @Test
    void execute_should_process_due_entry() {
        OutboxEntry entry = new OutboxEntry();
        entry.setEndToEndId("E2E-123");
        entry.setRoutingKey("SEPA_PAIN_001");
        entry.setEventType("TRANSACTION_INGESTED");
        entry.setPayload(new byte[]{1, 2, 3});

        OutboxEntry saved = transactionOutboxJpaRepository.saveAndFlush(entry);

        when(kafkaTemplate.send(eq("SEPA_PAIN_001"), eq(saved.getId().toString()), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        transactionOutboxPattern.execute();

        OutboxEntry reloaded = transactionOutboxJpaRepository.findById(saved.getId()).orElseThrow();
        assertEquals("PROCESSED", reloaded.getStatus());
        assertEquals(0, reloaded.getRetryCount());
        assertNotNull(reloaded.getProcessedAt());
        verify(kafkaTemplate).send(eq("SEPA_PAIN_001"), eq(saved.getId().toString()), any(byte[].class));
    }
}
