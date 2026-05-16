package org.neo_ledger.domain.value_object;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BalanceAmountTest {

  @Test
  void should_credit_and_debit_immutably() {
    BalanceAmount initial = new BalanceAmount(new BigDecimal("100.00"));

    BalanceAmount credited = initial.credit(new BigDecimal("25.00"));
    BalanceAmount debited = credited.debit(new BigDecimal("10.00"));

    assertEquals(new BigDecimal("100.00"), initial.amount());
    assertEquals(new BigDecimal("125.00"), credited.amount());
    assertEquals(new BigDecimal("115.00"), debited.amount());
  }

  @Test
  void should_refuse_negative_or_null_amounts() {
    assertThrows(NullPointerException.class, () -> new BalanceAmount(null));
    assertThrows(IllegalArgumentException.class, () -> new BalanceAmount(new BigDecimal("-1.00")));
    BalanceAmount amount = new BalanceAmount(new BigDecimal("10.00"));
    assertThrows(NullPointerException.class, () -> amount.credit(null));
    assertThrows(IllegalArgumentException.class, () -> amount.credit(BigDecimal.ZERO));
    assertThrows(IllegalArgumentException.class, () -> amount.debit(BigDecimal.ZERO));
  }

  @Test
  void should_refuse_debit_above_available_balance() {
    BalanceAmount amount = new BalanceAmount(new BigDecimal("10.00"));

    assertThrows(IllegalArgumentException.class, () -> amount.debit(new BigDecimal("10.01")));
  }

  @Test
  void should_compare_amounts_correctly() {
    BalanceAmount amount = new BalanceAmount(new BigDecimal("10.00"));

    assertTrue(amount.isPositive());
    assertTrue(amount.isGreaterThan(new BigDecimal("9.99")));
    assertFalse(amount.isGreaterThan(new BigDecimal("10.00")));
  }
}
