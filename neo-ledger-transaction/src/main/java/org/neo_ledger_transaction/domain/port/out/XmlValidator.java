package org.neo_ledger_transaction.domain.port.out;

import jakarta.validation.ValidationException;

import java.io.InputStream;

/**
 * Output port defining the contract for XML document validation.
 * <p>
 * This interface provides a mechanism to validate an XML stream against a specific
 * schema (XSD) and to determine a validator's compatibility with a given format.
 */
public interface XmlValidator {

    /**
     * Validates the provided XML stream against the specified schema.
     *
     * @param xmlStream The input stream of the XML document to be validated.
     * @param schema    The schema identifier or type to apply (e.g., "SEPA_PAIN_008").
     * @throws ValidationException If the XML document does not conform to the schema
     * or if an error occurs during processing.
     */
    void validate(InputStream xmlStream, String schema) throws ValidationException;

    /**
     * Determines whether this validator is capable of processing the specified format.
     *
     * @param format The name of the format to verify (e.g., "SEPA_PAIN_008").
     * @return {@code true} if the format is supported, otherwise {@code false}.
     */
    boolean supports(String format);
}