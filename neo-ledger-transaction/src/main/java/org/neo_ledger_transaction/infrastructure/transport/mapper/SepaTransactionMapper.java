package org.neo_ledger_transaction.infrastructure.transport.mapper;

import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.springframework.stereotype.Component;

@Component
public class SepaTransactionMapper implements TransactionMapper<RawSepaTransaction> {

    private final String topicName;

    public SepaTransactionMapper() {
        this.topicName = "sepa-transaction-topic";
    }

    @Override
    public boolean supports(RawTransaction transaction) {
        return transaction instanceof RawSepaTransaction;
    }

    @Override
    public byte[] toBinary(RawSepaTransaction transaction) {
        return org.neo_ledger.common.event.RawSepaTransaction.newBuilder()
                .setEndToEndId(transaction.endToEndId())
                .setDebtorIban(transaction.debtorIban())
                .setCreditorIban(transaction.creditorIban())
                .setAmount(transaction.amount().toPlainString())
                .setCurrency(transaction.currency())
                .setRequestedDate(transaction.requestedDate().toString())
                .setIsInstant(transaction.isInstant())
                .setRemittanceInfo(transaction.remittanceInfo() != null ? transaction.remittanceInfo() : "")
                .setMandateId(transaction.mandateId() != null ? transaction.mandateId() : "")
                .setCreditorSchemeId(transaction.creditorSchemeId() != null ? transaction.creditorSchemeId() : "")
                .build()
                .toByteArray();
    }
}
