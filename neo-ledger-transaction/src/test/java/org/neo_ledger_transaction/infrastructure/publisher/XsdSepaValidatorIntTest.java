package org.neo_ledger_transaction.infrastructure.publisher;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.neo_ledger_transaction.infrastructure.validator.XsdSepaValidator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class XsdSepaValidatorIntTest {
    private final XsdSepaValidator xsdSepaValidator = new XsdSepaValidator();

    public XsdSepaValidatorIntTest() throws SAXException {
    }

    @Test
    public void validateShouldThrowError() throws IOException, SAXException {
        InputStream xml = getClass().getResourceAsStream("/sepa-008-sample.xml");

        assertThrows(ValidationException.class, () -> this.xsdSepaValidator.validate(xml, "SEPA_PAIN_OO1"));
    }

    @Test
    public void validate() throws IOException, SAXException {
        InputStream xml = getClass().getResourceAsStream("/sepa-008-sample.xml");
        this.xsdSepaValidator.validate(xml, "SEPA_PAIN_008");
    }
}
