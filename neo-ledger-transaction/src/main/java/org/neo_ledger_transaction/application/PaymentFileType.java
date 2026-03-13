package org.neo_ledger_transaction.application;

import java.util.Arrays;
import java.util.List;

public enum PaymentFileType {
    SEPA_PAIN_008(List.of(
            "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02",
            "urn:iso:std:iso:20022:tech:xsd:pain.008.001.11"
    )),
    SEPA_PAIN_001(List.of(
            "urn:iso:std:iso:20022:tech:xsd:pain.001.001.12"
    ));

    private final List<String> namespaces;

    PaymentFileType(List<String> namespaces) {
        this.namespaces = namespaces;
    }

    public static PaymentFileType fromNamespace(String namespace) {
        return Arrays.stream(values())
                .filter(type -> type.namespaces.contains(namespace))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Namespace inconnu : " + namespace));
    }
}