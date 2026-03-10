package br.leg.congresso.etl.loader.silver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.metrics.EtlMetrics;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import br.leg.congresso.etl.transformer.silver.SilverCamaraDeputadoHashGenerator;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraDeputadoLoader — upsert por camara_id com hash de conteúdo")
class SilverCamaraDeputadoLoaderTest {

    @Mock
    private SilverCamaraDeputadoRepository repository;

    @Mock
    private SilverCamaraDeputadoHashGenerator hashGenerator;

    @Mock
    private EtlMetrics etlMetrics;

    @InjectMocks
    private SilverCamaraDeputadoLoader loader;

    private EtlJobControl jobControl;

    @BeforeEach
    void setup() {
        jobControl = EtlJobControl.builder()
                .id(UUID.randomUUID())
                .origem(CasaLegislativa.CAMARA)
                .tipoExecucao(TipoExecucao.FULL)
                .iniciadoEm(LocalDateTime.now())
                .build();
    }

    private SilverCamaraDeputado novoDeputado(String camaraId) {
        return SilverCamaraDeputado.builder()
                .camaraId(camaraId)
                .nomeCivil("João da Silva " + camaraId)
                .nomeParlamentar("Deputado " + camaraId)
                .sexo("M")
                .primeiraLegislatura("56")
                .ultimaLegislatura("57")
                .build();
    }

    // ── INSERT: registro novo ─────────────────────────────────────────────────

    @Test
    @DisplayName("INSERT: deputado novo (sem registro existente) é salvo com hash e goldSincronizado=false")
    void inserirDeputadoNovo() {
        SilverCamaraDeputado silver = novoDeputado("111");
        when(hashGenerator.generate(silver)).thenReturn("hashNovo001");
        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(Collections.emptyList());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(silver), jobControl);

        ArgumentCaptor<SilverCamaraDeputado> captor = ArgumentCaptor.forClass(SilverCamaraDeputado.class);
        verify(repository).save(captor.capture());

        SilverCamaraDeputado salvo = captor.getValue();
        assertThat(salvo.getContentHash()).isEqualTo("hashNovo001");
        assertThat(salvo.isGoldSincronizado()).isFalse();
        assertThat(salvo.getEtlJobId()).isEqualTo(jobControl.getId());

