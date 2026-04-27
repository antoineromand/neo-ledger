package org.neo_ledger_transaction.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo_ledger.common.exceptions.ApplicationException;
import org.neo_ledger_common.AbstractPostgresContainer;
import org.neo_ledger_transaction.application.port.in.IngestionUseCasePort;
import org.neo_ledger_transaction.infrastructure.repository.TransactionOutboxJpaRepository;
import org.neo_ledger_transaction.infrastructure.repository.TransactionOutboxRepositoryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class IngestionServiceRollbackIntTest extends AbstractPostgresContainer {

    @Autowired
    private IngestionUseCasePort ingestionUseCasePort;

    @Autowired
    private TransactionOutboxJpaRepository transactionOutboxJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private TransactionOutboxRepositoryAdapter transactionOutboxRepositoryAdapter;

    @BeforeEach
    void setUp() {
        transactionOutboxJpaRepository.deleteAllInBatch();
    }

    @Test
    void should_rollback_all_outbox_entries_when_second_save_fails() throws Exception {
        AtomicInteger saveInvocationCount = new AtomicInteger(0);

        doAnswer(invocation -> {
            int currentCall = saveInvocationCount.incrementAndGet();

            if (currentCall == 2) {
                throw new RuntimeException("Simulated outbox failure on second save");
            }

            Object result = invocation.callRealMethod();
            transactionOutboxJpaRepository.flush();
            return result;
        }).when(transactionOutboxRepositoryAdapter)
                .save(anyString(), anyString(), anyString(), any(byte[].class));

        try (InputStream inputStream = getClass().getResourceAsStream("/pain001_1776420067.xml")) {
            assertNotNull(inputStream, "Test file pain001_1776420067.xml not found");
            assertEquals(0L, countOutboxRows(), "Outbox should be empty before ingestion");

            assertThrows(ApplicationException.class, () -> ingestionUseCasePort.executeIngestion(inputStream));
        }

        verify(transactionOutboxRepositoryAdapter, times(2))
                .save(anyString(), anyString(), anyString(), any(byte[].class));

        assertEquals(0L, countOutboxRows(), "Outbox should be empty after rollback");
    }

    private long countOutboxRows() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transaction_outbox", Long.class);
        return count == null ? 0L : count;
    }
}
