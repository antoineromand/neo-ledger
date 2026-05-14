package org.neo_ledger_transaction.infrastructure.transport.mapper;

import org.neo_ledger_transaction.domain.model.ParsedSepaTransaction;
import org.neo_ledger_transaction.domain.model.ParsedTransaction;
import org.springframework.stereotype.Component;

@Component
public class SepaTransactionMapper implements TransactionMapper<ParsedSepaTransaction> {

  public SepaTransactionMapper() {}

  @Override
  public boolean supports(ParsedTransaction transaction) {
    return transaction instanceof ParsedSepaTransaction;
  }

  @Override
  public byte[] toBinary(ParsedSepaTransaction transaction) {
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
        .setCreditorSchemeId(
            transaction.creditorSchemeId() != null ? transaction.creditorSchemeId() : "")
        .setPaymentType(transaction.paymentType())
        .build()
        .toByteArray();
  }
}
