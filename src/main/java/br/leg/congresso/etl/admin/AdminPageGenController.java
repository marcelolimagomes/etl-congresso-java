package br.leg.congresso.etl.admin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.leg.congresso.etl.pagegen.PageGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoints REST para geração de páginas estáticas de proposições.
 *
 * <p>
 * Todos os endpoints requerem autenticação Basic Auth (ROLE_ADMIN).
 * Prefixo: /admin/etl/pages
 */
@Slf4j
@RestController
@RequestMapping("/admin/etl/pages")
@RequiredArgsConstructor
public class AdminPageGenController {

    private final PageGeneratorService pageGeneratorService;

    /** Impede execuções simultâneas — geração de páginas é I/O-intensivo. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Inicia a geração de páginas estáticas (assíncrono).
     *
     * <pre>
     * POST /admin/etl/pages/generate[?ano=YYYY]
     * </pre>
     *
     * @param ano opcional — se informado, gera apenas as páginas do ano indicado
     * @return 202 Accepted com mensagem de confirmação, ou
     *         409 Conflict se já houver uma geração em andamento
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePages(
            @RequestParam(required = false) Integer ano) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Tentativa de iniciar geração de páginas enquanto outra já está em andamento");
            return ResponseEntity.status(409)
                    .body(Map.of(
                            "mensagem", "Geração de páginas já está em andamento.",
                            "status", "em_andamento"));
        }

        log.info("Geração de páginas estáticas iniciada via API admin{}",
                (ano != null ? " (ano=" + ano + ")" : ""));
        CompletableFuture.runAsync(() -> {
            try {
                int total = pageGeneratorService.generateAll(ano);
                log.info("Geração de páginas concluída via API admin: {} páginas geradas", total);
            } catch (Exception e) {
                log.error("Erro durante geração de páginas via API admin: {}", e.getMessage(), e);
            } finally {
                running.set(false);
            }
        });

        return ResponseEntity.accepted()
                .body(Map.of(
                        "mensagem", "Geração de páginas estáticas iniciada em background.",
                        "status", "iniciado"));
    }

    /**
     * Retorna o status atual da geração de páginas.
     *
     * <pre>
     * GET / admin / etl / pages / status
     * </pre>
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "emAndamento", running.get(),
                "status", running.get() ? "em_andamento" : "ocioso"));
    }
}
