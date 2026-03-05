package br.leg.congresso.etl.transformer;

import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoProposicao;
import br.leg.congresso.etl.repository.ProposicaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeduplicationService — lógica INSERT / UPDATE / SKIP")
class DeduplicationServiceTest {

    @Mock
    private ProposicaoRepository proposicaoRepository;

    @Mock
    private ContentHashGenerator hashGenerator;

    @InjectMocks
    private DeduplicationService deduplicationService;

    private Proposicao proposicao;

    @BeforeEach
    void setup() {
        proposicao = Proposicao.builder()
                .casa(CasaLegislativa.CAMARA)
                .tipo(TipoProposicao.LEI_ORDINARIA)
                .sigla("PL")
                .numero(1234)
                .ano(2024)
                .ementa("Ementa de teste do ETL")
                .situacao("Em tramitação")
                .contentHash("abc123")
                .build();
    }

    @Test
    @DisplayName("retorna INSERT quando proposição não existe no banco")
    void retornaInsertParaNovaProposicao() {
        when(proposicaoRepository.findContentHashByChaveNatural(
                CasaLegislativa.CAMARA, "PL", 1234, 2024))
                .thenReturn(Optional.empty());

        DeduplicationService.Acao acao = deduplicationService.avaliar(proposicao);

        assertThat(acao).isEqualTo(DeduplicationService.Acao.INSERT);
    }

    @Test
    @DisplayName("retorna SKIP quando hash não mudou")
    void retornaSkipQuandoHashIgual() {
        when(proposicaoRepository.findContentHashByChaveNatural(
                CasaLegislativa.CAMARA, "PL", 1234, 2024))
                .thenReturn(Optional.of("abc123"));

        DeduplicationService.Acao acao = deduplicationService.avaliar(proposicao);

        assertThat(acao).isEqualTo(DeduplicationService.Acao.SKIP);
    }

    @Test
    @DisplayName("retorna UPDATE quando hash mudou")
    void retornaUpdateQuandoHashDiferente() {
        when(proposicaoRepository.findContentHashByChaveNatural(
                CasaLegislativa.CAMARA, "PL", 1234, 2024))
                .thenReturn(Optional.of("hash_antigo_diferente"));

        DeduplicationService.Acao acao = deduplicationService.avaliar(proposicao);

        assertThat(acao).isEqualTo(DeduplicationService.Acao.UPDATE);
    }

    @Test
    @DisplayName("retorna SKIP quando sigla é nula (chave incompleta)")
    void retornaSkipParaChaveIncomplotaSigla() {
        proposicao.setSigla(null);

        DeduplicationService.Acao acao = deduplicationService.avaliar(proposicao);

        assertThat(acao).isEqualTo(DeduplicationService.Acao.SKIP);
        verifyNoInteractions(proposicaoRepository);
    }

    @Test
    @DisplayName("retorna SKIP quando número é nulo (chave incompleta)")
    void retornaSkipParaChaveIncompletaNumero() {
        proposicao.setNumero(null);

        DeduplicationService.Acao acao = deduplicationService.avaliar(proposicao);

        assertThat(acao).isEqualTo(DeduplicationService.Acao.SKIP);
        verifyNoInteractions(proposicaoRepository);
    }

    @Test
    @DisplayName("retorna SKIP quando ano é nulo (chave incompleta)")
    void retornaSkipParaChaveIncompletaAno() {
        proposicao.setAno(null);

        DeduplicationService.Acao acao = deduplicationService.avaliar(proposicao);

        assertThat(acao).isEqualTo(DeduplicationService.Acao.SKIP);
        verifyNoInteractions(proposicaoRepository);
    }

    @Test
    @DisplayName("enriquecerComHash delega geração ao ContentHashGenerator")
    void enriquecerComHashDefinHashNaProposicao() {
        when(hashGenerator.generateForProposicao(
                "CAMARA", "PL", 1234, 2024, "Ementa de teste do ETL", "Em tramitação", false,
            null, null, null, null, null, null))
                .thenReturn("hash_gerado");

        deduplicationService.enriquecerComHash(proposicao);

        assertThat(proposicao.getContentHash()).isEqualTo("hash_gerado");
        verify(hashGenerator).generateForProposicao(
            "CAMARA", "PL", 1234, 2024, "Ementa de teste do ETL", "Em tramitação", false,
            null, null, null, null, null, null);
    }

    @Test
    @DisplayName("enriquecerComHash preenche idOrigem a partir da uriOrigem")
    void enriquecerComHashPreencheIdOrigemViaUri() {
        proposicao.setUriOrigem("https://dadosabertos.camara.leg.br/api/v2/proposicoes/253500");

        when(hashGenerator.generateForProposicao(
                "CAMARA", "PL", 1234, 2024, "Ementa de teste do ETL", "Em tramitação", false,
                "253500", "https://dadosabertos.camara.leg.br/api/v2/proposicoes/253500", null, null, null, null))
                .thenReturn("hash_com_id");

        deduplicationService.enriquecerComHash(proposicao);

        assertThat(proposicao.getIdOrigem()).isEqualTo("253500");
        assertThat(proposicao.getContentHash()).isEqualTo("hash_com_id");
        verify(hashGenerator).generateForProposicao(
                "CAMARA", "PL", 1234, 2024, "Ementa de teste do ETL", "Em tramitação", false,
                "253500", "https://dadosabertos.camara.leg.br/api/v2/proposicoes/253500", null, null, null, null);
    }
}
