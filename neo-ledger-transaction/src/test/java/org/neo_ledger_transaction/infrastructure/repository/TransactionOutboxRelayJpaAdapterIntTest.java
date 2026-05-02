package org.neo_ledger_transaction.infrastructure.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo_ledger_common.AbstractPostgresContainer;
import org.neo_ledger_transaction.application.model.TransactionOutboxMessage;
import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class TransactionOutboxRelayJpaAdapterIntTest extends AbstractPostgresContainer {

    @Autowired
    private TransactionOutboxRelayJpaAdapter relayJpaAdapter;

    @Autowired
    private TransactionOutboxJpaRepository transactionOutboxJpaRepository;

    @BeforeEach
    void setUp() {
        transactionOutboxJpaRepository.deleteAllInBatch();
    }

    @Test
    void claim_due_messages_should_mark_only_due_entries_processing() {
        OutboxEntry due = new OutboxEntry();
        due.setEndToEndId("E2E-1");
        due.setRoutingKey("SEPA_PAIN_001");
        due.setEventType("TRANSACTION_INGESTED");
        due.setPayload(new byte[]{1});
        due.setRetryCount(2);
        due.setNextAttemptAt(LocalDateTime.now().minusMinutes(1));

        OutboxEntry future = new OutboxEntry();
        future.setEndToEndId("E2E-2");
        future.setRoutingKey("SEPA_PAIN_008");
        future.setEventType("TRANSACTION_INGESTED");
        future.setPayload(new byte[]{2});
        future.setRetryCount(1);
        future.setNextAttemptAt(LocalDateTime.now().plusMinutes(10));

        transactionOutboxJpaRepository.saveAll(List.of(due, future));
        transactionOutboxJpaRepository.flush();

        List<TransactionOutboxMessage> claimed = relayJpaAdapter.claimDueMessages(LocalDateTime.now(), 20);

        assertEquals(1, claimed.size());
        assertEquals("SEPA_PAIN_001", claimed.getFirst().routingKey());
        assertEquals(2, claimed.getFirst().retryCount());

        OutboxEntry reloadedDue = transactionOutboxJpaRepository.findById(claimed.getFirst().id()).orElseThrow();
        OutboxEntry reloadedFuture = transactionOutboxJpaRepository.findById(future.getId()).orElseThrow();

        assertEquals("PROCESSING", reloadedDue.getStatus());
        assertEquals("PENDING", reloadedFuture.getStatus());
    }

    @Test
    void schedule_retry_should_update_retry_metadata() {
        OutboxEntry entry = new OutboxEntry();
        entry.setEndToEndId("E2E-3");
        entry.setRoutingKey("SEPA_PAIN_001");
        entry.setEventType("TRANSACTION_INGESTED");
        entry.setPayload(new byte[]{3});

        OutboxEntry saved = transactionOutboxJpaRepository.saveAndFlush(entry);

        LocalDateTime nextAttempt = LocalDateTime.of(2026, 5, 2, 10, 3);
        relayJpaAdapter.scheduleRetry(saved.getId(), 2, nextAttempt, "boom");

        OutboxEntry reloaded = transactionOutboxJpaRepository.findById(saved.getId()).orElseThrow();
        assertEquals("PENDING", reloaded.getStatus());
        assertEquals(2, reloaded.getRetryCount());
        assertEquals(nextAttempt.truncatedTo(ChronoUnit.MICROS), reloaded.getNextAttemptAt().truncatedTo(ChronoUnit.MICROS));
        assertEquals("boom", reloaded.getLastError());
    }

    @Test
    void mark_as_processed_should_clear_retry_fields() {
        OutboxEntry entry = new OutboxEntry();
        entry.setEndToEndId("E2E-4");
        entry.setRoutingKey("SEPA_PAIN_008");
        entry.setEventType("TRANSACTION_INGESTED");
        entry.setPayload(new byte[]{4});

        OutboxEntry saved = transactionOutboxJpaRepository.saveAndFlush(entry);

        LocalDateTime processedAt = LocalDateTime.of(2026, 5, 2, 10, 0);
        relayJpaAdapter.markAsProcessed(saved.getId(), processedAt);

        OutboxEntry reloaded = transactionOutboxJpaRepository.findById(saved.getId()).orElseThrow();
        assertEquals("PROCESSED", reloaded.getStatus());
        assertEquals(processedAt.truncatedTo(ChronoUnit.MICROS), reloaded.getProcessedAt().truncatedTo(ChronoUnit.MICROS));
        assertNull(reloaded.getNextAttemptAt());
        assertNull(reloaded.getLastError());
    }

    @Test
    void mark_as_dead_letter_should_set_terminal_state() {
        OutboxEntry entry = new OutboxEntry();
        entry.setEndToEndId("E2E-5");
        entry.setRoutingKey("SEPA_PAIN_008");
        entry.setEventType("TRANSACTION_INGESTED");
        entry.setPayload(new byte[]{5});

        OutboxEntry saved = transactionOutboxJpaRepository.saveAndFlush(entry);

        relayJpaAdapter.markAsDeadLetter(saved.getId(), 5, "boom");

        OutboxEntry reloaded = transactionOutboxJpaRepository.findById(saved.getId()).orElseThrow();
        assertEquals("DEAD_LETTER", reloaded.getStatus());
        assertEquals(5, reloaded.getRetryCount());
        assertEquals("boom", reloaded.getLastError());
        assertNull(reloaded.getNextAttemptAt());
    }
}
