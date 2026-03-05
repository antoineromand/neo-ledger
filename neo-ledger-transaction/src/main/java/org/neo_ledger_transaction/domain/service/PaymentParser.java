package org.neo_ledger_transaction.domain.service;

import org.neo_ledger_transaction.application.PaymentFileType;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

public interface PaymentParser<T> {
    T parse(InputStream stream) throws XMLStreamException;

    boolean supports(String type);
}
