package br.leg.congresso.etl.scheduler;

import br.leg.congresso.etl.orchestrator.EtlOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador do pipeline ETL.
 * Dispara cargas incrementais periódicas conforme configuração.
 * Configurado via propriedade {@code etl.scheduler.cron} no application.yml.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EtlScheduler {

    private final EtlOrchestrator orchestrator;

    @Value("${etl.scheduler.enabled:true}")
    private boolean enabled;

    /**
     * Carga incremental da Câmara dos Deputados.
     * Padrão: todo dia às 02:00 (horário de Brasília).
     * Configurar via {@code etl.scheduler.cron.camara}.
     */
    @Scheduled(cron = "${etl.scheduler.cron.camara:0 0 2 * * *}",
               zone  = "America/Sao_Paulo")
    public void executarIncrementalCamara() {
        if (!enabled) {
            log.debug("Scheduler desabilitado. Pulando carga incremental Câmara.");
            return;
        }
        log.info("Scheduler: iniciando carga incremental CÂMARA");
        try {
            orchestrator.incrementalCamara(null, null);
        } catch (Exception e) {
            log.error("Scheduler: erro na carga incremental Câmara: {}", e.getMessage(), e);
        }
    }

    /**
     * Carga incremental do Senado Federal.
     * Padrão: todo dia às 03:00 (horário de Brasília).
     * Configurar via {@code etl.scheduler.cron.senado}.
     */
    @Scheduled(cron = "${etl.scheduler.cron.senado:0 0 3 * * *}",
               zone  = "America/Sao_Paulo")
    public void executarIncrementalSenado() {
        if (!enabled) {
            log.debug("Scheduler desabilitado. Pulando carga incremental Senado.");
            return;
        }
        log.info("Scheduler: iniciando carga incremental SENADO");
        try {
            orchestrator.incrementalSenado(null, null);
        } catch (Exception e) {
            log.error("Scheduler: erro na carga incremental Senado: {}", e.getMessage(), e);
        }
    }
}
