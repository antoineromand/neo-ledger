package org.neo_ledger_transaction.service;

import org.neo_ledger_transaction.RawPaymentFile;
import org.neo_ledger_transaction.RawSepaTransaction;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

public class SepaPain008Parser implements PaymentParser<RawPaymentFile<RawSepaTransaction>> {
    @Override
    public RawPaymentFile<RawSepaTransaction> parse(InputStream stream) throws XMLStreamException {
        return null;
    }

    @Override
    public boolean supports(String paymentType) {
        return "SEPA_PAIN_008".equalsIgnoreCase(paymentType);
    }
}
