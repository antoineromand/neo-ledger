package org.neo_ledger_transaction.application.service.factory;

import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.neo_ledger_transaction.application.port.out.PaymentFileParser;
import org.springframework.stereotype.Component;

/**
 * Factory responsible for retrieving payment file parsers.
 *
 * <p>This component utilizes the Strategy pattern to provide the appropriate parser based on the
 * detected format or namespace.
 */
@Component
public class PaymentParserFactory {

  private final List<PaymentFileParser<?>> paymentParsers;

  /**
   * Constructor injecting all available {@link PaymentFileParser} implementations. * @param
   * paymentParsers The list of parser beans managed by the Spring context.
   */
  public PaymentParserFactory(List<PaymentFileParser<?>> paymentParsers) {
    this.paymentParsers = paymentParsers;
  }

  /**
   * Retrieves a parser that supports the specified namespace or format.
   *
   * @param namespace The format identifier (e.g., an XML namespace or payment type).
   * @return A {@link PaymentFileParser} implementation capable of handling the format.
   * @throws ParserConfigurationException If no supporting parser is configured for the given
   *     namespace.
   */
  public PaymentFileParser<?> getParser(String namespace) throws ParserConfigurationException {
    return paymentParsers.stream()
        .filter(p -> p.supports(namespace))
        .findFirst()
        .orElseThrow(() -> new ParserConfigurationException("Parser not implemented"));
  }
}
