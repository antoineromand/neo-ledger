package org.neo_ledger_transaction.application.service.sepa;

import org.neo_ledger_transaction.application.PaymentFileType;
import org.neo_ledger_transaction.domain.model.FileHeader;
import org.neo_ledger_transaction.domain.model.RawPaymentFile;
import org.neo_ledger_transaction.domain.model.RawSepaTransaction;
import org.neo_ledger_transaction.domain.service.PaymentParser;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation du parser pour les fichiers SEPA (PAIN.008).
 * Utilise l'API StAX pour un parsing performant (streaming).
 */
@Component
public class SepaPain008Parser implements PaymentParser<RawPaymentFile<RawSepaTransaction>> {

    /**
     * Point d'entrée principal pour le parsing d'un flux XML PAIN.008.
     * * @param stream Le flux d'entrée contenant le fichier XML.
     * @return Un objet {@link RawPaymentFile} contenant le header et la liste des transactions.
     * @throws XMLStreamException Si le flux XML est mal formé ou interrompu.
     */
    @Override
    public RawPaymentFile<RawSepaTransaction> parse(InputStream stream) throws XMLStreamException {
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xif.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        XMLStreamReader r = xif.createXMLStreamReader(stream, StandardCharsets.UTF_8.name());

        FileHeader header = null;
        List<RawSepaTransaction> allTransactions = new ArrayList<>();

        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String ln = r.getLocalName();
                if ("GrpHdr".equals(ln)) {
                    header = parseGrpHdr(r);
                } else if ("PmtInf".equals(ln)) {
                    allTransactions.addAll(parsePmtInf(r));
                }
            }
        }
        return new RawPaymentFile<>(header, allTransactions);
    }

    /**
     * Extrait les métadonnées globales du fichier situées dans le bloc Group Header (GrpHdr).
     * * @param r Le {@link XMLStreamReader} positionné au début de la balise GrpHdr.
     * @return Un {@link FileHeader} contenant l'ID du message et les infos de création.
     * @throws XMLStreamException Si une erreur de lecture survient.
     */
    private FileHeader parseGrpHdr(XMLStreamReader r) throws XMLStreamException {
        String msgId = null;
        int count = 0;
        LocalDateTime dt = null;

        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "MsgId" -> msgId = r.getElementText();
                    case "NbOfTxs" -> count = Integer.parseInt(r.getElementText());
                    case "CreDtTm" -> dt = LocalDateTime.parse(r.getElementText(), DateTimeFormatter.ISO_DATE_TIME);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "GrpHdr".equals(r.getLocalName())) {
                break;
            }
        }
        return new FileHeader(msgId, count, dt);
    }

    /**
     * Analyse un bloc d'information de paiement (PmtInf) et extrait les transactions associées.
     * Gère l'héritage des données communes (IBAN créancier, ICS, Date) vers les transactions individuelles.
     * * @param r Le {@link XMLStreamReader} positionné au début de la balise PmtInf.
     * @return Une liste de {@link RawSepaTransaction} extraites de ce bloc.
     * @throws XMLStreamException Si une erreur de lecture survient.
     */
    private List<RawSepaTransaction> parsePmtInf(XMLStreamReader r) throws XMLStreamException {
        List<RawSepaTransaction> transactions = new ArrayList<>();
        String creditorIban = null;
        String creditorSchemeId = null;
        LocalDate requestedDate = null;
        boolean groupIsInstant = false;

        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String ln = r.getLocalName();

                if ("IBAN".equals(ln)) creditorIban = r.getElementText();
                if ("ReqdColltnDt".equals(ln)) requestedDate = LocalDate.parse(r.getElementText());

                if ("CdtrSchmeId".equals(ln)) {
                    creditorSchemeId = parseSpecificId(r, "CdtrSchmeId");
                }

                if ("Cd".equals(ln) && "INST".equals(r.getElementText())) groupIsInstant = true;

                if ("DrctDbtTxInf".equals(ln)) {
                    transactions.add(parseTransaction(r, creditorIban, groupIsInstant, requestedDate, creditorSchemeId));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "PmtInf".equals(r.getLocalName())) {
                break;
            }
        }
        return transactions;
    }

    /**
     * Analyse une transaction individuelle de prélèvement (DrctDbtTxInf).
     * * @param r                Le {@link XMLStreamReader} positionné au début de DrctDbtTxInf.
     * @param creditorIban     L'IBAN du créancier hérité du bloc parent.
     * @param isInstant        Indicateur de paiement instantané hérité du bloc parent.
     * @param groupDate        La date d'exécution par défaut héritée du bloc parent.
     * @param creditorSchemeId L'identifiant créancier (ICS) hérité du bloc parent.
     * @return Un record {@link RawSepaTransaction} complet.
     * @throws XMLStreamException Si une erreur de lecture survient.
     */
    private RawSepaTransaction parseTransaction(XMLStreamReader r, String creditorIban, boolean isInstant, LocalDate groupDate, String creditorSchemeId) throws XMLStreamException {
        String e2eId = null;
        String debtorIban = null;
        BigDecimal amount = null;
        String currency = null;
        String remittanceInfo = null;
        String mandateId = null; // RUM
        LocalDate transactionDate = groupDate;

        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "EndToEndId" -> e2eId = r.getElementText();
                    case "InstdAmt" -> {
                        currency = r.getAttributeValue(null, "Ccy");
                        amount = new BigDecimal(r.getElementText());
                    }
                    case "IBAN" -> debtorIban = r.getElementText();
                    case "MndtId" -> mandateId = r.getElementText(); // Extraction de la RUM
                    case "ReqdColltnDt" -> transactionDate = LocalDate.parse(r.getElementText());
                    case "Ustrd" -> remittanceInfo = r.getElementText();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "DrctDbtTxInf".equals(r.getLocalName())) {
                break;
            }
        }

        return new RawSepaTransaction(
                e2eId,
                debtorIban,
                creditorIban,
                amount,
                currency,
                transactionDate,
                isInstant,
                remittanceInfo,
                mandateId,
                creditorSchemeId
        );
    }
    /**
     * Parcourt un bloc XML complexe pour en extraire la valeur textuelle finale (la feuille).
     * Utile pour extraire les IDs imbriqués comme l'ICS (CdtrSchmeId) ou la RUM (MndtId).
     *
     * @param r      Le XMLStreamReader positionné au début du bloc
     * @param endTag Le nom de la balise de fermeture qui délimite la recherche (ex: "CdtrSchmeId")
     * @return La valeur textuelle trouvée ou null
     */
    private String parseSpecificId(XMLStreamReader r, String endTag) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        boolean insideTargetTag = false;

        while (r.hasNext()) {
            int event = r.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                insideTargetTag = "Id".equals(r.getLocalName());
            }

            if (event == XMLStreamConstants.CHARACTERS && insideTargetTag) {
                String text = r.getText().trim();
                if (!text.isEmpty()) {
                    sb.append(text);
                }
            }

            if (event == XMLStreamConstants.END_ELEMENT) {
                if ("Id".equals(r.getLocalName())) {
                    insideTargetTag = false;
                }
                if (endTag.equals(r.getLocalName())) {
                    break;
                }
            }
        }

        String result = sb.toString();
        return result.isEmpty() ? null : result;
    }

    @Override
    public boolean supports(String type) {
        return PaymentFileType.SEPA_PAIN_008.name().equals(type);
    }
}