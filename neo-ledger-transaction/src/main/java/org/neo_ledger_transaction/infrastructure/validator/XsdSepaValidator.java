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

@Component
public class XsdSepaValidator implements XmlValidator {
    private final Map<String, Schema> schemas;

    public XsdSepaValidator() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.schemas = new HashMap<>();
        this.schemas.put("SEPA_PAIN_008", factory.newSchema(getClass().getResource("/xsd/pain.008.001.11.xsd")));
        this.schemas.put("SEPA_PAIN_001", factory.newSchema(getClass().getResource("/xsd/pain.001.001.12.xsd")));
    }

    @Override
    public void validate(InputStream xmlStream, String schemaType) {
        Schema schema = schemas.get(schemaType);

        try {
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlStream));
        } catch (Exception e) {
            throw new ValidationException("Error during validation : " + e.getMessage(), e);
        }
    }
}