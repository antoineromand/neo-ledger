package org.neo_ledger_transaction.domain.service;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

public interface PaymentParser<T> {
    T parse(InputStream stream) throws XMLStreamException;

    boolean supports(String paymentType);
}
