package org.neo_ledger_transaction.infrastructure.repository;

import org.neo_ledger_transaction.domain.port.out.TransactionOutboxPort;
import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.springframework.stereotype.Service;

@Service
public class TransactionOutboxRepositoryAdapter implements TransactionOutboxPort {
    private final TransactionOutboxJpaRepository transactionOutboxJpaRepository;

    public TransactionOutboxRepositoryAdapter(TransactionOutboxJpaRepository transactionOutboxJpaRepository) {
        this.transactionOutboxJpaRepository = transactionOutboxJpaRepository;
    }

    @Override
    public void save(String endToEndId, String aggregateType, String eventType, byte[] payload) {
        OutboxEntry outboxEntry = new OutboxEntry();
        outboxEntry.setEndToEndId(endToEndId);
        outboxEntry.setAggregateType(aggregateType);
        outboxEntry.setEventType(eventType);
        outboxEntry.setPayload(payload);
        this.transactionOutboxJpaRepository.save(outboxEntry);
    }
}
