package org.neo_ledger_transaction.application.service.factory;

import org.neo_ledger_transaction.domain.port.out.XmlValidator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory chargée de fournir l'implémentation appropriée de {@link XmlValidator}.
 * <p>
 * Elle utilise l'injection de dépendances de Spring pour collecter tous les validateurs disponibles
 * et sélectionne celui qui correspond au format demandé via la méthode {@code supports}.
 */
@Component
public class XmlValidatorFactory {
    private final List<XmlValidator> validators;

    /**
     * Constructeur injectant la liste de tous les beans implémentant {@link XmlValidator}.
     *
     * @param validators Liste des validateurs disponibles.
     */
    public XmlValidatorFactory(List<XmlValidator> validators) {
        this.validators = validators;
    }

    /**
     * Récupère un validateur compatible avec le format spécifié.
     *
     * @param format Le format de fichier cible (ex: "SEPA_PAIN_008").
     * @return Une implémentation de {@link XmlValidator} supportant le format.
     * @throws IllegalArgumentException Si aucune implémentation ne supporte le format fourni.
     */
    public XmlValidator getValidator(String format) {
        return validators
                .stream()
                .filter(validator -> validator.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No validator found for " + format));
    }
}
