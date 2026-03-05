package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverCamaraTramitacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraTramitacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraTramitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraTramitacaoLoader — upsert por (proposicaoId, sequencia)")
class SilverCamaraTramitacaoLoaderTest {

    @Mock
    private SilverCamaraTramitacaoRepository repository;

    @InjectMocks
    private SilverCamaraTramitacaoLoader loader;

    private SilverCamaraProposicao proposicao;

    @BeforeEach
    void setup() {
        proposicao = SilverCamaraProposicao.builder()
            .id(UUID.randomUUID())
            .camaraId("12345")
            .siglaTipo("PL")
            .numero(100)
            .ano(2024)
            .build();
    }

    private CamaraTramitacaoDTO tramitacaoDto(int sequencia, String siglaOrgao) {
        CamaraTramitacaoDTO dto = new CamaraTramitacaoDTO();
        dto.setSequencia(sequencia);
        dto.setSiglaOrgao(siglaOrgao);
        dto.setDataHora("2024-01-15T10:00:00");
        dto.setCodSituacao("91");
        dto.setDescricaoSituacao("Aguardando Deliberação");
        dto.setDespacho("Desarquive-se");
        dto.setUriUltimoOrgao("https://camara.gov.br/orgaos/123");
        return dto;
    }

    @Test
    @DisplayName("deve inserir tramitações novas (sem duplicata)")
    void carregarTramitacoes_insereLote() {
        CamaraTramitacaoDTO dto1 = tramitacaoDto(1, "PLEN");
        CamaraTramitacaoDTO dto2 = tramitacaoDto(2, "CCJC");

        when(repository.existsByCamaraProposicaoIdAndSequencia(proposicao.getId(), 1)).thenReturn(false);
        when(repository.existsByCamaraProposicaoIdAndSequencia(proposicao.getId(), 2)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(proposicao, List.of(dto1, dto2));

        assertThat(resultado).isEqualTo(2);
        ArgumentCaptor<SilverCamaraTramitacao> captor = ArgumentCaptor.forClass(SilverCamaraTramitacao.class);
        verify(repository, times(2)).save(captor.capture());

        List<SilverCamaraTramitacao> salvas = captor.getAllValues();
        assertThat(salvas).extracting(t -> t.getCamaraProposicao().getId())
            .containsOnly(proposicao.getId());
        assertThat(salvas).extracting(SilverCamaraTramitacao::getSiglaOrgao)
            .containsExactly("PLEN", "CCJC");
    }

    @Test
    @DisplayName("deve ignorar tramitações já existentes (idempotência)")
    void carregarTramitacoes_ignoraDuplicadas() {
        CamaraTramitacaoDTO dto = tramitacaoDto(1, "PLEN");

        when(repository.existsByCamaraProposicaoIdAndSequencia(proposicao.getId(), 1)).thenReturn(true);

        int resultado = loader.carregar(proposicao, List.of(dto));

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve ignorar DTOs com sequencia nula")
    void carregarTramitacoes_skipNull_sequencia() {
        CamaraTramitacaoDTO dtoSemSeq = new CamaraTramitacaoDTO();
        dtoSemSeq.setSequencia(null);
        dtoSemSeq.setSiglaOrgao("PLEN");

        int resultado = loader.carregar(proposicao, List.of(dtoSemSeq));

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).existsByCamaraProposicaoIdAndSequencia(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve retornar 0 para lista vazia sem interagir com repositório")
    void carregarTramitacoes_listaVazia_retorna0() {
        int resultado = loader.carregar(proposicao, Collections.emptyList());

        assertThat(resultado).isEqualTo(0);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("deve mapear codSituacao String para Integer")
    void carregarTramitacoes_parseCodSituacaoComoInteiro() {
        CamaraTramitacaoDTO dto = tramitacaoDto(1, "PLEN");
        dto.setCodSituacao("185");

        when(repository.existsByCamaraProposicaoIdAndSequencia(proposicao.getId(), 1)).thenReturn(false);
        ArgumentCaptor<SilverCamaraTramitacao> captor = ArgumentCaptor.forClass(SilverCamaraTramitacao.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(proposicao, List.of(dto));

        assertThat(captor.getValue().getCodSituacao()).isEqualTo(185);
    }
}
