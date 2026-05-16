package org.neo_ledger.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.neo_ledger.application.command.CreateAccountCommand;
import org.neo_ledger.application.ports.out.AccountRepositoryPort;
import org.neo_ledger.domain.Account;
import org.neo_ledger.domain.enums.AccountStatus;
import org.neo_ledger.domain.enums.AccountType;
import org.neo_ledger.domain.value_object.BalanceAmount;

class CreateAccountUseCaseTest {

  @Test
  void should_create_account_with_initial_state_and_delegate_to_repository() {
    InMemoryAccountRepository repository = new InMemoryAccountRepository();
    CreateAccountUseCase useCase = new CreateAccountUseCase(repository);
    CreateAccountCommand command =
        new CreateAccountCommand(
            "FR7630006000011234567890189", "AGRIFRPP", "EUR", AccountType.CUSTOMER);

    Account created = useCase.createAccount(command);

    assertNotNull(created.getIdentity());
    assertEquals(command.iban(), created.getIban());
    assertEquals(command.bic(), created.getBic());
    assertEquals(command.currency(), created.getCurrency());
    assertEquals(command.accountType(), created.getAccountType());
    assertEquals(AccountStatus.ACTIVE, created.getAccountStatus());
    assertEquals(new BalanceAmount(java.math.BigDecimal.ZERO), created.getCurrentBalance());
    assertEquals(new BalanceAmount(java.math.BigDecimal.ZERO), created.getReservedBalance());
    assertSame(created, repository.savedAccount);
  }

  private static final class InMemoryAccountRepository implements AccountRepositoryPort {
    private Account savedAccount;

    @Override
    public Account createAccount(Account account) {
      this.savedAccount = account;
      return account;
    }
  }
}
