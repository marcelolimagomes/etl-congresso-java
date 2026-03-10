package br.leg.congresso.etl.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.extractor.camara.CamaraDeputadoApiExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraDeputadoCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraDeputadoFrenteCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraDeputadoOcupacaoCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraDeputadoOrgaoCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraDeputadoPresencaEventoCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraDeputadoProfissaoCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraDespesaCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraGrupoMembroCSVExtractor;
import br.leg.congresso.etl.extractor.camara.CamaraMesaDiretoraCSVExtractor;
import br.leg.congresso.etl.extractor.senado.SenadoSenadorApiExtractor;
import br.leg.congresso.etl.extractor.senado.SenadoSenadorSubrecursosApiExtractor;
import br.leg.congresso.etl.service.EtlJobControlService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParlamentaresOrchestrator — carga completa de parlamentares Câmara e Senado")
class ParlamentaresOrchestratorTest {

    // ── Câmara CSV mocks ──────────────────────────────────────────────
    @Mock
    private CamaraDeputadoCSVExtractor camaraDeputadoCSVExtractor;
    @Mock
    private CamaraDeputadoProfissaoCSVExtractor camaraDeputadoProfissaoCSVExtractor;
    @Mock
    private CamaraDeputadoOcupacaoCSVExtractor camaraDeputadoOcupacaoCSVExtractor;
    @Mock
    private CamaraDeputadoOrgaoCSVExtractor camaraDeputadoOrgaoCSVExtractor;
    @Mock
    private CamaraDeputadoFrenteCSVExtractor camaraDeputadoFrenteCSVExtractor;
    @Mock
    private CamaraDeputadoPresencaEventoCSVExtractor camaraDeputadoPresencaEventoCSVExtractor;
    @Mock
    private CamaraDespesaCSVExtractor camaraDespesaCSVExtractor;
    @Mock
    private CamaraMesaDiretoraCSVExtractor camaraMesaDiretoraCSVExtractor;
    @Mock
    private CamaraGrupoMembroCSVExtractor camaraGrupoMembroCSVExtractor;

    // ── Câmara API mock ───────────────────────────────────────────────
    @Mock
    private CamaraDeputadoApiExtractor camaraDeputadoApiExtractor;

    // ── Senado mocks ──────────────────────────────────────────────────
    @Mock
    private SenadoSenadorApiExtractor senadoSenadorApiExtractor;
    @Mock
    private SenadoSenadorSubrecursosApiExtractor senadoSenadorSubrecursosApiExtractor;

    @Mock
    private EtlJobControlService jobControlService;

    @InjectMocks
    private ParlamentaresOrchestrator orchestrator;

    private EtlJobControl job;

    @BeforeEach
    void setUp() {
        job = EtlJobControl.builder()
                .id(UUID.randomUUID())
                .origem(CasaLegislativa.CAMARA)
                .tipoExecucao(TipoExecucao.FULL)
                .build();
        ReflectionTestUtils.setField(orchestrator, "camaraAnoInicio", 2023);
    }

    // ── Câmara ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("executarCamara")
    class ExecutarCamara {

        @BeforeEach
        void setUpCamara() {
            when(jobControlService.iniciar(
                    eq(CasaLegislativa.CAMARA), eq(TipoExecucao.FULL), any(Map.class)))
                    .thenReturn(job);
        }

        @Test
        @DisplayName("happy path: todos os extractors CSV base são chamados")
        void happyPath_extratoresBaseCSV() throws IOException {
            when(jobControlService.finalizar(job)).thenReturn(job);
            when(camaraDeputadoCSVExtractor.extrairEPersistir(job)).thenReturn(100L);
            when(camaraDeputadoProfissaoCSVExtractor.extrairEPersistir(job)).thenReturn(50L);
            when(camaraDeputadoOcupacaoCSVExtractor.extrairEPersistir(job)).thenReturn(60L);
            when(camaraDeputadoOrgaoCSVExtractor.extrairEPersistir(job)).thenReturn(70L);
            when(camaraDeputadoFrenteCSVExtractor.extrairEPersistir(job)).thenReturn(40L);
            when(camaraMesaDiretoraCSVExtractor.extrairEPersistir(job)).thenReturn(10L);
            when(camaraGrupoMembroCSVExtractor.extrairEPersistir(job)).thenReturn(20L);
            when(camaraDeputadoPresencaEventoCSVExtractor.extrairEPersistir(anyInt(), eq(job))).thenReturn(200L);
            when(camaraDespesaCSVExtractor.extrairEPersistir(anyInt(), eq(job))).thenReturn(5000L);
            when(camaraDeputadoApiExtractor.extrairEPersistirTodos(job)).thenReturn(300);

            EtlJobControl resultado = orchestrator.executarCamara(2023, 2023);

            assertThat(resultado).isEqualTo(job);
            verify(camaraDeputadoCSVExtractor).extrairEPersistir(job);
            verify(camaraDeputadoProfissaoCSVExtractor).extrairEPersistir(job);
            verify(camaraDeputadoOcupacaoCSVExtractor).extrairEPersistir(job);
            verify(camaraDeputadoOrgaoCSVExtractor).extrairEPersistir(job);
            verify(camaraDeputadoFrenteCSVExtractor).extrairEPersistir(job);
            verify(camaraMesaDiretoraCSVExtractor).extrairEPersistir(job);
            verify(camaraGrupoMembroCSVExtractor).extrairEPersistir(job);
            verify(camaraDeputadoApiExtractor).extrairEPersistirTodos(job);
            verify(jobControlService).finalizar(job);
        }

