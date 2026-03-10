package br.leg.congresso.etl.orchestrator;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquestrador de carga completa de dados de parlamentares (deputados e
 * senadores).
 * Coordena a extração de CSVs (Câmara) e endpoints de API (Câmara complementar
 * e Senado).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParlamentaresOrchestrator {

    // ── Câmara — extractors CSV ───────────────────────────────────────
    private final CamaraDeputadoCSVExtractor camaraDeputadoCSVExtractor;
    private final CamaraDeputadoProfissaoCSVExtractor camaraDeputadoProfissaoCSVExtractor;
    private final CamaraDeputadoOcupacaoCSVExtractor camaraDeputadoOcupacaoCSVExtractor;
    private final CamaraDeputadoOrgaoCSVExtractor camaraDeputadoOrgaoCSVExtractor;
    private final CamaraDeputadoFrenteCSVExtractor camaraDeputadoFrenteCSVExtractor;
    private final CamaraDeputadoPresencaEventoCSVExtractor camaraDeputadoPresencaEventoCSVExtractor;
    private final CamaraDespesaCSVExtractor camaraDespesaCSVExtractor;
    private final CamaraMesaDiretoraCSVExtractor camaraMesaDiretoraCSVExtractor;
    private final CamaraGrupoMembroCSVExtractor camaraGrupoMembroCSVExtractor;

    // ── Câmara — extractor API-only ───────────────────────────────────
    private final CamaraDeputadoApiExtractor camaraDeputadoApiExtractor;

    // ── Senado — extractors API ───────────────────────────────────────
    private final SenadoSenadorApiExtractor senadoSenadorApiExtractor;
    private final SenadoSenadorSubrecursosApiExtractor senadoSenadorSubrecursosApiExtractor;

    private final EtlJobControlService jobControlService;

    @Value("${etl.camara.ano-inicio:1988}")
    private int camaraAnoInicio;

    // ── Câmara ────────────────────────────────────────────────────────

    /**
     * Executa a carga completa de parlamentares da Câmara dos Deputados.
     * <ol>
     * <li>Base de deputados via CSV.</li>
     * <li>Sub-tabelas CSV (profissão, ocupação, órgão, frente, mesa, grupo).</li>
     * <li>Presenças em eventos e despesas CEAP, iterando por ano.</li>
     * <li>Endpoints API-only (discursos, eventos, histórico, mandatos
     * externos).</li>
     * </ol>
     *
     * @param anoInicio primeiro ano do intervalo de presenças e despesas
     * @param anoFim    último ano do intervalo de presenças e despesas
     * @return {@link EtlJobControl} do job finalizado
     */
    public EtlJobControl executarCamara(int anoInicio, int anoFim) {
        log.info("Iniciando carga parlamentares Câmara: {} a {}", anoInicio, anoFim);

        EtlJobControl job = jobControlService.iniciar(
                CasaLegislativa.CAMARA,
                TipoExecucao.FULL,
                Map.of("anoInicio", anoInicio, "anoFim", anoFim, "tipo", "parlamentares"));

        try {
            // 1. Base de deputados
            long deputados = camaraDeputadoCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara parlamentares: {} deputados carregados", deputados);

            // 2. Sub-tabelas CSV (sem corte por ano — arquivos únicos)
            long profissoes = camaraDeputadoProfissaoCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara parlamentares: {} profissões carregadas", profissoes);

            long ocupacoes = camaraDeputadoOcupacaoCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara parlamentares: {} ocupações carregadas", ocupacoes);

            long orgaos = camaraDeputadoOrgaoCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara parlamentares: {} órgãos carregados", orgaos);

            long frentes = camaraDeputadoFrenteCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara parlamentares: {} frentes parlamentares carregadas", frentes);

            long mesa = camaraMesaDiretoraCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara parlamentares: {} membros de mesa diretora carregados", mesa);

            long grupos = camaraGrupoMembroCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara parlamentares: {} membros de grupos carregados", grupos);

            // 3. Presenças em eventos e despesas CEAP, iterando por ano
            for (int ano = anoInicio; ano <= anoFim; ano++) {
                try {
                    long presencas = camaraDeputadoPresencaEventoCSVExtractor.extrairEPersistir(ano, job);
                    log.info("[Silver] Câmara parlamentares: {} presenças carregadas para ano={}", presencas, ano);
                } catch (IOException e) {
                    log.error("Erro ao extrair presenças Câmara ano={}: {}", ano, e.getMessage());
                    jobControlService.registrarErro(job, CasaLegislativa.CAMARA,
                            "PRESENCA_ERROR", "PRESENCA-" + ano, e.getMessage());
                    job.incrementarErros();
                }
                try {
                    long despesas = camaraDespesaCSVExtractor.extrairEPersistir(ano, job);
                    log.info("[Silver] Câmara parlamentares: {} despesas carregadas para ano={}", despesas, ano);
                } catch (IOException e) {
                    log.error("Erro ao extrair despesas CEAP Câmara ano={}: {}", ano, e.getMessage());
                    jobControlService.registrarErro(job, CasaLegislativa.CAMARA,
                            "DESPESA_ERROR", "DESPESA-" + ano, e.getMessage());
                    job.incrementarErros();
                }
            }

            // 4. Endpoints API-only: discursos, eventos, histórico, mandatos externos
            int apiTotal = camaraDeputadoApiExtractor.extrairEPersistirTodos(job);
            log.info("[Silver] Câmara parlamentares: {} registros API-only carregados", apiTotal);

            return jobControlService.finalizar(job);

        } catch (RuntimeException e) {
            log.error("Falha crítica na carga parlamentares Câmara: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Falha crítica na carga parlamentares Câmara: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw new RuntimeException("Falha na carga parlamentares Câmara: " + e.getMessage(), e);
        }
    }

    // ── Senado ────────────────────────────────────────────────────────

    /**
     * Executa a carga completa de parlamentares do Senado Federal.
     * <ol>
     * <li>Lista atual e histórica de senadores + afastados, partidos e tipos de uso
     * da palavra.</li>
     * <li>Sub-recursos por senador: profissão, mandatos, licenças, histórico
     * acadêmico,
     * filiações, discursos, comissões, cargos e apartes.</li>
     * </ol>
     *
     * @return {@link EtlJobControl} do job finalizado
     */
    public EtlJobControl executarSenado() {
        log.info("Iniciando carga parlamentares Senado");

        EtlJobControl job = jobControlService.iniciar(
                CasaLegislativa.SENADO,
                TipoExecucao.FULL,
                Map.of("tipo", "parlamentares"));

        try {
            // 1. Base de senadores + referências (afastados, partidos, tipos de uso da
            // palavra)
            int senadores = senadoSenadorApiExtractor.extrairEPersistirTodos(job);
            log.info("[Silver] Senado parlamentares: {} registros base carregados", senadores);

            // 2. Sub-recursos por senador
            int subRecursos = senadoSenadorSubrecursosApiExtractor.extrairEPersistirTodos(job);
            log.info("[Silver] Senado parlamentares: {} sub-recursos carregados", subRecursos);

            return jobControlService.finalizar(job);

        } catch (RuntimeException e) {
            log.error("Falha crítica na carga parlamentares Senado: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Falha crítica na carga parlamentares Senado: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw new RuntimeException("Falha na carga parlamentares Senado: " + e.getMessage(), e);
        }
    }

    /**
     * Executa apenas as etapas CSV da carga de parlamentares da Câmara (etapas
     * 1-3),
     * sem chamar os endpoints API de sub-recursos (etapa 4).
     * Útil para complementar presenças e despesas CEAP de anos ainda não
     * carregados.
     *
     * @param anoInicio primeiro ano do intervalo de presenças e despesas
     * @param anoFim    último ano do intervalo de presenças e despesas
     * @return {@link EtlJobControl} do job finalizado
     */
    public EtlJobControl executarCamaraCSV(int anoInicio, int anoFim) {
        log.info("Iniciando carga CSV parlamentares Câmara: {} a {}", anoInicio, anoFim);

        EtlJobControl job = jobControlService.iniciar(
                CasaLegislativa.CAMARA,
                TipoExecucao.FULL,
                Map.of("anoInicio", anoInicio, "anoFim", anoFim, "tipo", "parlamentares-csv"));

        try {
            // 1. Base de deputados
            long deputados = camaraDeputadoCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara CSV: {} deputados carregados", deputados);

            // 2. Sub-tabelas CSV (sem corte por ano — arquivos únicos)
            long profissoes = camaraDeputadoProfissaoCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara CSV: {} profissões carregadas", profissoes);

            long ocupacoes = camaraDeputadoOcupacaoCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara CSV: {} ocupações carregadas", ocupacoes);

            long orgaos = camaraDeputadoOrgaoCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara CSV: {} órgãos carregados", orgaos);

            long frentes = camaraDeputadoFrenteCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara CSV: {} frentes parlamentares carregadas", frentes);

            long mesa = camaraMesaDiretoraCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara CSV: {} membros de mesa diretora carregados", mesa);

            long grupos = camaraGrupoMembroCSVExtractor.extrairEPersistir(job);
            log.info("[Silver] Câmara CSV: {} membros de grupos carregados", grupos);

            // 3. Presenças em eventos e despesas CEAP, iterando por ano
            for (int ano = anoInicio; ano <= anoFim; ano++) {
                try {
                    long presencas = camaraDeputadoPresencaEventoCSVExtractor.extrairEPersistir(ano, job);
                    log.info("[Silver] Câmara CSV: {} presenças carregadas para ano={}", presencas, ano);
                } catch (IOException e) {
                    log.error("Erro ao extrair presenças Câmara ano={}: {}", ano, e.getMessage());
                    jobControlService.registrarErro(job, CasaLegislativa.CAMARA,
                            "PRESENCA_ERROR", "PRESENCA-" + ano, e.getMessage());
                    job.incrementarErros();
                }
                try {
                    long despesas = camaraDespesaCSVExtractor.extrairEPersistir(ano, job);
                    log.info("[Silver] Câmara CSV: {} despesas carregadas para ano={}", despesas, ano);
                } catch (IOException e) {
                    log.error("Erro ao extrair despesas CEAP Câmara ano={}: {}", ano, e.getMessage());
                    jobControlService.registrarErro(job, CasaLegislativa.CAMARA,
                            "DESPESA_ERROR", "DESPESA-" + ano, e.getMessage());
                    job.incrementarErros();
                }
            }

            return jobControlService.finalizar(job);

        } catch (RuntimeException e) {
            log.error("Falha crítica na carga CSV parlamentares Câmara: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Falha crítica na carga CSV parlamentares Câmara: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw new RuntimeException("Falha na carga CSV parlamentares Câmara: " + e.getMessage(), e);
        }
    }

    /**
     * Executa apenas os sub-recursos API de deputados da Câmara (etapa 4):
     * discursos, eventos, histórico e mandatos externos.
     * Seguro de re-executar: cada loader verifica duplicidade antes de inserir.
     *
     * @return {@link EtlJobControl} do job finalizado
     */
    public EtlJobControl executarCamaraSubRecursos() {
        log.info("Iniciando carga sub-recursos API de deputados Câmara");

        EtlJobControl job = jobControlService.iniciar(
                CasaLegislativa.CAMARA,
                TipoExecucao.FULL,
                Map.of("tipo", "parlamentares-sub-recursos"));

        try {
            int total = camaraDeputadoApiExtractor.extrairEPersistirTodos(job);
            log.info("[Silver] Sub-recursos deputados Câmara: {} registros inseridos", total);
            return jobControlService.finalizar(job);

        } catch (RuntimeException e) {
            log.error("Falha crítica nos sub-recursos de deputados: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Falha crítica nos sub-recursos de deputados: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw new RuntimeException("Falha nos sub-recursos de deputados: " + e.getMessage(), e);
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────

    public int getCamaraAnoInicio() {
        return camaraAnoInicio;
    }

    public int getAnoAtual() {
        return LocalDate.now().getYear();
    }

    // ── Enriquecimento det_* ──────────────────────────────────────────

    /**
     * Enriquece os campos {@code det_*} de todos os deputados pendentes via
     * GET /api/v2/deputados/{id}.
     * Seguro de re-executar: processa apenas registros com
     * {@code det_status_id IS NULL}.
     *
     * @return {@link EtlJobControl} do job finalizado
     */
    public EtlJobControl executarEnriquecimentoDeputados() {
        log.info("Iniciando enriquecimento det_* de deputados Câmara");

        EtlJobControl job = jobControlService.iniciar(
                CasaLegislativa.CAMARA,
                TipoExecucao.FULL,
                Map.of("tipo", "enriquecimento-deputies"));

        try {
            int total = camaraDeputadoApiExtractor.enriquecerDetalhesTodos(job);
            log.info("[Silver] Enriquecimento dep Câmara: {} deputados processados", total);
            return jobControlService.finalizar(job);

        } catch (RuntimeException e) {
            log.error("Falha crítica no enriquecimento de deputados: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Falha crítica no enriquecimento de deputados: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw new RuntimeException("Falha no enriquecimento de deputados: " + e.getMessage(), e);
        }
    }
}
