package org.neo_ledger.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.neo_ledger.domain.enums.AccountStatus;
import org.neo_ledger.domain.enums.AccountType;

@Entity
@Table(name = "accounts")
public class AccountEntity {
  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 34)
  private String iban;

  @Column(nullable = false, length = 11)
  private String bic;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AccountType type;

  @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal currentBalance;

  @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal reservedBalance;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AccountStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public AccountEntity() {}

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  public String getBic() {
    return bic;
  }

  public void setBic(String bic) {
    this.bic = bic;
  }

  public AccountType getType() {
    return type;
  }

  public void setType(AccountType type) {
    this.type = type;
  }

  public BigDecimal getCurrentBalance() {
    return currentBalance;
  }

  public void setCurrentBalance(BigDecimal currentBalance) {
    this.currentBalance = currentBalance;
  }

  public BigDecimal getReservedBalance() {
    return reservedBalance;
  }

  public void setReservedBalance(BigDecimal reservedBalance) {
    this.reservedBalance = reservedBalance;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public void setStatus(AccountStatus status) {
    this.status = status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
