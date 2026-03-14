package org.neo_ledger_transaction.domain.port.out;

import jakarta.validation.ValidationException;

import java.io.InputStream;

/**
 * Port de sortie définissant le contrat pour la validation de documents XML.
 * <p>
 * Cette interface permet de valider un flux XML par rapport à un schéma spécifique (XSD)
 * et de déterminer la compatibilité du validateur avec un format donné.
 */
public interface XmlValidator {

    /**
     * Valide le flux XML fourni par rapport au schéma spécifié.
     *
     * @param xmlStream Le flux de données (InputStream) du document XML à valider.
     * @param schema    L'identifiant ou le type de schéma à appliquer (ex: "SEPA_PAIN_008").
     * @throws ValidationException Si le document XML n'est pas conforme au schéma ou si une erreur survient.
     */
    void validate(InputStream xmlStream, String schema) throws ValidationException;

    /**
     * Détermine si ce validateur est capable de traiter le format spécifié.
     *
     * @param format Le nom du format à vérifier (ex: "SEPA_PAIN_008").
     * @return {@code true} si le format est supporté, sinon {@code false}.
     */
    boolean supports(String format);
}