package org.neo_ledger_transaction.application.port.out;

import java.io.InputStream;
import javax.xml.stream.XMLStreamException;

/**
 * Contract for payment file parsers owned by the application layer.
 *
 * <p>Implementations parse XML input streams into parsed payment file objects.
 *
 * @param <T> Parsed payment file type.
 */
public interface PaymentFileParser<T> {

  /**
   * Parses the input stream into a structured payment file representation.
   *
   * @param stream XML input stream to parse.
   * @return Parsed payment file.
   * @throws XMLStreamException If the XML stream is malformed or cannot be read.
   */
  T parse(InputStream stream) throws XMLStreamException;

  /**
   * Indicates whether the parser supports the given payment type.
   *
   * @param type Payment type identifier.
   * @return {@code true} when the parser can handle the type.
   */
  boolean supports(String type);
}
