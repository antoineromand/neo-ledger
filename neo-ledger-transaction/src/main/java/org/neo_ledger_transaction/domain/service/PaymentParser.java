package org.neo_ledger_transaction.domain.service;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

/**
 * Contrat de base pour le parsing des fichiers de transactions financières.
 * * <p>Chaque implémentation est responsable de l'extraction des données brutes
 * d'un format spécifique (XML, CSV, etc.) vers le modèle de domaine.</p>
 * * @param <T> Le type de fichier de paiement retourné (ex: RawPaymentFile).
 */
public interface PaymentParser<T> {

    /**
     * Analyse le flux d'entrée et le transforme en objet de domaine.
     * * @param stream Le flux binaire du fichier (doit être ouvert et prêt à la lecture).
     *
     * @return L'objet de domaine représentant le fichier parsé.
     * @throws XMLStreamException En cas d'erreur de lecture ou de format invalide.
     */
    T parse(InputStream stream) throws XMLStreamException;

    /**
     * Définit si ce parseur est capable de traiter le type de paiement donné.
     * * @param type L'identifiant du type de paiement (souvent le nom de l'Enum PaymentFileType).
     * @return true si le format est supporté.
     */
    boolean supports(String type);
}