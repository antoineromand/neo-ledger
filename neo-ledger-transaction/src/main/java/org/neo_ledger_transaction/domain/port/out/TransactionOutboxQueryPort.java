package org.neo_ledger_transaction.domain.port.out;

import org.neo_ledger_transaction.application.model.TransactionOutboxMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionOutboxQueryPort {
    List<TransactionOutboxMessage> claimDueMessages(LocalDateTime now, int batchSize);
}
