package org.neo_ledger.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.neo_ledger.domain.enums.AccountStatus;
import org.neo_ledger.domain.enums.AccountType;
import org.neo_ledger.domain.value_object.BalanceAmount;

public class Account {
  private final UUID identity;
  private final String iban;
  private final String bic;
  private BalanceAmount currentBalance;
  private BalanceAmount reservedBalance;
  private final String currency;
  private final AccountType accountType;
  private AccountStatus accountStatus;

  public Account(
      UUID identity,
      String iban,
      String bic,
      BalanceAmount currentBalance,
      BalanceAmount reservedBalance,
      String currency,
      AccountType accountType,
      AccountStatus accountStatus) {
    this.identity = Objects.requireNonNull(identity);
    this.iban = Objects.requireNonNull(iban);
    this.bic = Objects.requireNonNull(bic);
    this.currentBalance = Objects.requireNonNull(currentBalance);
    this.reservedBalance = Objects.requireNonNull(reservedBalance);
    this.currency = Objects.requireNonNull(currency);
    this.accountType = Objects.requireNonNull(accountType);
    this.accountStatus = Objects.requireNonNull(accountStatus);
  }

  public UUID getIdentity() {
    return identity;
  }

  public String getIban() {
    return iban;
  }

  public String getBic() {
    return bic;
  }

  public BalanceAmount getCurrentBalance() {
    return currentBalance;
  }

  public BalanceAmount getReservedBalance() {
    return reservedBalance;
  }

  public String getCurrency() {
    return currency;
  }

  public AccountType getAccountType() {
    return accountType;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public void creditCurrentBalance(BigDecimal amount) {
    ensureIsActive();
    this.currentBalance = this.currentBalance.credit(amount);
  }

  public void debitCurrentBalance(BigDecimal amount) {
    ensureIsActive();
    this.currentBalance = this.currentBalance.debit(amount);
  }

  public void releaseReservedBalance(BigDecimal amount) {
    ensureIsActive();
    this.reservedBalance = this.reservedBalance.debit(amount);
  }

  public void reserveReservedBalance(BigDecimal amount) {
    ensureIsActive();
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be > 0");
    }
    BigDecimal available = this.currentBalance.amount().subtract(this.reservedBalance.amount());
    if (amount.compareTo(available) > 0) {
      throw new IllegalArgumentException("amount exceeds available balance");
    }
    this.reservedBalance = this.reservedBalance.credit(amount);
  }

  public void activate() {
    if (accountStatus == AccountStatus.CLOSED) {
      throw new IllegalStateException("Account has been closed, it cannot be activated");
    }
    this.accountStatus = AccountStatus.ACTIVE;
  }

  public void close() {
    if (accountStatus == AccountStatus.CLOSED) {
      throw new IllegalStateException("Account has been closed, it cannot be deactivated");
    }
    this.accountStatus = AccountStatus.CLOSED;
  }

  public void suspend() {
    if (accountStatus == AccountStatus.SUSPENDED) {
      throw new IllegalStateException("Account has been suspended, it cannot be suspended");
    }
    this.accountStatus = AccountStatus.SUSPENDED;
  }

  public void block() {
    if (accountStatus == AccountStatus.BLOCKED) {
      throw new IllegalStateException("Account has been blocked, it cannot be blocked");
    }
    this.accountStatus = AccountStatus.BLOCKED;
  }

  private void ensureIsActive() {
    if (accountStatus != AccountStatus.ACTIVE) {
      throw new IllegalStateException("Account status must be ACTIVE");
    }
  }
}
