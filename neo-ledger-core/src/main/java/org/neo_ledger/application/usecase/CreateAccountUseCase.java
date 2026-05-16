package org.neo_ledger.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;
import org.neo_ledger.application.command.CreateAccountCommand;
import org.neo_ledger.application.ports.out.AccountRepositoryPort;
import org.neo_ledger.domain.Account;
import org.neo_ledger.domain.enums.AccountStatus;
import org.neo_ledger.domain.value_object.BalanceAmount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateAccountUseCase {
  private final AccountRepositoryPort accountRepository;

  public CreateAccountUseCase(AccountRepositoryPort accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Transactional
  public Account createAccount(CreateAccountCommand command) {
    Account account =
        new Account(
            UUID.randomUUID(),
            command.iban(),
            command.bic(),
            new BalanceAmount(BigDecimal.ZERO),
            new BalanceAmount(BigDecimal.ZERO),
            command.currency(),
            command.accountType(),
            AccountStatus.ACTIVE);

    return accountRepository.createAccount(account);
  }
}
