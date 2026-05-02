package org.neo_ledger_transaction.infrastructure.models;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistence model for the transaction outbox.
 * <p>
 * Each field carries a specific relay contract:
 * identity, business correlation, routing, payload, and lifecycle state.
 * </p>
 */
@Entity
@Table(name = "transaction_outbox")
public class OutboxEntry {

    /**
     * Unique technical identifier of the outbox row and stable message key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Business correlation key inherited from the payment file. */
    @Column(name = "end_to_end_id")
    private String endToEndId;

    /** Domain event name produced by ingestion, for example `TRANSACTION_INGESTED`. */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Broker routing key or topic key used to dispatch the message. */
    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    /** Serialized event payload to publish to the broker. */
    @JdbcTypeCode(Types.VARBINARY)
    @Column(name = "payload", nullable = false, columnDefinition = "BYTEA")
    private byte[] payload;

    /** Lifecycle state of the outbox row: PENDING, PROCESSING, PROCESSED, or DEAD_LETTER. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /** Number of publish attempts already performed for this row. */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /** Earliest time at which the row may be claimed again after a failure. */
    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    /** Last publish or relay error recorded for troubleshooting and retries. */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /** Timestamp when the outbox row was created. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the row was successfully published. */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public OutboxEntry() {
    }

    public OutboxEntry(LocalDateTime processedAt, LocalDateTime createdAt, String lastError, int retryCount, LocalDateTime nextAttemptAt, String status, byte[] payload, String eventType, String endToEndId, UUID id, String routingKey) {
        this.processedAt = processedAt;
        this.createdAt = createdAt;
        this.lastError = lastError;
        this.retryCount = retryCount;
        this.nextAttemptAt = nextAttemptAt;
        this.status = status;
        this.payload = payload;
        this.eventType = eventType;
        this.endToEndId = endToEndId;
        this.id = id;
        this.routingKey = routingKey;
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

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
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

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}
