package org.neo_ledger_transaction.infrastructure.transport.mapper;

import org.neo_ledger_transaction.domain.model.RawTransaction;

public interface TransactionMapper<T extends RawTransaction> {
    boolean supports(RawTransaction transaction);

    byte[] toBinary(T transaction);

}
