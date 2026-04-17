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
 * SEPA Credit Transfer file parser for {@code pain.001} format.
 * <p>
 * This implementation handles version 001.001.03 (used by banks) and follows the StAX
 * streaming model to maintain a low memory footprint during processing.
 * </p>
 */
@Component
public class SepaPain001Parser implements PaymentParser<RawPaymentFile<RawSepaTransaction>> {

    /**
     * Parses a SEPA Credit Transfer XML stream.
     *
     * @param stream The input XML stream.
     * @return A raw representation of the payment file.
     * @throws XMLStreamException If the XML content is malformed.
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
                }
            }
        }
        return new RawPaymentFile<>(header, allTransactions);
    }

    /**
     * Parses the {@code GrpHdr} (Group Header) element.
     *
     * @param r XML reader positioned at {@code GrpHdr}.
     * @return The file header metadata.
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
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "GrpHdr".equals(r.getLocalName())) {
                break;
            }
        }
        return new FileHeader(msgId, count, dt);
    }

    /**
     * Parses a {@code PmtInf} (Payment Information) block.
     *
     * @param r XML reader positioned at {@code PmtInf}.
     * @return A list of extracted credit transfer transactions.
     * @throws XMLStreamException In case of a parsing error.
     */
    private List<RawSepaTransaction> parsePmtInf(XMLStreamReader r) throws XMLStreamException {
        List<RawSepaTransaction> transactions = new ArrayList<>();

        String debtorIban = null;
        LocalDate requestedDate = null;
        boolean groupIsInstant = false;

        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "ReqdExctnDt" -> requestedDate = LocalDate.parse(r.getElementText());
                    case "DbtrAcct" -> debtorIban = parseAccountIban(r, "DbtrAcct");
                    case "PmtTpInf" -> groupIsInstant = parsePaymentTypeInformation(r);
                    case "CdtTrfTxInf" -> transactions.add(parseTransaction(
                            r,
                            debtorIban,
                            groupIsInstant,
                            requestedDate
                    ));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "PmtInf".equals(r.getLocalName())) {
                break;
            }
        }
        return transactions;
    }

    /**
     * Parses a {@code CdtTrfTxInf} (Credit Transfer Transaction Information) block.
     *
     * @param r          XML reader positioned at the transaction level.
     * @param debtorIban Debtor's IBAN (inherited from the group).
     * @param isInstant  Flag indicating an instant payment.
     * @param groupDate  The requested execution date.
     * @return A raw SEPA transaction object.
     * @throws XMLStreamException In case of a parsing error.
     */
    private RawSepaTransaction parseTransaction(
            XMLStreamReader r,
            String debtorIban,
            boolean isInstant,
            LocalDate groupDate
    ) throws XMLStreamException {

        String e2eId = null;
        String creditorIban = null;
        BigDecimal amount = null;
        String currency = null;
        String remittanceInfo = null;

        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "EndToEndId" -> e2eId = r.getElementText();
                    case "InstdAmt" -> {
                        currency = r.getAttributeValue(null, "Ccy");
                        amount = new BigDecimal(r.getElementText());
                    }
                    case "CdtrAcct" -> creditorIban = parseAccountIban(r, "CdtrAcct");
                    case "RmtInf" -> remittanceInfo = parseRemittanceInfo(r);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "CdtTrfTxInf".equals(r.getLocalName())) {
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
                null,
                null
        );
    }

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

    private boolean parsePaymentTypeInformation(XMLStreamReader r) throws XMLStreamException {
        boolean isInstant = false;
        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT && "Cd".equals(r.getLocalName())) {
                if ("INST".equals(r.getElementText())) isInstant = true;
            } else if (event == XMLStreamConstants.END_ELEMENT && "PmtTpInf".equals(r.getLocalName())) {
                break;
            }
        }
        return isInstant;
    }

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

    @Override
    public boolean supports(String type) {
        return PaymentFileType.SEPA_PAIN_001.name().equals(type);
    }
}