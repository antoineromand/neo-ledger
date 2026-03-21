package org.neo_ledger_transaction.application.service.factory;

import org.neo_ledger_transaction.domain.port.out.XmlValidator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory responsible for providing the appropriate {@link XmlValidator} implementation.
 * <p>
 * It leverages Spring's dependency injection to collect all available validator beans
 * and selects the one that matches the requested format via the {@code supports} method.
 */
@Component
public class XmlValidatorFactory {
    private final List<XmlValidator> validators;

    /**
     * Constructor injecting the list of all beans implementing {@link XmlValidator}.
     *
     * @param validators The list of available validators.
     */
    public XmlValidatorFactory(List<XmlValidator> validators) {
        this.validators = validators;
    }

    /**
     * Retrieves a validator compatible with the specified format.
     *
     * @param format The target file format (e.g., "SEPA_PAIN_008").
     * @return An implementation of {@link XmlValidator} that supports the format.
     * @throws IllegalArgumentException If no implementation supports the provided format.
     */
    public XmlValidator getValidator(String format) {
        return validators
                .stream()
                .filter(validator -> validator.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No validator found for " + format));
    }
}