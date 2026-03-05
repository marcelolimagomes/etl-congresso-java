package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.metrics.EtlMetrics;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.transformer.silver.SilverCamaraHashGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SilverCamaraLoader — upsert por camara_id com hash de conteúdo")
class SilverCamaraLoaderTest {

    @Mock
    private SilverCamaraProposicaoRepository repository;

    @Mock
    private SilverCamaraHashGenerator hashGenerator;

    @Mock
    private EtlMetrics etlMetrics;

    @InjectMocks
    private SilverCamaraLoader loader;

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

    private SilverCamaraProposicao novaProposicao(String camaraId) {
        return SilverCamaraProposicao.builder()
            .camaraId(camaraId)
            .siglaTipo("PL")
            .numero(100)
            .ano(2024)
            .ementa("Ementa " + camaraId)
            .build();
    }

    // ── INSERT: registro novo ─────────────────────────────────────────────────

    @Test
    @DisplayName("INSERT: proposição nova (sem registro existente) é salva com hash e goldSincronizado=false")
    void inserirProposicaoNova() {
        SilverCamaraProposicao silver = novaProposicao("111");
        when(hashGenerator.generate(silver)).thenReturn("hashNovo001");
        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(Collections.emptyList());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(silver), jobControl);

        ArgumentCaptor<SilverCamaraProposicao> captor = ArgumentCaptor.forClass(SilverCamaraProposicao.class);
        verify(repository).save(captor.capture());

        SilverCamaraProposicao salvo = captor.getValue();
        assertThat(salvo.getContentHash()).isEqualTo("hashNovo001");
        assertThat(salvo.isGoldSincronizado()).isFalse();
        assertThat(salvo.getEtlJobId()).isEqualTo(jobControl.getId());

        verify(etlMetrics).registrarInseridos(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics, never()).registrarAtualizados(any(), anyInt());
        verify(etlMetrics, never()).registrarIgnorados(any(), anyInt());
    }

    // ── SKIP: hash igual ──────────────────────────────────────────────────────

    @Test
    @DisplayName("SKIP: proposição com mesmo hash não é salva novamente")
    void ignorarProposicaoComHashIgual() {
        SilverCamaraProposicao silver = novaProposicao("222");
        SilverCamaraProposicao existente = novaProposicao("222");
        existente.setId(UUID.randomUUID());
        existente.setContentHash("hashExistente");
        existente.setGoldSincronizado(true);

        when(hashGenerator.generate(silver)).thenReturn("hashExistente"); // mesmo hash
        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(List.of(existente));

        loader.carregar(List.of(silver), jobControl);

        verify(repository, never()).save(any());
        verify(etlMetrics).registrarIgnorados(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics, never()).registrarInseridos(any(), anyInt());
        verify(etlMetrics, never()).registrarAtualizados(any(), anyInt());
    }

    // ── UPDATE: hash mudou ────────────────────────────────────────────────────

    @Test
    @DisplayName("UPDATE: proposição com hash diferente é atualizada com goldSincronizado=false")
    void atualizarProposicaoComHashDiferente() {
        SilverCamaraProposicao silver = novaProposicao("333");
        UUID idExistente = UUID.randomUUID();
        LocalDateTime ingeridoEm = LocalDateTime.now().minusDays(5);

        SilverCamaraProposicao existente = novaProposicao("333");
        existente.setId(idExistente);
        existente.setContentHash("hashAntigo");
        existente.setIngeridoEm(ingeridoEm);
        existente.setGoldSincronizado(true);

        when(hashGenerator.generate(silver)).thenReturn("hashNovo"); // hash mudou
        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(List.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(silver), jobControl);

        ArgumentCaptor<SilverCamaraProposicao> captor = ArgumentCaptor.forClass(SilverCamaraProposicao.class);
        verify(repository).save(captor.capture());

        SilverCamaraProposicao salvo = captor.getValue();
        assertThat(salvo.getId()).isEqualTo(idExistente);               // preservou o ID
        assertThat(salvo.getIngeridoEm()).isEqualTo(ingeridoEm);         // preservou data de ingestão
        assertThat(salvo.getContentHash()).isEqualTo("hashNovo");         // atualizou hash
        assertThat(salvo.isGoldSincronizado()).isFalse();                 // marcou para re-promoção

        verify(etlMetrics).registrarAtualizados(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics, never()).registrarInseridos(any(), anyInt());
        verify(etlMetrics, never()).registrarIgnorados(any(), anyInt());
    }

    // ── Contador de erros ─────────────────────────────────────────────────────

    @Test
    @DisplayName("erro em save incrementa contador de erros no jobControl")
    void erroEmSaveIncrementaContadorDeErros() {
        SilverCamaraProposicao silver = novaProposicao("444");
        when(hashGenerator.generate(silver)).thenReturn("hashQualquer");
        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(Collections.emptyList());
        when(repository.save(any())).thenThrow(new RuntimeException("Erro simulado de BD"));

        loader.carregar(List.of(silver), jobControl);

        assertThat(jobControl.getTotalErros()).isEqualTo(1);
        // métricas de inserção não são registradas em caso de erro
        verify(etlMetrics, never()).registrarInseridos(any(), anyInt());
    }

    // ── Lista vazia ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("lista vazia não realiza nenhuma operação no repositório")
    void listaVaziaNaoFazNada() {
        loader.carregar(List.of(), jobControl);

        verifyNoInteractions(repository, hashGenerator, etlMetrics);
    }

    // ── Lote misto ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("lote misto: INSERT + SKIP + UPDATE são contabilizados corretamente")
    void loteMistoContabilizaCorretamente() {
        SilverCamaraProposicao novaP   = novaProposicao("501");
        SilverCamaraProposicao skipP   = novaProposicao("502");
        SilverCamaraProposicao updateP = novaProposicao("503");

        when(hashGenerator.generate(novaP)).thenReturn("hashNova");
        when(hashGenerator.generate(skipP)).thenReturn("hashSkip");
        when(hashGenerator.generate(updateP)).thenReturn("hashNovo503");

        SilverCamaraProposicao existenteSkip = novaProposicao("502");
        existenteSkip.setId(UUID.randomUUID());
        existenteSkip.setContentHash("hashSkip"); // igual

        SilverCamaraProposicao existenteUpdate = novaProposicao("503");
        existenteUpdate.setId(UUID.randomUUID());
        existenteUpdate.setContentHash("hashAntigo503"); // diferente
        existenteUpdate.setIngeridoEm(LocalDateTime.now().minusDays(1));

        when(repository.findAllByCamaraIdIn(anyCollection())).thenReturn(List.of(existenteSkip, existenteUpdate));

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader.carregar(List.of(novaP, skipP, updateP), jobControl);

        // 2 saves (INSERT + UPDATE), nenhum para o SKIP
        verify(repository, times(2)).save(any());

        // Métricas
        verify(etlMetrics).registrarInseridos(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics).registrarAtualizados(CasaLegislativa.CAMARA, 1);
        verify(etlMetrics).registrarIgnorados(CasaLegislativa.CAMARA, 1);

        // Contadores do job
        assertThat(jobControl.getTotalInserido()).isEqualTo(1);
        assertThat(jobControl.getTotalAtualizado()).isEqualTo(1);
        assertThat(jobControl.getTotalIgnorados()).isEqualTo(1);
    }
}
