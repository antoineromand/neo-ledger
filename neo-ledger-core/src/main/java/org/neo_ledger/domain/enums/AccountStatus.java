package org.neo_ledger.domain.enums;

public enum AccountStatus {
  ACTIVE,
  BLOCKED,
  SUSPENDED,
  CLOSED;

  public boolean allowsMoneyOperations() {
    return this == ACTIVE;
  }

  public boolean canTransitionTo(AccountStatus targetStatus) {
    return switch (this) {
      case ACTIVE -> targetStatus == BLOCKED || targetStatus == SUSPENDED || targetStatus == CLOSED;
      case BLOCKED -> targetStatus == ACTIVE || targetStatus == CLOSED;
      case SUSPENDED -> targetStatus == ACTIVE || targetStatus == CLOSED;
      case CLOSED -> false;
    };
  }
}
