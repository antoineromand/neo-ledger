package org.neo_ledger.infrastructure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.neo_ledger.domain.Account;
import org.neo_ledger.domain.enums.AccountStatus;
import org.neo_ledger.domain.enums.AccountType;
import org.neo_ledger.domain.value_object.BalanceAmount;
import org.neo_ledger.infrastructure.entity.AccountEntity;

class AccountMapperTest {

  @Test
  void should_map_entity_to_domain() {
    AccountEntity entity = new AccountEntity();
    entity.setId(UUID.randomUUID());
    entity.setIban("FR7630006000011234567890189");
    entity.setBic("AGRIFRPP");
    entity.setType(AccountType.CUSTOMER);
    entity.setCurrentBalance(new BigDecimal("120.50"));
    entity.setReservedBalance(new BigDecimal("20.50"));
    entity.setCurrency("EUR");
    entity.setStatus(AccountStatus.ACTIVE);

    Account account = AccountMapper.toDomain(entity);

    assertEquals(entity.getId(), account.getIdentity());
    assertEquals(entity.getIban(), account.getIban());
    assertEquals(entity.getBic(), account.getBic());
    assertEquals(entity.getType(), account.getAccountType());
    assertEquals(new BalanceAmount(new BigDecimal("120.50")), account.getCurrentBalance());
    assertEquals(new BalanceAmount(new BigDecimal("20.50")), account.getReservedBalance());
    assertEquals(entity.getCurrency(), account.getCurrency());
    assertEquals(entity.getStatus(), account.getAccountStatus());
  }

  @Test
  void should_map_domain_to_entity() {
    Account account =
        new Account(
            UUID.randomUUID(),
            "FR7630006000011234567890189",
            "AGRIFRPP",
            new BalanceAmount(new BigDecimal("120.50")),
            new BalanceAmount(new BigDecimal("20.50")),
            "EUR",
            AccountType.CUSTOMER,
            AccountStatus.ACTIVE);

    AccountEntity entity = AccountMapper.toEntity(account);

    assertNotNull(entity);
    assertEquals(account.getIdentity(), entity.getId());
    assertEquals(account.getIban(), entity.getIban());
    assertEquals(account.getBic(), entity.getBic());
    assertEquals(account.getAccountType(), entity.getType());
    assertEquals(account.getCurrentBalance().amount(), entity.getCurrentBalance());
    assertEquals(account.getReservedBalance().amount(), entity.getReservedBalance());
    assertEquals(account.getCurrency(), entity.getCurrency());
    assertEquals(account.getAccountStatus(), entity.getStatus());
  }
}
