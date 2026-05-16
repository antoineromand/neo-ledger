package org.neo_ledger.application.command;

import org.neo_ledger.domain.enums.AccountType;

public record CreateAccountCommand(
    String iban, String bic, String currency, AccountType accountType) {}
