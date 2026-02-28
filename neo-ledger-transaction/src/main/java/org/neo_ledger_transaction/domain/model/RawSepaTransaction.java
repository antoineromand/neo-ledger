package org.neo_ledger_transaction.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RawSepaTransaction(
        String endToEndId,
        String debtorIban,
        String creditorIban,
        BigDecimal amount,
        String currency,
        LocalDate requestedDate,
        boolean isInstant,
        String remittanceInfo
) {}
