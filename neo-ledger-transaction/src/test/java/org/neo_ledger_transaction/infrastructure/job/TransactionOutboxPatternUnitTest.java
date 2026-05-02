package org.neo_ledger_transaction.infrastructure.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxClaimException;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxEntryNotFoundException;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxPublishException;
import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.neo_ledger_transaction.infrastructure.repository.TransactionOutboxJpaRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TransactionOutboxPatternUnitTest {

    private final TransactionOutboxJpaRepository transactionOutboxJpaRepository = mock(TransactionOutboxJpaRepository.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);
    private final Clock clock = mock(Clock.class);

    private final TransactionOutboxPattern transactionOutboxPattern =
            new TransactionOutboxPattern(transactionOutboxJpaRepository, kafkaTemplate, transactionManager, clock);

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2026-05-02T10:00:00Z"));
    }

    @Test
    void claim_pending_transactions_should_mark_entries_processing() {
        OutboxEntry entry = new OutboxEntry();
        entry.setId(UUID.randomUUID());
        entry.setStatus("PENDING");

        when(transactionOutboxJpaRepository.findBatchByStatusForUpdate(eq("PENDING"), any(), any()))
                .thenReturn(List.of(entry));
        when(transactionOutboxJpaRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<OutboxEntry> claimed = transactionOutboxPattern.claimPendingTransactions();

        assertEquals("PROCESSING", claimed.getFirst().getStatus());
        verify(transactionOutboxJpaRepository).saveAll(any());
        verify(transactionManager).commit(transactionStatus);
    }

    @Test
    void claim_pending_transactions_should_wrap_repository_failure() {
        when(transactionOutboxJpaRepository.findBatchByStatusForUpdate(eq("PENDING"), any(), any()))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(TransactionOutboxClaimException.class, () -> transactionOutboxPattern.claimPendingTransactions());
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    void publish_should_use_outbox_id_as_message_key() {
        OutboxEntry entry = new OutboxEntry();
        UUID id = UUID.randomUUID();
        entry.setId(id);
        entry.setRoutingKey("SEPA_PAIN_001");
        entry.setPayload(new byte[]{1, 2, 3});

        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        transactionOutboxPattern.publish(entry);

        verify(kafkaTemplate).send(eq("SEPA_PAIN_001"), eq(id.toString()), any(byte[].class));
    }

    @Test
    void publish_should_wrap_kafka_failure() {
        OutboxEntry entry = new OutboxEntry();
        entry.setId(UUID.randomUUID());
        entry.setRoutingKey("SEPA_PAIN_001");
        entry.setPayload(new byte[]{1, 2, 3});

        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        assertThrows(TransactionOutboxPublishException.class, () -> transactionOutboxPattern.publish(entry));
    }

    @Test
    void mark_transaction_as_failed_should_schedule_retry_and_keep_pending() {
        OutboxEntry entry = new OutboxEntry();
        UUID id = UUID.randomUUID();
        entry.setId(id);
        entry.setRetryCount(0);

        when(transactionOutboxJpaRepository.findById(id)).thenReturn(Optional.of(entry));

        transactionOutboxPattern.markTransactionAsFailed(entry, new RuntimeException("boom"));

        verify(transactionOutboxJpaRepository).save(any());
        assertEquals("PENDING", entry.getStatus());
        assertEquals(1, entry.getRetryCount());
        assertNotNull(entry.getNextAttemptAt());
    }

    @Test
    void mark_transaction_as_failed_should_dead_letter_after_max_retries() {
        OutboxEntry entry = new OutboxEntry();
        UUID id = UUID.randomUUID();
        entry.setId(id);
        entry.setRetryCount(4);

        when(transactionOutboxJpaRepository.findById(id)).thenReturn(Optional.of(entry));

        transactionOutboxPattern.markTransactionAsFailed(entry, new RuntimeException("boom"));

        assertEquals("DEAD_LETTER", entry.getStatus());
        assertEquals(5, entry.getRetryCount());
    }

    @Test
    void mark_transaction_as_processed_should_fail_when_entry_missing() {
        OutboxEntry entry = new OutboxEntry();
        UUID id = UUID.randomUUID();
        entry.setId(id);

        when(transactionOutboxJpaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionOutboxEntryNotFoundException.class,
                () -> transactionOutboxPattern.markTransactionAsProcessed(entry));
        verify(transactionManager).rollback(transactionStatus);
    }
}
