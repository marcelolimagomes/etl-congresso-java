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

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacaoVoto;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoVotoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraVotacaoVotoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraVotacoesVotosLoader — upsert por (idVotacao, deputadoId)")
@SuppressWarnings("null")
class SilverCamaraVotacoesVotosLoaderTest {

    @Mock
    private SilverCamaraVotacaoVotoRepository repository;

    @InjectMocks
    private SilverCamaraVotacoesVotosLoader loader;

    private UUID jobId;

    @BeforeEach
    void setup() {
        jobId = UUID.randomUUID();
    }

    private CamaraVotacaoVotoCSVRow votoRow(String idVotacao, String deputadoId) {
        CamaraVotacaoVotoCSVRow row = new CamaraVotacaoVotoCSVRow();
        row.setIdVotacao(idVotacao);
        row.setUriVotacao("https://camara.leg.br/votacoes/" + idVotacao);
        row.setDataHoraVoto("2024-05-15T14:32:00");
        row.setVoto("Sim");
        row.setDeputadoId(deputadoId);
        row.setDeputadoUri("https://camara.leg.br/deputados/" + deputadoId);
        row.setDeputadoNome("Deputado Teste");
        row.setDeputadoSiglaPartido("PT");
        row.setDeputadoUriPartido("https://camara.leg.br/partidos/10");
        row.setDeputadoSiglaUf("SP");
        row.setDeputadoIdLegislatura("57");
        row.setDeputadoUrlFoto("https://camara.leg.br/foto/dep" + deputadoId + ".jpg");
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
    @DisplayName("ignora linhas com idVotacao nulo")
    void ignoraLinhasComIdVotacaoNulo() {
        CamaraVotacaoVotoCSVRow row = votoRow(null, "12345");
        assertThat(loader.carregar(List.of(row), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacaoVoto.class));
    }

    @Test
    @DisplayName("ignora linhas com deputadoId nulo")
    void ignoraLinhasComDeputadoIdNulo() {
        CamaraVotacaoVotoCSVRow row = votoRow("VT-2024-001", null);
        assertThat(loader.carregar(List.of(row), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacaoVoto.class));
    }

    @Test
    @DisplayName("ignora linhas com deputadoId não numérico")
    void ignoraLinhasComDeputadoIdNaoNumerico() {
        CamaraVotacaoVotoCSVRow row = votoRow("VT-2024-001", "NAO-NUMERICO");
        assertThat(loader.carregar(List.of(row), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacaoVoto.class));
    }

    @Test
    @DisplayName("insere novo voto quando não existe")
    void insereNovoVoto() {
        when(repository.findAllByIdVotacaoIn(any())).thenReturn(Collections.emptyList());

        CamaraVotacaoVotoCSVRow row = votoRow("VT-2024-001", "12345");
        int resultado = loader.carregar(List.of(row), jobId);

        assertThat(resultado).isEqualTo(1);
        ArgumentCaptor<SilverCamaraVotacaoVoto> captor = ArgumentCaptor.forClass(SilverCamaraVotacaoVoto.class);
        verify(repository).save(captor.capture());

        SilverCamaraVotacaoVoto salvo = captor.getValue();
        assertThat(salvo.getIdVotacao()).isEqualTo("VT-2024-001");
        assertThat(salvo.getDeputadoId()).isEqualTo(12345);
        assertThat(salvo.getVoto()).isEqualTo("Sim");
        assertThat(salvo.getDeputadoSiglaPartido()).isEqualTo("PT");
        assertThat(salvo.getDeputadoSiglaUf()).isEqualTo("SP");
        assertThat(salvo.getDeputadoIdLegislatura()).isEqualTo(57);
        assertThat(salvo.getEtlJobId()).isEqualTo(jobId);
        assertThat(salvo.getOrigemCarga()).isEqualTo("CSV");
    }

    @Test
    @DisplayName("ignora voto já existente (idempotente)")
    void ignoraVotoJaExistente() {
        SilverCamaraVotacaoVoto existente = SilverCamaraVotacaoVoto.builder()
                .idVotacao("VT-2024-001")
                .deputadoId(12345)
                .build();
        when(repository.findAllByIdVotacaoIn(any())).thenReturn(List.of(existente));
        assertThat(loader.carregar(List.of(votoRow("VT-2024-001", "12345")), jobId)).isZero();
        verify(repository, never()).save(any(SilverCamaraVotacaoVoto.class));
    }

    @Test
    @DisplayName("insere somente os novos quando há mistura")
    void insereSomenteNovos() {
        SilverCamaraVotacaoVoto existente = SilverCamaraVotacaoVoto.builder()
                .idVotacao("VT-2024-001")
                .deputadoId(100)
                .build();
        when(repository.findAllByIdVotacaoIn(any())).thenReturn(List.of(existente));

        List<CamaraVotacaoVotoCSVRow> rows = List.of(
                votoRow("VT-2024-001", "100"),
                votoRow("VT-2024-001", "200"));

        assertThat(loader.carregar(rows, jobId)).isEqualTo(1);
        verify(repository, times(1)).save(any(SilverCamaraVotacaoVoto.class));
    }
}
