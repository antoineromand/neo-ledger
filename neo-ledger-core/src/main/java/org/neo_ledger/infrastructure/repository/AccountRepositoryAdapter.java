package org.neo_ledger.infrastructure.repository;

import org.neo_ledger.application.ports.out.AccountRepositoryPort;
import org.neo_ledger.domain.Account;
import org.neo_ledger.infrastructure.entity.AccountEntity;
import org.neo_ledger.infrastructure.mapper.AccountMapper;
import org.springframework.stereotype.Component;

@Component
public class AccountRepositoryAdapter implements AccountRepositoryPort {
    private final AccountJpaRepository accountJpaRepository;

    public AccountRepositoryAdapter(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public Account createAccount(Account account) {
        AccountEntity accountEntity = AccountMapper.toEntity(account);
        return AccountMapper.toDomain(accountJpaRepository.save(accountEntity));
    }
}