        verify(etlMetrics).registrarInseridos(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics, never()).registrarAtualizados(any(), anyInt());
        verify(etlMetrics, never()).registrarIgnorados(any(), anyInt());
    }

    // ── SKIP: hash igual ──────────────────────────────────────────────────────

    @Test
    @DisplayName("SKIP: deputado com mesmo hash não é salvo novamente")
    void ignorarDeputadoComHashIgual() {
        SilverCamaraDeputado silver = novoDeputado("222");
        SilverCamaraDeputado existente = novoDeputado("222");
        existente.setId(UUID.randomUUID());
        existente.setContentHash("hashExistente");
        existente.setGoldSincronizado(true);

        when(hashGenerator.generate(silver)).thenReturn("hashExistente");
        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(List.of(existente));

        loader.carregar(List.of(silver), jobControl);

        verify(repository, never()).save(any());
        verify(etlMetrics).registrarIgnorados(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics, never()).registrarInseridos(any(), anyInt());
        verify(etlMetrics, never()).registrarAtualizados(any(), anyInt());
    }

    // ── UPDATE: hash mudou ────────────────────────────────────────────────────

    @Test
    @DisplayName("UPDATE: deputado com hash diferente é atualizado com goldSincronizado=false")
    void atualizarDeputadoComHashDiferente() {
        SilverCamaraDeputado silver = novoDeputado("333");
        UUID idExistente = UUID.randomUUID();
        LocalDateTime ingeridoEm = LocalDateTime.now().minusDays(5);

        SilverCamaraDeputado existente = novoDeputado("333");
        existente.setId(idExistente);
        existente.setContentHash("hashAntigo");
        existente.setIngeridoEm(ingeridoEm);
        existente.setGoldSincronizado(true);

        when(hashGenerator.generate(silver)).thenReturn("hashNovo");
        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(List.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(silver), jobControl);

        ArgumentCaptor<SilverCamaraDeputado> captor = ArgumentCaptor.forClass(SilverCamaraDeputado.class);
        verify(repository).save(captor.capture());

        SilverCamaraDeputado salvo = captor.getValue();
        assertThat(salvo.getId()).isEqualTo(idExistente);
        assertThat(salvo.getIngeridoEm()).isEqualTo(ingeridoEm);
        assertThat(salvo.getContentHash()).isEqualTo("hashNovo");
        assertThat(salvo.isGoldSincronizado()).isFalse();

        verify(etlMetrics).registrarAtualizados(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics, never()).registrarInseridos(any(), anyInt());
        verify(etlMetrics, never()).registrarIgnorados(any(), anyInt());
    }

    // ── UPDATE: preserva campos det_* do enriquecimento ──────────────────────

    @Test
    @DisplayName("UPDATE: campos det_* existentes são preservados no update do CSV")
    void preservarCamposDetNoUpdate() {
        SilverCamaraDeputado silver = novoDeputado("444");
        UUID idExistente = UUID.randomUUID();

        SilverCamaraDeputado existente = novoDeputado("444");
        existente.setId(idExistente);
        existente.setContentHash("hashAntigo");
        existente.setDetStatusId("444");
        existente.setDetStatusSiglaPartido("PT");
        existente.setDetGabineteNome("Gabinete 10");

        when(hashGenerator.generate(silver)).thenReturn("hashNovo");
        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(List.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(silver), jobControl);

        ArgumentCaptor<SilverCamaraDeputado> captor = ArgumentCaptor.forClass(SilverCamaraDeputado.class);
        verify(repository).save(captor.capture());

        SilverCamaraDeputado salvo = captor.getValue();
        assertThat(salvo.getDetStatusId()).isEqualTo("444");
        assertThat(salvo.getDetStatusSiglaPartido()).isEqualTo("PT");
        assertThat(salvo.getDetGabineteNome()).isEqualTo("Gabinete 10");
    }

    // ── Lote misto: INSERT + SKIP + UPDATE ───────────────────────────────────

    @Test
    @DisplayName("Lote misto: INSERT + SKIP + UPDATE contabilizados corretamente")
    void loteMisto() {
        SilverCamaraDeputado novoD = novoDeputado("1");
        SilverCamaraDeputado skipD = novoDeputado("2");
        SilverCamaraDeputado updateD = novoDeputado("3");

        SilverCamaraDeputado existeSkip = novoDeputado("2");
        existeSkip.setId(UUID.randomUUID());
        existeSkip.setContentHash("hashSkip");

        SilverCamaraDeputado existeUpdate = novoDeputado("3");
        existeUpdate.setId(UUID.randomUUID());
        existeUpdate.setContentHash("hashAntigo3");

        when(hashGenerator.generate(novoD)).thenReturn("hashNovo1");
        when(hashGenerator.generate(skipD)).thenReturn("hashSkip");
        when(hashGenerator.generate(updateD)).thenReturn("hashNovo3");
        when(repository.findAllByCamaraIdIn(anyCollection()))
                .thenReturn(List.of(existeSkip, existeUpdate));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(novoD, skipD, updateD), jobControl);

        verify(repository, times(2)).save(any());
        verify(etlMetrics).registrarInseridos(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics).registrarAtualizados(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics).registrarIgnorados(CasaLegislativa.CAMARA, 1);

        assertThat(jobControl.getTotalInserido()).isEqualTo(1);
        assertThat(jobControl.getTotalAtualizado()).isEqualTo(1);
        assertThat(jobControl.getTotalIgnorados()).isEqualTo(1);
    }

    // ── Lista vazia: sem operação ─────────────────────────────────────────────

    @Test
    @DisplayName("Lista vazia: nenhuma interação com o repositório")
    void listaVazia() {
        loader.carregar(Collections.emptyList(), jobControl);
        verifyNoInteractions(repository, etlMetrics);
    }
}
