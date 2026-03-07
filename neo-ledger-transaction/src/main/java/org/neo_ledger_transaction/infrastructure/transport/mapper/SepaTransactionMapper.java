package org.neo_ledger_transaction.infrastructure.transport.mapper;

import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.springframework.stereotype.Component;

@Component
public class SepaTransactionMapper implements TransactionMapper<RawSepaTransaction> {

    @Override
    public boolean supports(RawSepaTransaction transaction) {
        return transaction instanceof RawSepaTransaction;
    }

    @Override
    public byte[] toBinary(RawSepaTransaction transaction) {
        return new byte[0];
    }

    @Override
    public String getTopicName() {
        return "sepa-transaction-topic";
    }
}