        @Test
        @DisplayName("happy path: extratores por ano chamados para cada ano no intervalo")
        void happyPath_extratoresPorAno() throws IOException {
            when(jobControlService.finalizar(job)).thenReturn(job);
            when(camaraDeputadoCSVExtractor.extrairEPersistir(job)).thenReturn(100L);
            when(camaraDeputadoProfissaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoOcupacaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoOrgaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoFrenteCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraMesaDiretoraCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraGrupoMembroCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoPresencaEventoCSVExtractor.extrairEPersistir(anyInt(), eq(job))).thenReturn(0L);
            when(camaraDespesaCSVExtractor.extrairEPersistir(anyInt(), eq(job))).thenReturn(0L);
            when(camaraDeputadoApiExtractor.extrairEPersistirTodos(job)).thenReturn(0);

            orchestrator.executarCamara(2021, 2023);

            // 3 anos → 3 chamadas a cada extrator por ano
            verify(camaraDeputadoPresencaEventoCSVExtractor, times(3)).extrairEPersistir(anyInt(), eq(job));
            verify(camaraDespesaCSVExtractor, times(3)).extrairEPersistir(anyInt(), eq(job));
            verify(camaraDeputadoPresencaEventoCSVExtractor).extrairEPersistir(eq(2021), eq(job));
            verify(camaraDeputadoPresencaEventoCSVExtractor).extrairEPersistir(eq(2022), eq(job));
            verify(camaraDeputadoPresencaEventoCSVExtractor).extrairEPersistir(eq(2023), eq(job));
        }

        @Test
        @DisplayName("IOException em presença de evento é resiliente: registra erro e continua")
        void ioExceptionPresenca_resiliente() throws IOException {
            when(jobControlService.finalizar(job)).thenReturn(job);
            when(camaraDeputadoCSVExtractor.extrairEPersistir(job)).thenReturn(100L);
            when(camaraDeputadoProfissaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoOcupacaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoOrgaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoFrenteCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraMesaDiretoraCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraGrupoMembroCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoPresencaEventoCSVExtractor.extrairEPersistir(eq(2023), eq(job)))
                    .thenThrow(new IOException("arquivo não encontrado"));
            when(camaraDespesaCSVExtractor.extrairEPersistir(eq(2023), eq(job))).thenReturn(1000L);
            when(camaraDeputadoApiExtractor.extrairEPersistirTodos(job)).thenReturn(0);

            // NÃO deve lançar exceção — erro por ano é resiliente
            EtlJobControl resultado = orchestrator.executarCamara(2023, 2023);

            assertThat(resultado).isEqualTo(job);
            verify(jobControlService).registrarErro(
                    eq(job), eq(CasaLegislativa.CAMARA),
                    eq("PRESENCA_ERROR"), eq("PRESENCA-2023"), any(String.class));
            verify(camaraDespesaCSVExtractor).extrairEPersistir(eq(2023), eq(job));
            verify(jobControlService).finalizar(job);
            verify(jobControlService, never()).falhar(any(), any());
        }

