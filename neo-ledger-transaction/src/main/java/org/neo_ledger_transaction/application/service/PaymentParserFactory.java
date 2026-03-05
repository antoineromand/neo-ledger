package org.neo_ledger_transaction.application.service;

import org.neo_ledger_transaction.domain.service.PaymentParser;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory orchestrant la récupération des parseurs de fichiers de paiement.
 * Utilise le pattern Strategy pour fournir le parseur correspondant au format détecté.
 */
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
