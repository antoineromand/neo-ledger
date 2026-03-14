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
 * Parser de fichiers SEPA Credit Transfer de type {@code pain.001}.
 * Gère la version 001.001.12.
 */
@Component
public class SepaPain001Parser implements PaymentParser<RawPaymentFile<RawSepaTransaction>> {

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

    private List<RawSepaTransaction> parsePmtInf(XMLStreamReader r) throws XMLStreamException {
        List<RawSepaTransaction> transactions = new ArrayList<>();

        String debtorIban = null;
        LocalDate requestedDate = null;
        boolean groupIsInstant = false;

        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "ReqdExctnDt" -> requestedDate = parseDateChoice(r, "ReqdExctnDt");
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

    /**
     * Gère le bloc de choix Date ou DateTime pour ReqdExctnDt.
     */
    private LocalDate parseDateChoice(XMLStreamReader r, String endTag) throws XMLStreamException {
        LocalDate date = null;
        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Dt".equals(r.getLocalName())) {
                    date = LocalDate.parse(r.getElementText());
                } else if ("DtTm".equals(r.getLocalName())) {
                    date = LocalDateTime.parse(r.getElementText(), DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
                    date = LocalDateTime.parse(r.getElementText(), DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && endTag.equals(r.getLocalName())) {
                break;
            }
        }
        return date;
    }

    @Override
    public boolean supports(String type) {
        return PaymentFileType.SEPA_PAIN_001.name().equals(type);
    }
}
