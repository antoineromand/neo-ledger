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
 * Parser de fichiers SEPA Direct Debit de type {@code pain.008}.
 *
 * <p>
 * Cette implémentation utilise l'API StAX (Streaming API for XML) afin de parser
 * les fichiers XML de manière efficace en mémoire.
 * Contrairement à DOM, le document n'est pas chargé entièrement en mémoire.
 * </p>
 *
 * <p>
 * Le parser extrait :
 * <ul>
 *     <li>les informations globales du fichier ({@code GrpHdr})</li>
 *     <li>les blocs de paiement ({@code PmtInf})</li>
 *     <li>les transactions individuelles ({@code DrctDbtTxInf})</li>
 * </ul>
 * </p>
 *
 * <p>
 * Le résultat est encapsulé dans un {@link RawPaymentFile} contenant :
 * <ul>
 *     <li>un {@link FileHeader}</li>
 *     <li>une liste de {@link RawSepaTransaction}</li>
 * </ul>
 * </p>
 */
@Component
public class SepaPain008Parser implements PaymentParser<RawPaymentFile<RawSepaTransaction>> {

    /**
     * Parse un flux XML correspondant à un fichier SEPA {@code pain.008}.
     *
     * @param stream flux XML en entrée
     * @return représentation brute du fichier avec header et transactions
     * @throws XMLStreamException si le flux XML est mal formé
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
                switch (r.getLocalName()) {
                    case "GrpHdr" -> header = parseGrpHdr(r);
                    case "PmtInf" -> allTransactions.addAll(parsePmtInf(r));
                    default -> {
                    }
                }
            }
        }

        return new RawPaymentFile<>(header, allTransactions);
    }

    /**
     * Parse le bloc {@code GrpHdr} (Group Header).
     *
     * <p>
     * Ce bloc contient les métadonnées globales du fichier SEPA :
     * <ul>
     *     <li>l'identifiant du message</li>
     *     <li>le nombre total de transactions</li>
     *     <li>la date/heure de création du fichier</li>
     * </ul>
     * </p>
     *
     * @param r lecteur XML positionné sur {@code GrpHdr}
     * @return header du fichier SEPA
     * @throws XMLStreamException en cas d'erreur de parsing
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
                    default -> {
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "GrpHdr".equals(r.getLocalName())) {
                break;
            }
        }

        return new FileHeader(msgId, count, dt);
    }

    /**
     * Parse un bloc {@code PmtInf} contenant un groupe de transactions.
     *
     * <p>
     * Les informations présentes dans ce bloc sont communes aux transactions
     * qu'il contient (IBAN créancier, date de collecte, identifiant créancier).
     * </p>
     *
     * @param r lecteur XML positionné sur {@code PmtInf}
     * @return liste des transactions contenues dans ce bloc
     * @throws XMLStreamException en cas d'erreur de parsing
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
                switch (r.getLocalName()) {
                    case "ReqdColltnDt" -> requestedDate = LocalDate.parse(r.getElementText());
                    case "CdtrAcct" -> creditorIban = parseAccountIban(r, "CdtrAcct");
                    case "CdtrSchmeId" -> creditorSchemeId = parseCreditorSchemeId(r);
                    case "PmtTpInf" -> groupIsInstant = parsePaymentTypeInformation(r);

                    case "DrctDbtTxInf" -> transactions.add(parseTransaction(
                            r,
                            creditorIban,
                            groupIsInstant,
                            requestedDate,
                            creditorSchemeId
                    ));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "PmtInf".equals(r.getLocalName())) {
                break;
            }
        }

        return transactions;
    }

    /**
     * Parse une transaction individuelle {@code DrctDbtTxInf}.
     *
     * @param r lecteur XML positionné sur la transaction
     * @param creditorIban IBAN du créancier (hérité du bloc PmtInf)
     * @param isInstant indicateur de paiement instantané
     * @param groupDate date de collecte du groupe
     * @param creditorSchemeId identifiant créancier SEPA (ICS)
     * @return transaction SEPA brute
     * @throws XMLStreamException en cas d'erreur de parsing
     */
    private RawSepaTransaction parseTransaction(
            XMLStreamReader r,
            String creditorIban,
            boolean isInstant,
            LocalDate groupDate,
            String creditorSchemeId
    ) throws XMLStreamException {

        String e2eId = null;
        String debtorIban = null;
        BigDecimal amount = null;
        String currency = null;
        String remittanceInfo = null;
        String mandateId = null;

        while (r.hasNext()) {
            int event = r.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "EndToEndId" -> e2eId = r.getElementText();

                    case "InstdAmt" -> {
                        currency = r.getAttributeValue(null, "Ccy");
                        amount = new BigDecimal(r.getElementText());
                    }

                    case "DbtrAcct" -> debtorIban = parseAccountIban(r, "DbtrAcct");

                    case "DrctDbtTx" -> mandateId = parseMandateId(r);

                    case "RmtInf" -> remittanceInfo = parseRemittanceInfo(r);
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
                groupDate,
                isInstant,
                remittanceInfo,
                mandateId,
                creditorSchemeId
        );
    }

