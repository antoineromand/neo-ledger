package org.neo_ledger_transaction.application.service.sepa;

import org.neo_ledger_transaction.domain.model.*;
import org.neo_ledger_transaction.domain.service.PaymentParser;

import javax.xml.stream.*;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SepaPain008Parser implements PaymentParser<RawPaymentFile<RawSepaTransaction>> {

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
        String creditorIban = null;
        boolean groupIsInstant = false;

        while (r.hasNext()) {
            int event = r.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String ln = r.getLocalName();

                if ("IBAN".equals(ln)) creditorIban = r.getElementText();
                if ("Cd".equals(ln)) {
                    if ("INST".equals(r.getElementText())) groupIsInstant = true;
                }

                if ("DrctDbtTxInf".equals(ln)) {
                    transactions.add(parseTransaction(r, creditorIban, groupIsInstant));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "PmtInf".equals(r.getLocalName())) {
                break;
            }
        }
        return transactions;
    }

    private RawSepaTransaction parseTransaction(XMLStreamReader r, String creditorIban, boolean isInstant) throws XMLStreamException {
        String e2eId = null;
        String debtorIban = null;
        BigDecimal amount = null;
        String currency = null;
        LocalDate requestedDate = null;
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
                    case "IBAN" -> debtorIban = r.getElementText();
                    case "ReqdColltnDt" -> requestedDate = LocalDate.parse(r.getElementText());
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
                requestedDate,
                isInstant,
                remittanceInfo
        );
    }

    @Override
    public boolean supports(String paymentType) {
        return "SEPA_PAIN_008".equalsIgnoreCase(paymentType);
    }
}