package br.leg.congresso.etl.admin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.leg.congresso.etl.pagegen.ParlamentaresPageGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoints REST para geração de páginas estáticas de parlamentares.
 *
 * <p>
 * Todos os endpoints requerem autenticação Basic Auth (ROLE_ADMIN).
 * Prefixo: {@code /admin/etl/pages/parlamentares}
 */
@Slf4j
@RestController
@RequestMapping("/admin/etl/pages/parlamentares")
@RequiredArgsConstructor
public class AdminPageGenParlamentaresController {

    private final ParlamentaresPageGeneratorService pageGeneratorService;

    /** Impede execuções simultâneas. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Inicia a geração de páginas de parlamentares (assíncrono).
     *
     * <pre>
     * POST / admin / etl / pages / parlamentares / generate
     * </pre>
     *
     * @return 202 Accepted ou 409 Conflict se já em andamento
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePages() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Tentativa de iniciar geração de páginas de parlamentares enquanto outra já está em andamento");
            return ResponseEntity.status(409)
                    .body(Map.of(
                            "mensagem", "Geração de páginas de parlamentares já está em andamento.",
                            "status", "em_andamento"));
        }

        log.info("Geração de páginas de parlamentares iniciada via API admin");
        CompletableFuture.runAsync(() -> {
            try {
                int[] totais = pageGeneratorService.generateAll();
                log.info("Geração de páginas de parlamentares concluída: {} deputados, {} senadores",
                        totais[0], totais[1]);
            } catch (Exception e) {
                log.error("Erro durante geração de páginas de parlamentares: {}", e.getMessage(), e);
            } finally {
                running.set(false);
            }
        });

        return ResponseEntity.accepted()
                .body(Map.of(
                        "mensagem", "Geração de páginas de parlamentares iniciada em background.",
                        "status", "iniciado"));
    }

    /**
     * Retorna o status atual da geração de páginas de parlamentares.
     *
     * <pre>
     * GET / admin / etl / pages / parlamentares / status
     * </pre>
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "emAndamento", running.get(),
                "status", running.get() ? "em_andamento" : "ocioso"));
    }
}
