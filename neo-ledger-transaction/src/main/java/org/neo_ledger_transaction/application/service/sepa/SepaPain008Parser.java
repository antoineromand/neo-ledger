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
 * SEPA Direct Debit file parser for {@code pain.008} format.
 *
 * <p>
 * This implementation uses the StAX (Streaming API for XML) API to parse
 * XML files efficiently. Unlike DOM, the document is not loaded
 * entirely into memory, ensuring high performance for large files.
 * </p>
 *
 * <p>
 * The parser extracts:
 * <ul>
 * <li>Global file information ({@code GrpHdr})</li>
 * <li>Payment information blocks ({@code PmtInf})</li>
 * <li>Individual transaction details ({@code DrctDbtTxInf})</li>
 * </ul>
 * </p>
 *
 * <p>
 * The result is wrapped in a {@link RawPaymentFile} containing:
 * <ul>
 * <li>A {@link FileHeader}</li>
 * <li>A list of {@link RawSepaTransaction} objects</li>
 * </ul>
 * </p>
 */
@Component
public class SepaPain008Parser implements PaymentParser<RawPaymentFile<RawSepaTransaction>> {

    /**
     * Parses an XML stream corresponding to a SEPA {@code pain.008} file.
     *
     * @param stream The input XML stream.
     * @return A raw representation of the file, including header and transactions.
     * @throws XMLStreamException If the XML stream is malformed.
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
     * Parses the {@code GrpHdr} (Group Header) block.
     *
     * <p>
     * This block contains the global metadata for the SEPA file:
     * <ul>
     * <li>Message identifier</li>
     * <li>Total number of transactions</li>
     * <li>File creation date and time</li>
     * </ul>
     * </p>
     *
     * @param r XML reader positioned at {@code GrpHdr}.
     * @return The SEPA file header.
     * @throws XMLStreamException In case of a parsing error.
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
     * Parses a {@code PmtInf} block containing a group of transactions.
     *
     * <p>
     * Information in this block is shared across all transactions
     * it contains (Creditor IBAN, collection date, creditor identifier).
     * </p>
     *
     * @param r XML reader positioned at {@code PmtInf}.
     * @return A list of transactions contained in this block.
     * @throws XMLStreamException In case of a parsing error.
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
     * Parses an individual {@code DrctDbtTxInf} transaction.
     *
     * @param r                XML reader positioned at the transaction.
     * @param creditorIban     Creditor's IBAN (inherited from the PmtInf block).
     * @param isInstant        Instant payment indicator.
     * @param groupDate        Collection date for the group.
     * @param creditorSchemeId SEPA Creditor Identifier (ICS).
     * @return A raw SEPA transaction object.
     * @throws XMLStreamException In case of a parsing error.
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
     * Extracts an IBAN from a {@code CdtrAcct} or {@code DbtrAcct} block.
     *
     * @param r      XML reader.
     * @param endTag The expected closing tag.
     * @return The extracted IBAN or null.
     * @throws XMLStreamException XML error.
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
     * Parses payment type information.
     *
     * @param r XML reader.
     * @return {@code true} if service level "INST" is detected.
     * @throws XMLStreamException XML error.
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
     * Extracts the SEPA Creditor Identifier (ICS).
     *
     * @param r XML reader.
     * @return The ICS identifier.
     * @throws XMLStreamException XML error.
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
     * Parses an {@code Othr} block containing a generic identifier.
     *
     * @param r XML reader.
     * @return The identifier value.
     * @throws XMLStreamException XML error.
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
     * Extracts the Mandate ID (Unique Mandate Reference).
     *
     * @param r XML reader.
     * @return The mandate identifier.
     * @throws XMLStreamException XML error.
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
     * Extracts unstructured remittance information.
     *
     * @param r XML reader.
     * @return The remittance text.
     * @throws XMLStreamException XML error.
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
     * Indicates whether this parser supports the given file type.
     *
     * @param type The payment file type.
     * @return {@code true} if the parser can process this type.
     */
    @Override
    public boolean supports(String type) {
        return PaymentFileType.SEPA_PAIN_008.name().equals(type);
    }
}