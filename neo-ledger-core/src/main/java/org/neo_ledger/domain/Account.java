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
    ensureMoneyOperationsAllowed();
    this.currentBalance = this.currentBalance.credit(amount);
  }

  public void debitCurrentBalance(BigDecimal amount) {
    ensureMoneyOperationsAllowed();
    this.currentBalance = this.currentBalance.debit(amount);
  }

  public void reserveFunds(BigDecimal amount) {
    ensureMoneyOperationsAllowed();
    requirePositiveAmount(amount);
    BigDecimal available = this.currentBalance.amount().subtract(this.reservedBalance.amount());
    if (amount.compareTo(available) > 0) {
      throw new IllegalArgumentException("amount exceeds available balance");
    }
    this.reservedBalance = this.reservedBalance.credit(amount);
  }

  public void releaseFunds(BigDecimal amount) {
    ensureMoneyOperationsAllowed();
    this.reservedBalance = this.reservedBalance.debit(amount);
  }

  public void reactivateAccount() {
    ensureTransitionAllowed(AccountStatus.ACTIVE);
    this.accountStatus = AccountStatus.ACTIVE;
  }

  public void closeAccount() {
    ensureTransitionAllowed(AccountStatus.CLOSED);
    this.accountStatus = AccountStatus.CLOSED;
  }

  public void suspendAccount() {
    ensureTransitionAllowed(AccountStatus.SUSPENDED);
    this.accountStatus = AccountStatus.SUSPENDED;
  }

  public void blockAccount() {
    ensureTransitionAllowed(AccountStatus.BLOCKED);
    this.accountStatus = AccountStatus.BLOCKED;
  }

  public void activate() {
    reactivateAccount();
  }

  public void close() {
    closeAccount();
  }

  public void suspend() {
    suspendAccount();
  }

  public void block() {
    blockAccount();
  }

  private void ensureMoneyOperationsAllowed() {
    if (!accountStatus.allowsMoneyOperations()) {
      throw new IllegalStateException("Account status must be ACTIVE");
    }
  }

  private void ensureTransitionAllowed(AccountStatus targetStatus) {
    if (accountStatus == targetStatus) {
      throw new IllegalStateException("Account is already " + targetStatus);
    }
    if (!accountStatus.canTransitionTo(targetStatus)) {
      throw new IllegalStateException(
          "Transition from " + accountStatus + " to " + targetStatus + " is not allowed");
    }
  }

  private static void requirePositiveAmount(BigDecimal amount) {
    if (amount == null) {
      throw new NullPointerException("amount must not be null");
    }
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be > 0");
    }
  }
}
