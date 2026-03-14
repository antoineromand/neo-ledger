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
 * Implémentation de {@link XmlValidator} dédiée aux formats SEPA (ISO 20022).
 * <p>
 * Cette classe charge les schémas XSD officiels pour les messages PAIN.008 (Direct Debit)
 * et PAIN.001 (Credit Transfer) afin de garantir la conformité structurelle des fichiers traités.
 */
@Component
public class XsdSepaValidator implements XmlValidator {

    /**
     * Cache des schémas XSD compilés pour optimiser les performances de validation.
     */
    private final Map<String, Schema> schemas;

    /**
     * Constructeur initialisant les schémas SEPA supportés.
     * * @throws SAXException Si une erreur survient lors du chargement ou de la compilation des fichiers XSD.
     */
    public XsdSepaValidator() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.schemas = new HashMap<>();
        this.schemas.put("SEPA_PAIN_008", factory.newSchema(getClass().getResource("/xsd/pain.008.001.11.xsd")));
        this.schemas.put("SEPA_PAIN_001", factory.newSchema(getClass().getResource("/xsd/pain.001.001.12.xsd")));
    }

    /**
     * Effectue la validation stricte du flux XML par rapport au schéma SEPA demandé.
     *
     * @param xmlStream  Le flux XML à valider.
     * @param schemaType Le type de schéma cible ("SEPA_PAIN_008" ou "SEPA_PAIN_001").
     * @throws ValidationException Si le XML est invalide ou si le schéma est introuvable.
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
            throw new ValidationException("Error during validation : " + e.getMessage(), e);
        }
    }

    /**
     * Indique si le format est géré par ce validateur SEPA.
     * * @param format Identifiant du format.
     *
     * @return {@code true} pour les formats PAIN.008 et PAIN.001.
     */
    @Override
    public boolean supports(String format) {
        return "SEPA_PAIN_008".equals(format) || "SEPA_PAIN_001".equals(format);
    }
}