        @Test
        @DisplayName("IOException em despesas CEAP é resiliente: registra erro e continua")
        void ioExceptionDespesa_resiliente() throws IOException {
            when(jobControlService.finalizar(job)).thenReturn(job);
            when(camaraDeputadoCSVExtractor.extrairEPersistir(job)).thenReturn(100L);
            when(camaraDeputadoProfissaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoOcupacaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoOrgaoCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoFrenteCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraMesaDiretoraCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraGrupoMembroCSVExtractor.extrairEPersistir(job)).thenReturn(0L);
            when(camaraDeputadoPresencaEventoCSVExtractor.extrairEPersistir(anyInt(), eq(job))).thenReturn(200L);
            when(camaraDespesaCSVExtractor.extrairEPersistir(eq(2023), eq(job)))
                    .thenThrow(new IOException("ZIP inválido"));
            when(camaraDeputadoApiExtractor.extrairEPersistirTodos(job)).thenReturn(0);

            EtlJobControl resultado = orchestrator.executarCamara(2023, 2023);

            assertThat(resultado).isEqualTo(job);
            verify(jobControlService).registrarErro(
                    eq(job), eq(CasaLegislativa.CAMARA),
                    eq("DESPESA_ERROR"), eq("DESPESA-2023"), any(String.class));
            verify(jobControlService).finalizar(job);
            verify(jobControlService, never()).falhar(any(), any());
        }

        @Test
        @DisplayName("IOException no extrator base falha o job e relança como RuntimeException")
        void ioExceptionBase_falhaJob() throws IOException {
            when(camaraDeputadoCSVExtractor.extrairEPersistir(job))
                    .thenThrow(new IOException("CSV base corrompido"));

            assertThatThrownBy(() -> orchestrator.executarCamara(2023, 2023))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Falha na carga parlamentares Câmara");

            verify(jobControlService).falhar(eq(job), any(String.class));
            verify(jobControlService, never()).finalizar(any());
        }
    }

    // ── Senado ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("executarSenado")
    class ExecutarSenado {

        private EtlJobControl jobSenado;

        @BeforeEach
        void setUpSenado() {
            jobSenado = EtlJobControl.builder()
                    .id(UUID.randomUUID())
                    .origem(CasaLegislativa.SENADO)
                    .tipoExecucao(TipoExecucao.FULL)
                    .build();
            when(jobControlService.iniciar(
                    eq(CasaLegislativa.SENADO), eq(TipoExecucao.FULL), any(Map.class)))
                    .thenReturn(jobSenado);
        }

        @Test
        @DisplayName("happy path: extractor base e sub-recursos são chamados em sequência")
        void happyPath_extratoresChamados() {
            when(jobControlService.finalizar(jobSenado)).thenReturn(jobSenado);
            when(senadoSenadorApiExtractor.extrairEPersistirTodos(jobSenado)).thenReturn(81);
            when(senadoSenadorSubrecursosApiExtractor.extrairEPersistirTodos(jobSenado)).thenReturn(500);

            EtlJobControl resultado = orchestrator.executarSenado();

            assertThat(resultado).isEqualTo(jobSenado);
            verify(senadoSenadorApiExtractor).extrairEPersistirTodos(jobSenado);
            verify(senadoSenadorSubrecursosApiExtractor).extrairEPersistirTodos(jobSenado);
            verify(jobControlService).finalizar(jobSenado);
            verify(jobControlService, never()).falhar(any(), any());
        }

        @Test
        @DisplayName("sub-recursos não são chamados se extrator base lançar exceção")
        void excecaoBase_subRecursosNaoSaoChamados() {
            when(senadoSenadorApiExtractor.extrairEPersistirTodos(jobSenado))
                    .thenThrow(new RuntimeException("API Senado indisponível"));

            assertThatThrownBy(() -> orchestrator.executarSenado())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("API Senado indisponível");

            verify(senadoSenadorSubrecursosApiExtractor, never()).extrairEPersistirTodos(any());
            verify(jobControlService).falhar(eq(jobSenado), any(String.class));
            verify(jobControlService, never()).finalizar(any());
        }

        @Test
        @DisplayName("exceção em sub-recursos falha o job e relança")
        void excecaoSubRecursos_falhaJob() {
            when(senadoSenadorApiExtractor.extrairEPersistirTodos(jobSenado)).thenReturn(81);
            when(senadoSenadorSubrecursosApiExtractor.extrairEPersistirTodos(jobSenado))
                    .thenThrow(new RuntimeException("Timeout ao buscar sub-recursos"));

            assertThatThrownBy(() -> orchestrator.executarSenado())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Timeout ao buscar sub-recursos");

            verify(jobControlService).falhar(eq(jobSenado), any(String.class));
            verify(jobControlService, never()).finalizar(any());
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────

    @Test
    @DisplayName("getCamaraAnoInicio retorna valor injetado via @Value")
    void getCamaraAnoInicio() {
        assertThat(orchestrator.getCamaraAnoInicio()).isEqualTo(2023);
    }

    @Test
    @DisplayName("getAnoAtual retorna o ano corrente")
    void getAnoAtual() {
        assertThat(orchestrator.getAnoAtual())
                .isEqualTo(java.time.LocalDate.now().getYear());
    }
}
