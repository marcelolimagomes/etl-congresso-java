package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoMovimentacao;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMovimentacaoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoMovimentacaoRepository;
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
@DisplayName("SilverSenadoMovimentacaoLoader — upsert por (materiaId, sequenciaMovimentacao)")
class SilverSenadoMovimentacaoLoaderTest {

    @Mock
    private SilverSenadoMovimentacaoRepository repository;

    @InjectMocks
    private SilverSenadoMovimentacaoLoader loader;

    private SilverSenadoMateria materia;

    @BeforeEach
    void setup() {
        materia = SilverSenadoMateria.builder()
            .id(UUID.randomUUID())
            .codigo("12345")
            .sigla("PL")
            .numero("100")
            .ano(2024)
            .build();
    }

    private SenadoMovimentacaoDTO.Movimentacao movimentacaoDto(String sequencia, String descricao) {
        SenadoMovimentacaoDTO.Movimentacao mov = new SenadoMovimentacaoDTO.Movimentacao();
        mov.setSequenciaMovimentacao(sequencia);
        mov.setDescricaoMovimentacao(descricao);
        mov.setDataMovimentacao("2024-01-10");
        mov.setDescricaoSituacao("Em tramitação");
        mov.setDespacho("Aprove-se");
        mov.setAmbito("SF");

        SenadoMovimentacaoDTO.Local local = new SenadoMovimentacaoDTO.Local();
        local.setSiglaLocal("CAE");
        local.setNomeLocal("Comissão de Assuntos Econômicos");
        mov.setLocal(local);

        return mov;
    }

    @Test
    @DisplayName("deve inserir movimentações novas (sem duplicata)")
    void carregarMovimentacoes_insereLote() {
        SenadoMovimentacaoDTO.Movimentacao mov1 = movimentacaoDto("1", "Lida em Plenário");
        SenadoMovimentacaoDTO.Movimentacao mov2 = movimentacaoDto("2", "Encaminhada à Comissão");

        when(repository.existsBySenadoMateriaIdAndSequenciaMovimentacao(materia.getId(), "1")).thenReturn(false);
        when(repository.existsBySenadoMateriaIdAndSequenciaMovimentacao(materia.getId(), "2")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int resultado = loader.carregar(materia, List.of(mov1, mov2));

        assertThat(resultado).isEqualTo(2);
        ArgumentCaptor<SilverSenadoMovimentacao> captor =
            ArgumentCaptor.forClass(SilverSenadoMovimentacao.class);
        verify(repository, times(2)).save(captor.capture());

        List<SilverSenadoMovimentacao> salvas = captor.getAllValues();
        assertThat(salvas).extracting(m -> m.getSenadoMateria().getId())
            .containsOnly(materia.getId());
        assertThat(salvas).extracting(SilverSenadoMovimentacao::getDescricaoMovimentacao)
            .containsExactly("Lida em Plenário", "Encaminhada à Comissão");
    }

    @Test
    @DisplayName("deve ignorar movimentações já existentes (idempotência)")
    void carregarMovimentacoes_ignoraDuplicadas() {
        SenadoMovimentacaoDTO.Movimentacao mov = movimentacaoDto("1", "Lida em Plenário");

        when(repository.existsBySenadoMateriaIdAndSequenciaMovimentacao(materia.getId(), "1")).thenReturn(true);

        int resultado = loader.carregar(materia, List.of(mov));

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve ignorar DTOs com sequenciaMovimentacao nula")
    void carregarMovimentacoes_skipNull_sequencia() {
        SenadoMovimentacaoDTO.Movimentacao movSemSeq = new SenadoMovimentacaoDTO.Movimentacao();
        movSemSeq.setSequenciaMovimentacao(null);
        movSemSeq.setDescricaoMovimentacao("Sem sequência");

        int resultado = loader.carregar(materia, List.of(movSemSeq));

        assertThat(resultado).isEqualTo(0);
        verify(repository, never()).existsBySenadoMateriaIdAndSequenciaMovimentacao(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve retornar 0 para lista vazia sem interagir com repositório")
    void carregarMovimentacoes_listaVazia_retorna0() {
        int resultado = loader.carregar(materia, Collections.emptyList());

        assertThat(resultado).isEqualTo(0);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("deve extrair siglaLocal e nomeLocal de Local com null-safety")
    void carregarMovimentacoes_extraiLocalComNullSafe() {
        SenadoMovimentacaoDTO.Movimentacao movSemLocal = movimentacaoDto("1", "Sem Local");
        movSemLocal.setLocal(null);

        when(repository.existsBySenadoMateriaIdAndSequenciaMovimentacao(materia.getId(), "1")).thenReturn(false);
        ArgumentCaptor<SilverSenadoMovimentacao> captor =
            ArgumentCaptor.forClass(SilverSenadoMovimentacao.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(materia, List.of(movSemLocal));

        SilverSenadoMovimentacao salva = captor.getValue();
        assertThat(salva.getSiglaLocal()).isNull();
        assertThat(salva.getNomeLocal()).isNull();
    }
}
