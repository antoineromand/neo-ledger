package org.neo_ledger.infrastructure.mapper;

import org.neo_ledger.domain.Account;
import org.neo_ledger.domain.value_object.BalanceAmount;
import org.neo_ledger.infrastructure.entity.AccountEntity;

public final class AccountMapper {

  private AccountMapper() {}

  public static Account toDomain(AccountEntity entity) {
    return new Account(
        entity.getId(),
        entity.getIban(),
        entity.getBic(),
        new BalanceAmount(entity.getCurrentBalance()),
        new BalanceAmount(entity.getReservedBalance()),
        entity.getCurrency(),
        entity.getType(),
        entity.getStatus());
  }

  public static AccountEntity toEntity(Account domain) {
    AccountEntity entity = new AccountEntity();
    entity.setId(domain.getIdentity());
    entity.setIban(domain.getIban());
    entity.setBic(domain.getBic());
    entity.setType(domain.getAccountType());
    entity.setCurrentBalance(domain.getCurrentBalance().amount());
    entity.setReservedBalance(domain.getReservedBalance().amount());
    entity.setCurrency(domain.getCurrency());
    entity.setStatus(domain.getAccountStatus());
    return entity;
  }
}
