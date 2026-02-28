package org.neo_ledger_transaction.application.service;

import org.neo_ledger_transaction.application.port.in.IngestionUseCasePort;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class IngestionService implements IngestionUseCasePort {
    @Override
    public void executeIngestion(File file) {

    }
}
