package org.neo_ledger.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.neo_ledger.domain.enums.AccountStatus;
import org.neo_ledger.domain.enums.AccountType;
import org.neo_ledger.domain.value_object.BalanceAmount;

class AccountTest {

  @Test
  void should_credit_and_debit_current_balance_when_account_is_active() {
    Account account = newAccount(AccountStatus.ACTIVE);

    account.creditCurrentBalance(new BigDecimal("25.00"));
    account.debitCurrentBalance(new BigDecimal("10.00"));

    assertEquals(new BigDecimal("115.00"), account.getCurrentBalance().amount());
  }

  @Test
  void should_reserve_and_release_funds_when_account_is_active() {
    Account account = newAccount(AccountStatus.ACTIVE);

    account.reserveFunds(new BigDecimal("30.00"));
    account.releaseFunds(new BigDecimal("10.00"));

    assertEquals(new BigDecimal("20.00"), account.getReservedBalance().amount());
  }

  @Test
  void should_refuse_operations_when_account_is_not_active() {
    Account account = newAccount(AccountStatus.BLOCKED);

    assertThrows(
        IllegalStateException.class, () -> account.creditCurrentBalance(new BigDecimal("1.00")));
    assertThrows(
        IllegalStateException.class, () -> account.debitCurrentBalance(new BigDecimal("1.00")));
    assertThrows(IllegalStateException.class, () -> account.reserveFunds(new BigDecimal("1.00")));
    assertThrows(IllegalStateException.class, () -> account.releaseFunds(new BigDecimal("1.00")));
  }

  @Test
  void should_refuse_reservation_above_available_balance() {
    Account account = newAccount(AccountStatus.ACTIVE);

    assertThrows(IllegalArgumentException.class, () -> account.reserveFunds(new BigDecimal("101.00")));
  }

  @Test
  void should_close_and_prevent_reactivation_after_close() {
    Account account = newAccount(AccountStatus.ACTIVE);

    account.closeAccount();

    assertEquals(AccountStatus.CLOSED, account.getAccountStatus());
    assertThrows(IllegalStateException.class, account::reactivateAccount);
  }

  @Test
  void should_allow_block_suspend_and_reactivate_when_transition_is_valid() {
    Account account = newAccount(AccountStatus.ACTIVE);

    account.blockAccount();
    assertEquals(AccountStatus.BLOCKED, account.getAccountStatus());

    account.reactivateAccount();
    assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());

    account.suspendAccount();
    assertEquals(AccountStatus.SUSPENDED, account.getAccountStatus());

    account.reactivateAccount();
    assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
  }

  @Test
  void should_refuse_money_operations_when_account_is_closed() {
    Account account = newAccount(AccountStatus.CLOSED);

    assertThrows(
        IllegalStateException.class, () -> account.creditCurrentBalance(new BigDecimal("1.00")));
    assertThrows(
        IllegalStateException.class, () -> account.debitCurrentBalance(new BigDecimal("1.00")));
    assertThrows(IllegalStateException.class, () -> account.reserveFunds(new BigDecimal("1.00")));
    assertThrows(IllegalStateException.class, () -> account.releaseFunds(new BigDecimal("1.00")));
  }

  @Test
  void should_refuse_same_status_transition() {
    Account account = newAccount(AccountStatus.BLOCKED);

    assertThrows(IllegalStateException.class, account::blockAccount);
  }

  private static Account newAccount(AccountStatus status) {
    return new Account(
        UUID.randomUUID(),
        "FR7630006000011234567890189",
        "AGRIFRPP",
        new BalanceAmount(new BigDecimal("100.00")),
        new BalanceAmount(new BigDecimal("0.00")),
        "EUR",
        AccountType.CUSTOMER,
        status);
  }
}
