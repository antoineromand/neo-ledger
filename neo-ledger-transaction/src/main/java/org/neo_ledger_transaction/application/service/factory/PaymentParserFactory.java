package org.neo_ledger_transaction.application.service.factory;

import org.neo_ledger_transaction.domain.service.PaymentParser;
import org.springframework.stereotype.Component;

import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

/**
 * Factory responsible for retrieving payment file parsers.
 * <p>
 * This component utilizes the Strategy pattern to provide the appropriate parser
 * based on the detected format or namespace.
 * </p>
 */
@Component
public class PaymentParserFactory {

    private final List<PaymentParser<?>> paymentParsers;

    /**
     * Constructor injecting all available {@link PaymentParser} implementations.
     * * @param paymentParsers The list of parser beans managed by the Spring context.
     */
    public PaymentParserFactory(List<PaymentParser<?>> paymentParsers) {
        this.paymentParsers = paymentParsers;
    }

    /**
     * Retrieves a parser that supports the specified namespace or format.
     *
     * @param namespace The format identifier (e.g., an XML namespace or payment type).
     * @return A {@link PaymentParser} implementation capable of handling the format.
     * @throws ParserConfigurationException If no supporting parser is configured for the given namespace.
     */
    public PaymentParser<?> getParser(String namespace) throws ParserConfigurationException {
        return paymentParsers
                .stream()
                .filter(p -> p.supports(namespace))
                .findFirst()
                .orElseThrow(() -> new ParserConfigurationException("Parser not implemented"));
    }
}