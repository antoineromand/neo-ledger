CREATE TABLE IF NOT EXISTS transaction_outbox
(
    id             UUID PRIMARY KEY,
    end_to_end_id VARCHAR(255) NOT NULL,          -- end to end id
    aggregate_type VARCHAR(50)  NOT NULL,         -- RAW_SEPA_TRANSACTION
    event_type     VARCHAR(100) NOT NULL,         -- TRANSACTION_INGESTED
    payload        BYTEA        NOT NULL,         -- RawSepaTransaction -> protobuf
    created_at     TIMESTAMP    NOT NULL,
    status         VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROCESSED, FAILED
    processed_at   TIMESTAMP,                     -- AFTER_PUSH
    retry_count    INT         DEFAULT 0,
    last_error     TEXT
);