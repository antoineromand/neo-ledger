package org.neo_ledger_transaction.infrastructure.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.neo_ledger_transaction.infrastructure.models.OutboxEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionOutboxJpaRepository extends JpaRepository<OutboxEntry, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select o from OutboxEntry o
            where o.status = :status
              and (o.nextAttemptAt is null or o.nextAttemptAt <= :now)
            order by o.createdAt asc
            """)
  List<OutboxEntry> findBatchByStatusForUpdate(
      @Param("status") String status, @Param("now") LocalDateTime now, Pageable pageable);
}
