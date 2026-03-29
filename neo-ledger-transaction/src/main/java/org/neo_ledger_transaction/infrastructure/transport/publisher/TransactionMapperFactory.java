package org.neo_ledger_transaction.infrastructure.transport.publisher;

import org.neo_ledger_transaction.domain.model.RawTransaction;
import org.neo_ledger_transaction.domain.port.out.TransactionMapperFactoryPort;
import org.neo_ledger_transaction.infrastructure.transport.mapper.TransactionMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionMapperFactory implements TransactionMapperFactoryPort {

    private final List<TransactionMapper<?>> mappers;

    public TransactionMapperFactory(List<TransactionMapper<?>> transactionMappers) {
        this.mappers = transactionMappers;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public byte[] toBinary(RawTransaction transaction) {
        TransactionMapper mapper = mappers.stream()
                .filter(m -> m.supports(transaction))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No mapper found for: " + transaction.getClass()));

        return mapper.toBinary(transaction);
    }
}
