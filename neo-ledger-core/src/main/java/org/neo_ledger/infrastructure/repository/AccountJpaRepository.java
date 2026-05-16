package org.neo_ledger.infrastructure.repository;

import java.util.UUID;
import org.neo_ledger.infrastructure.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {}
