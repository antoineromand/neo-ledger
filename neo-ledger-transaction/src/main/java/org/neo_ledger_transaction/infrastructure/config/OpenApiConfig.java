package org.neo_ledger_transaction.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(
                new Info()
                        .title("Neo Ledger Transaction")
                        .description("Ingest SEPA payments and send them to Neo Ledger Core.")
                        .version("0.1.0")
        ).servers(
                List.of(
                        new Server().url("http://localhost:3000").description("Local development server.")
                ));
    }
}
