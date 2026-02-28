package org.neo_ledger_transaction.service;

import java.util.List;

public class PaymentParserFactory {
    private final List<PaymentParser<?>>  paymentParsers;
    public PaymentParserFactory(List<PaymentParser<?>> paymentParsers) {
        this.paymentParsers = paymentParsers;
    }

    public PaymentParser<?> getParser(String paymentType) {
        return paymentParsers
                .stream()
                .filter(p -> p.supports(paymentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment type not supported: " + paymentType));
    }
}
