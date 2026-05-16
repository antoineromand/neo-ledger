package org.neo_ledger.domain.value_object;

import java.math.BigDecimal;
import java.util.Objects;

public record BalanceAmount(BigDecimal amount) {
  public BalanceAmount {
    Objects.requireNonNull(amount, "amount must not be null");
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("amount must be >= 0");
    }
  }

  public BalanceAmount credit(BigDecimal amount) {
    Objects.requireNonNull(amount, "amount must not be null");
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("credit amount must be > 0");
    }
    return new BalanceAmount(this.amount.add(amount));
  }

  public BalanceAmount debit(BigDecimal amount) {
    Objects.requireNonNull(amount, "amount must not be null");
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("debit amount must be > 0");
    }
    if (this.amount.compareTo(amount) < 0) {
      throw new IllegalArgumentException("insufficient balance");
    }
    return new BalanceAmount(this.amount.subtract(amount));
  }

  public boolean isPositive() {
    return amount.compareTo(BigDecimal.ZERO) > 0;
  }

  public boolean isGreaterThan(BigDecimal otherAmount) {
    Objects.requireNonNull(otherAmount, "otherAmount must not be null");
    return amount.compareTo(otherAmount) > 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BalanceAmount(BigDecimal amount1))) return false;
    return amount.compareTo(amount1) == 0;
  }

  @Override
  public int hashCode() {
    return amount.stripTrailingZeros().hashCode();
  }

  @Override
  public String toString() {
    return amount.toPlainString();
  }
}
