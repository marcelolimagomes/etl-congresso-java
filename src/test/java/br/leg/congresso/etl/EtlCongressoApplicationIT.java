package br.leg.congresso.etl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Teste de integração do contexto Spring Boot completo.
 *
 * <p>Requer PostgreSQL disponível. Por padrão usa localhost:5433 (docker-compose up).
 * Para personalizar: {@code -Dtest.datasource.url=jdbc:postgresql://...}
 *
 * <p>Para executar:
 * <pre>
 *   docker compose up -d postgres
 *   mvn verify -Dspring.profiles.active=test
 * </pre>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("EtlCongressoApplication — contexto Spring Boot sobe com sucesso")
class EtlCongressoApplicationIT {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> System.getProperty("test.datasource.url",
                        "jdbc:postgresql://localhost:5433/etl_congresso"));
        registry.add("spring.datasource.username",
                () -> System.getProperty("test.datasource.username", "etl_user"));
        registry.add("spring.datasource.password",
                () -> System.getProperty("test.datasource.password", "etl_pass"));
    }

    @Test
    @DisplayName("contexto Spring Boot inicializa sem erros (Flyway + Hibernate + Security + WebClient)")
    void contextLoads() {
        // Contexto carregado com sucesso = todos os beans criados corretamente
    }
}
