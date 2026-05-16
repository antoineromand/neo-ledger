package org.neo_ledger.application.ports.out;

import org.neo_ledger.domain.Account;

public interface AccountRepositoryPort {
  Account createAccount(Account account);
}
