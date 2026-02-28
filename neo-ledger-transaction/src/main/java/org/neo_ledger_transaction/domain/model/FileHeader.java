package org.neo_ledger_transaction.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FileHeader(
        String msgId,
        int expectedCount,
        BigDecimal expectedCtrlSum,
        LocalDateTime creationDateTime
) {
}
