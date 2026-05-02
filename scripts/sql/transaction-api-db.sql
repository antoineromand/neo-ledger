CREATE TABLE IF NOT EXISTS transaction_outbox
(
    id             UUID PRIMARY KEY,
    end_to_end_id  VARCHAR(255) NOT NULL UNIQUE,  -- end to end id
    routing_key VARCHAR(50) NOT NULL,             -- SEPA_PAIN_008, SEPA_PAIN_001 (broker)
    event_type     VARCHAR(100) NOT NULL,         -- TRANSACTION_INGESTED
    payload        BYTEA        NOT NULL,         -- RawSepaTransaction -> protobuf
    created_at     TIMESTAMP    NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROCESSING, PROCESSED, DEAD_LETTER
    processed_at   TIMESTAMP,                     -- AFTER_PUSH
    retry_count    INT         DEFAULT 0,
    next_attempt_at TIMESTAMP,
    last_error     TEXT
);
