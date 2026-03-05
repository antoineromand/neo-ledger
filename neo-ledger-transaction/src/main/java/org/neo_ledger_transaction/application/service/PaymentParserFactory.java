package org.neo_ledger_transaction.application.service;

import org.neo_ledger_transaction.application.PaymentFileType;
import org.neo_ledger_transaction.domain.service.PaymentParser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentParserFactory {
    private final List<PaymentParser<?>>  paymentParsers;
    public PaymentParserFactory(List<PaymentParser<?>> paymentParsers) {
        this.paymentParsers = paymentParsers;
    }

    public PaymentParser<?> getParser(String namespace) {
        return paymentParsers
                .stream()
                .filter(p -> p.supports(namespace))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment type not supported: " + namespace));
    }
}
