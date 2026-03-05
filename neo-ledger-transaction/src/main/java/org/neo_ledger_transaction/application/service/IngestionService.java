package org.neo_ledger_transaction.application.service;

import org.neo_ledger_transaction.application.PaymentFileType;
import org.neo_ledger_transaction.application.port.in.IngestionUseCasePort;
import org.neo_ledger_transaction.domain.model.RawPaymentFile;
import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionEventPublisher;
import org.neo_ledger_transaction.domain.service.PaymentParser;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service d'application responsable de l'ingestion des fichiers de paiement.
 * <p>
 * Ce service orchestre le processus de détection du format, le parsing des données
 * et la publication des transactions vers les systèmes tiers via un port de sortie.
 * </p>
 */
@Service
public class IngestionService implements IngestionUseCasePort {

    private final PaymentParserFactory factory;
    private final TransactionEventPublisher eventPublisher;

    /**
     * Constructeur pour l'injection des dépendances.
     * * @param paymentParserFactory La factory permettant de récupérer le parseur adapté au type de fichier.
     *
     * @param eventPublisher Le port de sortie pour la publication des événements de transaction (ex: Kafka).
     */
    public IngestionService(PaymentParserFactory paymentParserFactory, TransactionEventPublisher eventPublisher) {
        this.factory = paymentParserFactory;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Exécute le flux complet d'ingestion d'un fichier de paiement.
     * <p>
     * Le processus utilise un {@link BufferedInputStream} pour permettre une double lecture :
     * 1. Une lecture partielle pour détecter le type de fichier (Namespace XML).
     * 2. Une lecture complète pour le parsing et l'extraction des transactions.
     * </p>
     * * @param file Le flux binaire (InputStream) du fichier à traiter.
     * @throws XMLStreamException Si le contenu XML est invalide ou corrompu.
     * @throws IOException        En cas de problème de lecture du flux.
     */
    @Override
    public void executeIngestion(InputStream file) throws XMLStreamException, IOException {
        BufferedInputStream bis = new BufferedInputStream(file);
        bis.mark(4096);
        String paymentType = this.detectPaymentType(bis);
        bis.reset();

        PaymentParser<RawPaymentFile<? extends RawTransaction>> parser =
                (PaymentParser<RawPaymentFile<? extends RawTransaction>>) this.factory.getParser(paymentType);

        RawPaymentFile<? extends RawTransaction> res = parser.parse(bis);

        res.transactions().forEach(this.eventPublisher::publish);

    }

    /**
     * Analyse le début du flux XML pour identifier le Namespace du document.
     * <p>
     * Cette méthode recherche la balise racine {@code <Document>} et utilise
     * son URI de namespace pour mapper vers un {@link PaymentFileType}.
     * </p>
     * * @param stream Le flux à analyser.
     * @return Le nom (String) du type de paiement détecté.
     * @throws XMLStreamException Si la structure XML ne permet pas la détection.
     * @throws IllegalArgumentException Si le namespace détecté est inconnu.
     */
    private String detectPaymentType(InputStream stream) throws XMLStreamException {
        XMLInputFactory xif = XMLInputFactory.newFactory();
        XMLStreamReader r = xif.createXMLStreamReader(stream);

        while (r.hasNext()) {
            if (r.next() == XMLStreamConstants.START_ELEMENT && "Document".equals(r.getLocalName())) {
                return PaymentFileType.fromNamespace(r.getNamespaceURI()).name();
            }
        }
        throw new IllegalArgumentException("Unknown payment type");
    }
}
