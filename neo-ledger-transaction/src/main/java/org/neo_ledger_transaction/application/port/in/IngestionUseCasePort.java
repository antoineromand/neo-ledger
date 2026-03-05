package org.neo_ledger_transaction.application.port.in;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface IngestionUseCasePort {
    void executeIngestion(InputStream file) throws XMLStreamException, IOException;
}
