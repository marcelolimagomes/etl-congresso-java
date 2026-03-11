package br.leg.congresso.etl.repository;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração do repositório de proposições.
 *
 * <p>Requer PostgreSQL disponível em localhost:5433 (docker-compose up).
 * Para personalizar: {@code -Dtest.datasource.url=jdbc:postgresql://...}
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("ProposicaoRepository — operações de persistência e upsert")
class ProposicaoRepositoryIT {

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

    @Autowired
    private ProposicaoRepository repository;

    private static final CasaLegislativa CASA = CasaLegislativa.CAMARA;
    private static final String SIGLA = "PL";
    private static final int NUMERO = 9999;
    private static final int ANO = 2024;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("upsert insere novo registro com sucesso")
    void upsertInsereNovoRegistro() {
        int rows = executarUpsert("hash_v1", "Em tramitação", "Ementa original");

        assertThat(rows).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);

        Optional<Proposicao> salvo = repository.findByCasaAndSiglaAndNumeroAndAno(CASA, SIGLA, NUMERO, ANO);
        assertThat(salvo).isPresent();
        assertThat(salvo.get().getEmenta()).isEqualTo("Ementa original");
        assertThat(salvo.get().getSituacao()).isEqualTo("Em tramitação");
    }

    @Test
    @DisplayName("upsert atualiza registro existente quando hash muda")
    void upsertAtualizaQuandoHashMuda() {
        executarUpsert("hash_v1", "Em tramitação", "Ementa original");

        int rows = executarUpsert("hash_v2", "Aprovado", "Ementa atualizada");

        assertThat(rows).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);  // continua 1 registro

        Optional<Proposicao> atualizado = repository.findByCasaAndSiglaAndNumeroAndAno(CASA, SIGLA, NUMERO, ANO);
        assertThat(atualizado).isPresent();
        assertThat(atualizado.get().getSituacao()).isEqualTo("Aprovado");
        assertThat(atualizado.get().getEmenta()).isEqualTo("Ementa atualizada");
        assertThat(atualizado.get().getContentHash()).isEqualTo("hash_v2");
    }

    @Test
    @DisplayName("upsert retorna 0 linhas quando hash não muda (SKIP)")
    void upsertSkipQuandoHashIgual() {
        executarUpsert("hash_v1", "Em tramitação", "Ementa original");

        // Segundo insert com mesmo hash
        int rows = executarUpsert("hash_v1", "Em tramitação", "Ementa original");

        assertThat(rows).isEqualTo(0);  // nada foi alterado
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("findContentHashByChaveNatural retorna hash do registro existente")
    void findContentHashRetornaHashCorreto() {
        executarUpsert("meu_hash_sha256", "Em tramitação", "Ementa");

        Optional<String> hash = repository.findContentHashByChaveNatural(CASA, SIGLA, NUMERO, ANO);

        assertThat(hash).isPresent().hasValue("meu_hash_sha256");
    }

    @Test
    @DisplayName("findContentHashByChaveNatural retorna vazio para registro inexistente")
    void findContentHashRetornaVazioSeNaoExiste() {
        Optional<String> hash = repository.findContentHashByChaveNatural(CASA, SIGLA, NUMERO, ANO);
        assertThat(hash).isEmpty();
    }

    @Test
    @DisplayName("countByCasa conta corretamente por casa legislativa")
    void countByCasaFunciona() {
        executarUpsert("h1", "sit", "ementa");

        // Insere um registro do Senado para verificar isolamento
        repository.upsert(
                CasaLegislativa.SENADO.name(), TipoProposicao.LEI_ORDINARIA.name(),
                "PLS", 1, 2024, "Ementa Senado", "Em tramitação", null,
                null, null, null, null, null, null, null, "h_senado");

        assertThat(repository.countByCasa(CasaLegislativa.CAMARA)).isEqualTo(1);
        assertThat(repository.countByCasa(CasaLegislativa.SENADO)).isEqualTo(1);
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private int executarUpsert(String contentHash, String situacao, String ementa) {
        return repository.upsert(
                CASA.name(),
                TipoProposicao.LEI_ORDINARIA.name(),
                SIGLA, NUMERO, ANO,
                ementa, situacao,
                null,   // despachoAtual
                LocalDate.of(2024, 1, 15),
                LocalDateTime.now(),
                null,   // statusFinal
                "id_camara_" + NUMERO,
                "https://camara.leg.br/proposicoes/" + NUMERO,
                null,   // urlInteiroTeor
                null,   // keywords
                contentHash
        );
    }
}
