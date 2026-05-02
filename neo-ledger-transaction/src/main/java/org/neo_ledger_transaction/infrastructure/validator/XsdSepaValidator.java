package org.neo_ledger_transaction.infrastructure.validator;

import jakarta.validation.ValidationException;
import org.neo_ledger_transaction.domain.port.out.XmlValidator;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link XmlValidator} dedicated to SEPA formats (ISO 20022).
 * <p>
 * This class loads official XSD schemas for PAIN.008 (Direct Debit) and
 * PAIN.001 (Credit Transfer) messages to ensure the structural compliance
 * of processed files.
 */
@Component
public class XsdSepaValidator implements XmlValidator {

    /**
     * Cache of compiled XSD schemas to optimize validation performance.
     */
    private final Map<String, Schema> schemas;

    /**
     * Constructor initializing the supported SEPA schemas.
     * * @throws SAXException If an error occurs during the loading or compilation
     * of the XSD files.
     */
    public XsdSepaValidator() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.schemas = new HashMap<>();
        this.schemas.put("SEPA_PAIN_008", factory.newSchema(getClass().getResource("/xsd/pain.008.001.11.xsd")));
        this.schemas.put("SEPA_PAIN_001", factory.newSchema(getClass().getResource("/xsd/pain.001.001.03.xsd")));
    }

    /**
     * Performs strict validation of the XML stream against the requested SEPA schema.
     *
     * @param xmlStream  The XML stream to validate.
     * @param schemaType The target schema type ("SEPA_PAIN_008" or "SEPA_PAIN_001").
     * @throws ValidationException If the XML is invalid or if the schema configuration is missing.
     */
    @Override
    public void validate(InputStream xmlStream, String schemaType) {
        Schema schema = schemas.get(schemaType);

        if (schema == null) {
            throw new ValidationException("Schema configuration missing for: " + schemaType);
        }

        try {
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlStream));
        } catch (Exception e) {
            throw new ValidationException("Error during validation: " + e.getMessage(), e);
        }
    }

    /**
     * Indicates whether the format is handled by this SEPA validator.
     * * @param format The format identifier.
     * @return {@code true} for PAIN.008 and PAIN.001 formats.
     */
    @Override
    public boolean supports(String format) {
        return "SEPA_PAIN_008".equals(format) || "SEPA_PAIN_001".equals(format);
    }
}