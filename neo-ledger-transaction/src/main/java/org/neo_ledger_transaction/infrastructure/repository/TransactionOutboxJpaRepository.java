package org.neo_ledger_transaction.infrastructure.repository;

import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionOutboxJpaRepository extends JpaRepository<OutboxEntry, UUID> {
}
