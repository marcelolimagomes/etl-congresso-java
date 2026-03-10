package br.leg.congresso.etl.admin;

import br.leg.congresso.etl.admin.dto.EtlErrorResponse;
import br.leg.congresso.etl.admin.dto.EtlStatusResponse;
import br.leg.congresso.etl.admin.dto.EtlTriggerRequest;
import br.leg.congresso.etl.admin.dto.SilverStatusDTO;
import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.orchestrator.EtlOrchestrator;
import br.leg.congresso.etl.service.EtlJobControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller REST para gerenciamento e monitoramento do pipeline ETL.
 * Todos os endpoints requerem autenticação Basic Auth (ROLE_ADMIN).
 * Prefixo: /admin/etl
 */
@Slf4j
@RestController
@RequestMapping("/admin/etl")
@RequiredArgsConstructor
public class AdminEtlController {

    private final EtlOrchestrator orchestrator;
    private final EtlJobControlService jobControlService;
    private final SilverStatusService silverStatusService;

    // ── Câmara ────────────────────────────────────────────────────────

    /**
     * Dispara a carga total da Câmara (assíncrono).
     * GET /admin/etl/camara/full-load?anoInicio=1988&anoFim=2024
     */
    @PostMapping("/camara/full-load")
    public ResponseEntity<Map<String, String>> fullLoadCamara(
            @RequestParam(required = false) Integer anoInicio,
            @RequestParam(required = false) Integer anoFim) {

        log.info("Acionado full-load Câmara via API: {} - {}", anoInicio, anoFim);
        CompletableFuture.runAsync(() -> orchestrator.fullLoadCamara(anoInicio, anoFim));
        return ResponseEntity.accepted().body(Map.of("mensagem", "Carga total Câmara iniciada em background."));
    }

    /**
     * Dispara a carga incremental da Câmara (assíncrono).
     * POST /admin/etl/camara/incremental
     */
    @PostMapping("/camara/incremental")
    public ResponseEntity<Map<String, String>> incrementalCamara(
            @RequestBody(required = false) EtlTriggerRequest request) {

        var dataInicio = request != null ? request.dataInicio() : null;
        var dataFim = request != null ? request.dataFim() : null;
        log.info("Acionado incremental Câmara via API: {} - {}", dataInicio, dataFim);
        CompletableFuture.runAsync(() -> orchestrator.incrementalCamara(dataInicio, dataFim));
        return ResponseEntity.accepted().body(Map.of("mensagem", "Carga incremental Câmara iniciada em background."));
    }

    // ── Senado ────────────────────────────────────────────────────────

    /**
     * Dispara a carga total do Senado (assíncrono).
     * POST /admin/etl/senado/full-load?anoInicio=1988&anoFim=2024
     */
    @PostMapping("/senado/full-load")
    public ResponseEntity<Map<String, String>> fullLoadSenado(
            @RequestParam(required = false) Integer anoInicio,
            @RequestParam(required = false) Integer anoFim) {

        log.info("Acionado full-load Senado via API: {} - {}", anoInicio, anoFim);
        CompletableFuture.runAsync(() -> orchestrator.fullLoadSenado(anoInicio, anoFim));
        return ResponseEntity.accepted().body(Map.of("mensagem", "Carga total Senado iniciada em background."));
    }

    /**
     * Dispara a carga incremental do Senado (assíncrono).
     * POST /admin/etl/senado/incremental
     */
    @PostMapping("/senado/incremental")
    public ResponseEntity<Map<String, String>> incrementalSenado(
            @RequestBody(required = false) EtlTriggerRequest request) {

        var dataInicio = request != null ? request.dataInicio() : null;
        var dataFim = request != null ? request.dataFim() : null;
        log.info("Acionado incremental Senado via API: {} - {}", dataInicio, dataFim);
        CompletableFuture.runAsync(() -> orchestrator.incrementalSenado(dataInicio, dataFim));
        return ResponseEntity.accepted().body(Map.of("mensagem", "Carga incremental Senado iniciada em background."));
    }

    // ── Parlamentares ─────────────────────────────────────────────────

    /**
     * Dispara a carga completa de deputados da Câmara (CSVs + API-only,
     * assíncrono).
     * POST /admin/etl/parlamentares/camara?anoInicio=1988&anoFim=2024
     */
    @PostMapping("/parlamentares/camara")
    public ResponseEntity<Map<String, String>> fullLoadParlamentaresCamara(
            @RequestParam(required = false) Integer anoInicio,
            @RequestParam(required = false) Integer anoFim) {

        log.info("Acionado full-load parlamentares Câmara via API: {} - {}", anoInicio, anoFim);
        CompletableFuture.runAsync(() -> orchestrator.fullLoadParlamentaresCamara(anoInicio, anoFim));
        return ResponseEntity.accepted().body(
                Map.of("mensagem", "Carga de parlamentares Câmara iniciada em background."));
    }

    /**
     * Dispara a carga completa de senadores do Senado Federal (API, assíncrono).
     * POST /admin/etl/parlamentares/senado
     */
    @PostMapping("/parlamentares/senado")
    public ResponseEntity<Map<String, String>> fullLoadParlamentaresSenado() {

        log.info("Acionado full-load parlamentares Senado via API");
        CompletableFuture.runAsync(() -> orchestrator.fullLoadParlamentaresSenado());
        return ResponseEntity.accepted().body(
                Map.of("mensagem", "Carga de parlamentares Senado iniciada em background."));
    }

