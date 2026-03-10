package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacaoOrientacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoOrientacaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraVotacaoOrientacaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraVotacoesOrientacoesLoader — upsert por (idVotacao, siglaBancada)")
@SuppressWarnings("null")
class SilverCamaraVotacoesOrientacoesLoaderTest {

    @Mock
    private SilverCamaraVotacaoOrientacaoRepository repository;

    @InjectMocks
    private SilverCamaraVotacoesOrientacoesLoader loader;

    private UUID jobId;

    @BeforeEach
    void setup() {
        jobId = UUID.randomUUID();
    }

    private CamaraVotacaoOrientacaoCSVRow orientacaoRow(String idVotacao, String siglaBancada) {
        CamaraVotacaoOrientacaoCSVRow row = new CamaraVotacaoOrientacaoCSVRow();
        row.setIdVotacao(idVotacao);
        row.setUriVotacao("https://camara.leg.br/votacoes/" + idVotacao);
        row.setSiglaOrgao("PLEN");
        row.setDescricao("Votação de teste");
        row.setSiglaBancada(siglaBancada);
        row.setUriBancada("https://camara.leg.br/bancadas/" + siglaBancada);
        row.setOrientacao("Sim");
        return row;
    }

    @Test
    @DisplayName("retorna 0 para lista nula")
    void retornaZeroParaListaNula() {
        assertThat(loader.carregar(null, jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("retorna 0 para lista vazia")
    void retornaZeroParaListaVazia() {
        assertThat(loader.carregar(Collections.emptyList(), jobId)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("ignora linhas com idVotacao nulo ou em branco")
    void ignoraLinhasComIdVotacaoInvalido() {
        CamaraVotacaoOrientacaoCSVRow row = orientacaoRow("  ", "PT");
        assertThat(loader.carregar(List.of(row), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacaoOrientacao.class));
    }

    @Test
    @DisplayName("ignora linhas com siglaBancada nulo ou em branco")
    void ignoraLinhasComSiglaBancadaInvalida() {
        CamaraVotacaoOrientacaoCSVRow row = orientacaoRow("VT-2024-001", null);
        assertThat(loader.carregar(List.of(row), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacaoOrientacao.class));
    }

    @Test
    @DisplayName("insere nova orientação quando não existe")
    void insereNovaOrientacao() {
        when(repository.findAllByIdVotacaoIn(any())).thenReturn(Collections.emptyList());

        CamaraVotacaoOrientacaoCSVRow row = orientacaoRow("VT-2024-001", "PT");
        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraVotacaoOrientacao> captor = ArgumentCaptor
                .forClass(SilverCamaraVotacaoOrientacao.class);
        verify(repository).save(captor.capture());

        SilverCamaraVotacaoOrientacao salvo = captor.getValue();
        assertThat(salvo.getIdVotacao()).isEqualTo("VT-2024-001");
        assertThat(salvo.getSiglaBancada()).isEqualTo("PT");
        assertThat(salvo.getOrientacao()).isEqualTo("Sim");
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("CSV");
    }

    @Test
    @DisplayName("ignora orientação já existente (idempotente)")
    void ignoraOrientacaoJaExistente() {
        SilverCamaraVotacaoOrientacao existente = SilverCamaraVotacaoOrientacao.builder()
                .idVotacao("VT-2024-001")
                .siglaBancada("PT")
                .build();
        when(repository.findAllByIdVotacaoIn(any())).thenReturn(List.of(existente));
        assertThat(loader.carregar(List.of(orientacaoRow("VT-2024-001", "PT")), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacaoOrientacao.class));
    }

    @Test
    @DisplayName("insere múltiplas orientações de bancadas distintas")
    void insereMultiplasOrientacoes() {
        when(repository.findAllByIdVotacaoIn(any())).thenReturn(Collections.emptyList());

        List<CamaraVotacaoOrientacaoCSVRow> rows = List.of(
                orientacaoRow("VT-2024-001", "PT"),
                orientacaoRow("VT-2024-001", "MDB"),
                orientacaoRow("VT-2024-001", "PL"));

        assertThat(loader.carregar(rows, jobId)).isEqualTo(3);
        verify(repository, times(3)).save(any(SilverCamaraVotacaoOrientacao.class));
    }
}
