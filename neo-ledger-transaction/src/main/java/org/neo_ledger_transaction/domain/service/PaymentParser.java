package org.neo_ledger_transaction.domain.service;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

/**
 * Base contract for parsing financial transaction files.
 * <p>Each implementation is responsible for extracting raw data
 * from an XML file into the domain model.</p>
 *
 * @param <T> The type of payment file returned (e.g., RawPaymentFile).
 */
public interface PaymentParser<T> {

    /**
     * Parses the input stream and transforms it into a domain object.
     *
     * @param stream The binary file stream (must be open and ready for reading).
     * @return The domain object representing the parsed file.
     * @throws XMLStreamException In case of a reading error or invalid format.
     */
    T parse(InputStream stream) throws XMLStreamException;

    /**
     * Determines if this parser is capable of handling the given payment type.
     *
     * @param type The payment type identifier (often the name of the PaymentFileType Enum).
     * @return true if the format is supported.
     */
    boolean supports(String type);
}