    /**
     * Enriquece os campos det_* dos deputados via GET /api/v2/deputados/{id}.
     * Processa apenas registros com det_status_id IS NULL (pendentes de
     * enriquecimento).
     * POST /admin/etl/parlamentares/camara/enriquece
     */
    @PostMapping("/parlamentares/camara/enriquece")
    public ResponseEntity<Map<String, String>> enriquecerDeputados() {

        log.info("Acionado enriquecimento det_* deputados Câmara via API");
        CompletableFuture.runAsync(() -> orchestrator.enriquecerDeputadosCamara());
        return ResponseEntity.accepted().body(
                Map.of("mensagem", "Enriquecimento de deputados Câmara iniciado em background."));
    }

    /**
     * Carga CSV de parlamentares da Câmara (etapas 1-3), sem sub-recursos API.
     * Útil para complementar presenças e despesas CEAP de anos ainda não
     * carregados.
     * POST /admin/etl/parlamentares/camara/csv?anoInicio=2025&anoFim=2026
     */
    @PostMapping("/parlamentares/camara/csv")
    public ResponseEntity<Map<String, String>> cargaCSVParlamentaresCamara(
            @RequestParam(required = false) Integer anoInicio,
            @RequestParam(required = false) Integer anoFim) {

        log.info("Acionado carga CSV parlamentares Câmara via API: {} - {}", anoInicio, anoFim);
        CompletableFuture.runAsync(() -> orchestrator.cargaCSVParlamentaresCamara(anoInicio, anoFim));
        return ResponseEntity.accepted().body(
                Map.of("mensagem", "Carga CSV de parlamentares Câmara iniciada em background."));
    }

    /**
     * Carga dos sub-recursos API de deputados (etapa 4 isolada):
     * discursos, eventos, histórico e mandatos externos.
     * POST /admin/etl/parlamentares/camara/sub-recursos
     */
    @PostMapping("/parlamentares/camara/sub-recursos")
    public ResponseEntity<Map<String, String>> subRecursosParlamentaresCamara() {

        log.info("Acionado carga sub-recursos API deputados Câmara via API");
        CompletableFuture.runAsync(() -> orchestrator.subRecursosParlamentaresCamara());
        return ResponseEntity.accepted().body(
                Map.of("mensagem", "Carga de sub-recursos de deputados Câmara iniciada em background."));
    }

    // ── Status e Monitoramento ────────────────────────────────────────

    /**
     * Lista os últimos jobs ETL (padrão: 20 mais recentes).
     * GET /admin/etl/jobs?limite=20
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<EtlStatusResponse>> listarJobs(
            @RequestParam(defaultValue = "20") int limite) {

        List<EtlStatusResponse> jobs = jobControlService.listarRecentes(limite)
                .stream()
                .map(EtlStatusResponse::from)
                .toList();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Retorna detalhes de um job específico.
     * GET /admin/etl/jobs/{id}
     */
    @GetMapping("/jobs/{id}")
    public ResponseEntity<EtlStatusResponse> buscarJob(@PathVariable UUID id) {
        return jobControlService.buscarPorId(id)
                .map(j -> ResponseEntity.ok(EtlStatusResponse.from(j)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lista os erros de um job específico.
     * GET /admin/etl/jobs/{id}/errors
     */
    @GetMapping("/jobs/{id}/errors")
    public ResponseEntity<List<EtlErrorResponse>> buscarErrosJob(@PathVariable UUID id) {
        return jobControlService.buscarPorId(id)
                .map(j -> ResponseEntity.ok(
                        jobControlService.buscarErros(id)
                                .stream()
                                .map(EtlErrorResponse::from)
                                .toList()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Reprocessamento forçado ───────────────────────────────────────

    /**
     * Força o reprocessamento de um ano específico da Câmara (recheca CSV e
     * reimporta).
     * POST /admin/etl/camara/reprocess?ano=2024
     */
    @PostMapping("/camara/reprocess")
    public ResponseEntity<Map<String, String>> reprocessarCamara(
            @RequestParam int ano) {

        log.info("Reprocessamento forçado Câmara ano={} acionado via API", ano);
        CompletableFuture.runAsync(() -> orchestrator.reprocessarCamara(ano));
        return ResponseEntity.accepted().body(Map.of(
                "mensagem", "Reprocessamento Câmara ano=" + ano + " iniciado em background."));
    }

    /**
     * Força o reprocessamento de uma data específica do Senado.
     * POST /admin/etl/senado/reprocess?data=2024-01-01
     */
    @PostMapping("/senado/reprocess")
    public ResponseEntity<Map<String, String>> reprocessarSenado(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        log.info("Reprocessamento forçado Senado data={} acionado via API", data);
        CompletableFuture.runAsync(() -> orchestrator.reprocessarSenado(data));
        return ResponseEntity.accepted().body(Map.of(
                "mensagem", "Reprocessamento Senado data=" + data + " iniciado em background."));
    }

    /**
     * Retorna o status atual da camada Silver (contagens Silver x Gold).
     * GET /admin/etl/silver/status?ano=2024
     */
    @GetMapping("/silver/status")
    public ResponseEntity<SilverStatusDTO> silverStatus(
            @RequestParam(required = false) Integer ano) {
        return ResponseEntity.ok(silverStatusService.calcularStatus(ano));
    }

    /**
     * Verifica o status atual do pipeline ETL.
     * GET /admin/etl/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean camaraRunning = jobControlService.isRunning(
                br.leg.congresso.etl.domain.enums.CasaLegislativa.CAMARA);
        boolean senadoRunning = jobControlService.isRunning(
                br.leg.congresso.etl.domain.enums.CasaLegislativa.SENADO);

        return ResponseEntity.ok(Map.of(
                "camara", Map.of("emExecucao", camaraRunning),
                "senado", Map.of("emExecucao", senadoRunning)));
    }
}
