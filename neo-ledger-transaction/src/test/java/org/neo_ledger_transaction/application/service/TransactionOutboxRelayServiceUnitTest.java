package org.neo_ledger_transaction.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxClaimException;
import org.neo_ledger_transaction.application.exceptions.TransactionOutboxStateException;
import org.neo_ledger_transaction.application.model.TransactionOutboxMessage;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxPublisherPort;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxQueryPort;
import org.neo_ledger_transaction.domain.port.out.TransactionOutboxStatePort;

class TransactionOutboxRelayServiceUnitTest {
  private final TransactionOutboxQueryPort queryPort = mock(TransactionOutboxQueryPort.class);
  private final TransactionOutboxStatePort statePort = mock(TransactionOutboxStatePort.class);
  private final TransactionOutboxPublisherPort publisherPort =
      mock(TransactionOutboxPublisherPort.class);
  private final Clock clock = mock(Clock.class);

  private final TransactionOutboxRelayService service =
      new TransactionOutboxRelayService(queryPort, statePort, publisherPort, clock);

  @BeforeEach
  void setUp() {
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(clock.instant()).thenReturn(Instant.parse("2026-05-02T10:00:00Z"));
  }

  @Test
  void execute_should_publish_and_mark_as_processed() {
    TransactionOutboxMessage message =
        new TransactionOutboxMessage(UUID.randomUUID(), "SEPA_PAIN_001", new byte[] {1, 2, 3}, 0);

    when(queryPort.claimDueMessages(any(), eq(20))).thenReturn(List.of(message));

    service.execute();

    verify(publisherPort).publish(eq("SEPA_PAIN_001"), eq(message.id().toString()), any());
    verify(statePort).markAsProcessed(eq(message.id()), any());
  }

  @Test
  void execute_should_schedule_retry_on_publish_failure() {
    UUID id = UUID.randomUUID();
    TransactionOutboxMessage message =
        new TransactionOutboxMessage(id, "SEPA_PAIN_008", new byte[] {1, 2, 3}, 0);

    when(queryPort.claimDueMessages(any(), eq(20))).thenReturn(List.of(message));
    doThrow(new RuntimeException("broker down")).when(publisherPort).publish(any(), any(), any());

    service.execute();

    verify(statePort).scheduleRetry(eq(id), eq(1), any(), eq("broker down"));
  }

  @Test
  void execute_should_dead_letter_after_fifth_retry() {
    UUID id = UUID.randomUUID();
    TransactionOutboxMessage message =
        new TransactionOutboxMessage(id, "SEPA_PAIN_008", new byte[] {1, 2, 3}, 4);

    when(queryPort.claimDueMessages(any(), eq(20))).thenReturn(List.of(message));
    doThrow(new RuntimeException("broker down")).when(publisherPort).publish(any(), any(), any());

    service.execute();

    verify(statePort).markAsDeadLetter(eq(id), eq(5), eq("broker down"));
  }

  @Test
  void execute_should_wrap_claim_failure() {
    when(queryPort.claimDueMessages(any(), eq(20))).thenThrow(new RuntimeException("db down"));

    assertThrows(TransactionOutboxClaimException.class, () -> service.execute());
  }

  @Test
  void execute_should_wrap_state_failure() {
    UUID id = UUID.randomUUID();
    TransactionOutboxMessage message =
        new TransactionOutboxMessage(id, "SEPA_PAIN_001", new byte[] {1, 2, 3}, 0);

    when(queryPort.claimDueMessages(any(), eq(20))).thenReturn(List.of(message));
    doThrow(new RuntimeException("broken state")).when(statePort).markAsProcessed(any(), any());

    assertThrows(TransactionOutboxStateException.class, () -> service.execute());
  }
}
