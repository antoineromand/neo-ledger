package org.neo_ledger_transaction.infrastructure.models;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_outbox")
public class OutboxEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "end_to_end_id", unique = true)
    private String endToEndId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(Types.VARBINARY)
    @Column(name = "payload", nullable = false, columnDefinition = "BYTEA")
    private byte[] payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public OutboxEntry() {
    }

    public OutboxEntry(LocalDateTime processedAt, LocalDateTime createdAt, String lastError, int retryCount, String status, byte[] payload, String eventType, String endToEndId, UUID id) {
        this.processedAt = processedAt;
        this.createdAt = createdAt;
        this.lastError = lastError;
        this.retryCount = retryCount;
        this.status = status;
        this.payload = payload;
        this.eventType = eventType;
        this.endToEndId = endToEndId;
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public void setEndToEndId(String endToEndId) {
        this.endToEndId = endToEndId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
