package org.neo_ledger_transaction.domain.port.out;

import jakarta.validation.ValidationException;

import java.io.InputStream;

public interface XmlValidator {
    void validate(InputStream xmlStream, String schema) throws ValidationException;
}