    /**
     * Extrait un IBAN à partir d'un bloc {@code CdtrAcct} ou {@code DbtrAcct}.
     *
     * @param r lecteur XML
     * @param endTag balise de fermeture attendue
     * @return IBAN extrait ou null
     * @throws XMLStreamException erreur XML
     */
    private String parseAccountIban(XMLStreamReader r, String endTag) throws XMLStreamException {
        String iban = null;

        while (r.hasNext()) {
            int event = r.next();

            if (event == XMLStreamConstants.START_ELEMENT && "IBAN".equals(r.getLocalName())) {
                iban = r.getElementText();
            } else if (event == XMLStreamConstants.END_ELEMENT && endTag.equals(r.getLocalName())) {
                break;
            }
        }

        return iban;
    }

    /**
     * Parse les informations de type de paiement.
     *
     * @param r lecteur XML
     * @return true si service level INST détecté
     * @throws XMLStreamException erreur XML
     */
    private boolean parsePaymentTypeInformation(XMLStreamReader r) throws XMLStreamException {
        boolean isInstant = false;

        while (r.hasNext()) {
            int event = r.next();

            if (event == XMLStreamConstants.START_ELEMENT && "Cd".equals(r.getLocalName())) {
                String value = r.getElementText();
                if ("INST".equals(value)) {
                    isInstant = true;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "PmtTpInf".equals(r.getLocalName())) {
                break;
            }
        }

        return isInstant;
    }

    /**
     * Extrait l'identifiant créancier SEPA (ICS).
     *
     * @param r lecteur XML
     * @return identifiant ICS
     * @throws XMLStreamException erreur XML
     */
    private String parseCreditorSchemeId(XMLStreamReader r) throws XMLStreamException {
        String creditorSchemeId = null;

        while (r.hasNext()) {
            int event = r.next();

            if (event == XMLStreamConstants.START_ELEMENT && "Othr".equals(r.getLocalName())) {
                creditorSchemeId = parseOtherIdentification(r);
            } else if (event == XMLStreamConstants.END_ELEMENT && "CdtrSchmeId".equals(r.getLocalName())) {
                break;
            }
        }

        return creditorSchemeId;
    }

    /**
     * Parse un bloc {@code Othr} contenant un identifiant générique.
     *
     * @param r lecteur XML
     * @return valeur de l'identifiant
     * @throws XMLStreamException erreur XML
     */
    private String parseOtherIdentification(XMLStreamReader r) throws XMLStreamException {
        String id = null;

        while (r.hasNext()) {
            int event = r.next();

            if (event == XMLStreamConstants.START_ELEMENT && "Id".equals(r.getLocalName())) {
                id = r.getElementText();
            } else if (event == XMLStreamConstants.END_ELEMENT && "Othr".equals(r.getLocalName())) {
                break;
            }
        }

        return id;
    }

    /**
     * Extrait la RUM (Référence Unique de Mandat).
     *
     * @param r lecteur XML
     * @return identifiant de mandat
     * @throws XMLStreamException erreur XML
     */
    private String parseMandateId(XMLStreamReader r) throws XMLStreamException {
        String mandateId = null;

        while (r.hasNext()) {
            int event = r.next();

            if (event == XMLStreamConstants.START_ELEMENT && "MndtId".equals(r.getLocalName())) {
                mandateId = r.getElementText();
            } else if (event == XMLStreamConstants.END_ELEMENT && "DrctDbtTx".equals(r.getLocalName())) {
                break;
            }
        }

        return mandateId;
    }

    /**
     * Extrait l'information de remise non structurée.
     *
     * @param r lecteur XML
     * @return texte de remittance
     * @throws XMLStreamException erreur XML
     */
    private String parseRemittanceInfo(XMLStreamReader r) throws XMLStreamException {
        String remittanceInfo = null;

        while (r.hasNext()) {
            int event = r.next();

            if (event == XMLStreamConstants.START_ELEMENT && "Ustrd".equals(r.getLocalName())) {
                remittanceInfo = r.getElementText();
            } else if (event == XMLStreamConstants.END_ELEMENT && "RmtInf".equals(r.getLocalName())) {
                break;
            }
        }

        return remittanceInfo;
    }

    /**
     * Indique si ce parser supporte le type de fichier donné.
     *
     * @param type type du fichier de paiement
     * @return true si le parser peut traiter ce type
     */
    @Override
    public boolean supports(String type) {
        return PaymentFileType.SEPA_PAIN_008.name().equals(type);
    }
}