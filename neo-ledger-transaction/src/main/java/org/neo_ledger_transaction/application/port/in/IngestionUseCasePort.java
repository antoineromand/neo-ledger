package org.neo_ledger_transaction.application.port.in;

import java.io.File;

public interface IngestionUseCasePort {
    void executeIngestion(File file);
}
