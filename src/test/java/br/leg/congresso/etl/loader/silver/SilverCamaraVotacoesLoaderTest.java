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

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraVotacaoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraVotacoesLoader — upsert por votacaoId")
@SuppressWarnings("null")
class SilverCamaraVotacoesLoaderTest {

    @Mock
    private SilverCamaraVotacaoRepository repository;

    @InjectMocks
    private SilverCamaraVotacoesLoader loader;

    private UUID jobId;

    @BeforeEach
    void setup() {
        jobId = UUID.randomUUID();
    }

    private CamaraVotacaoCSVRow votacaoRow(String id) {
        CamaraVotacaoCSVRow row = new CamaraVotacaoCSVRow();
        row.setId(id);
        row.setUri("https://camara.leg.br/votacoes/" + id);
        row.setData("2024-05-15");
        row.setDataHoraRegistro("2024-05-15T14:30:00");
        row.setIdOrgao("180");
        row.setUriOrgao("https://camara.leg.br/orgaos/180");
        row.setSiglaOrgao("PLEN");
        row.setIdEvento("0");
        row.setUriEvento("");
        row.setAprovacao("1");
        row.setVotosSim("257");
        row.setVotosNao("122");
        row.setVotosOutros("10");
        row.setDescricao("Votação de teste");
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
    @DisplayName("ignora linhas com id nulo ou em branco")
    void ignoraLinhasComIdNulo() {
        CamaraVotacaoCSVRow row = new CamaraVotacaoCSVRow();
        row.setId(null);

        assertThat(loader.carregar(List.of(row), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacao.class));
    }

    @Test
    @DisplayName("insere nova votação quando não existe")
    void insereNovaVotacao() {
        when(repository.findAllByVotacaoIdIn(any())).thenReturn(Collections.emptyList());

        CamaraVotacaoCSVRow row = votacaoRow("VT-2024-001");
        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraVotacao> captor = ArgumentCaptor.forClass(SilverCamaraVotacao.class);
        verify(repository).save(captor.capture());

        SilverCamaraVotacao salvo = captor.getValue();
        assertThat(salvo.getVotacaoId()).isEqualTo("VT-2024-001");
        assertThat(salvo.getSiglaOrgao()).isEqualTo("PLEN");
        assertThat(salvo.getVotosSim()).isEqualTo(257);
        assertThat(salvo.getVotosNao()).isEqualTo(122);
        assertThat(salvo.getAprovacao()).isEqualTo((short) 1);
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("CSV");
    }

    @Test
    @DisplayName("ignora votação já existente (idempotente)")
    void ignoraVotacaoJaExistente() {
        SilverCamaraVotacao existente = SilverCamaraVotacao.builder().votacaoId("VT-2024-001").build();
        when(repository.findAllByVotacaoIdIn(any())).thenReturn(List.of(existente));

        assertThat(loader.carregar(List.of(votacaoRow("VT-2024-001")), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacao.class));
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura de novos e existentes")
    void insereSomenteNovos() {
        SilverCamaraVotacao existente = SilverCamaraVotacao.builder().votacaoId("VT-2024-001").build();
        when(repository.findAllByVotacaoIdIn(any())).thenReturn(List.of(existente));

        int resultado = loader.carregar(
                List.of(votacaoRow("VT-2024-001"), votacaoRow("VT-2024-002")), jobId);

        assertThat(resultado).isEqualTo(1);
        verify(repository, times(1)).save(any(SilverCamaraVotacao.class));
    }

    @Test
    @DisplayName("mapeia data no formato ISO (YYYY-MM-DD)")
    void mapeiaData() {
        when(repository.findAllByVotacaoIdIn(any())).thenReturn(Collections.emptyList());

        CamaraVotacaoCSVRow row = votacaoRow("VT-2024-001");
        row.setData("2024-03-20");
        loader.carregar(List.of(row), jobId);

        ArgumentCaptor<SilverCamaraVotacao> captor = ArgumentCaptor.forClass(SilverCamaraVotacao.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getData()).hasYear(2024).hasMonthValue(3).hasDayOfMonth(20);
    }

    @Test
    @DisplayName("trata data inválida como null")
    void trataDataInvalida() {
        when(repository.findAllByVotacaoIdIn(any())).thenReturn(Collections.emptyList());

        CamaraVotacaoCSVRow row = votacaoRow("VT-2024-001");
        row.setData("invalido");
        loader.carregar(List.of(row), jobId);

        ArgumentCaptor<SilverCamaraVotacao> captor = ArgumentCaptor.forClass(SilverCamaraVotacao.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getData()).isNull();
    }
}
