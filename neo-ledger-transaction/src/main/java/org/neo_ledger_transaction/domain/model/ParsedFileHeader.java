package org.neo_ledger_transaction.domain.model;

import java.time.LocalDateTime;

public record ParsedFileHeader(String msgId, int expectedNbTxs, LocalDateTime creationDateTime) {}
