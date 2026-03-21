package org.neo_ledger_transaction.infrastructure.transport.mapper;

import org.neo_ledger_transaction.domain.model.RawTransaction;

/**
 * Strategy interface for mapping domain transactions to transport-specific binary formats.
 * <p>
 * This mapper is responsible for serializing a {@link RawTransaction} or its subtypes
 * into a byte array suitable for messaging systems or network transmission.
 * </p>
 *
 * @param <T> The specific subtype of RawTransaction handled by this mapper.
 */
public interface TransactionMapper<T extends RawTransaction> {

    /**
     * Checks if this mapper supports the given transaction instance.
     *
     * @param transaction The transaction to evaluate.
     * @return {@code true} if this mapper can process the transaction, otherwise {@code false}.
     */
    boolean supports(RawTransaction transaction);

    /**
     * Converts the transaction into its binary representation.
     *
     * @param transaction The transaction object to be mapped.
     * @return A byte array representing the serialized transaction.
     * @throws RuntimeException If a mapping or serialization error occurs.
     */
    byte[] toBinary(T transaction);
}