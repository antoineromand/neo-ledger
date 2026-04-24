package org.neo_ledger_transaction;

import org.junit.jupiter.api.Test;
import org.neo_ledger_common.AbstractPostgresContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class PostgresDatabaseIntTest extends AbstractPostgresContainer {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void postgresDatabaseIntTest() {
        String sql = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                        AND table_name = 'transaction_outbox'
                    )
                """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class);

        assertEquals(Boolean.TRUE, exists, "La table transaction_outbox devrait exister");
    }
}
