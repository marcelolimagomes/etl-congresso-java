package br.leg.congresso.etl.metrics;

import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Métricas Micrometer para o pipeline ETL.
 * Expõe contadores e timers acessíveis via /actuator/prometheus.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EtlMetrics {

    private final MeterRegistry meterRegistry;

    // ── Contadores ────────────────────────────────────────────────────

    private Counter camaraInseridos;
    private Counter camaraAtualizados;
    private Counter camaraIgnorados;
    private Counter camaraErros;

    private Counter senadoInseridos;
    private Counter senadoAtualizados;
    private Counter senadoIgnorados;
    private Counter senadoErros;

    // ── Timers ────────────────────────────────────────────────────────

    private Timer camaraFullLoadTimer;
    private Timer camaraIncrementalTimer;
    private Timer senadoFullLoadTimer;
    private Timer senadoIncrementalTimer;

    @PostConstruct
    public void init() {
        // Câmara
        camaraInseridos   = Counter.builder("etl.proposicoes.inseridas")
            .tag("casa", "CAMARA").description("Proposições inseridas da Câmara").register(meterRegistry);
        camaraAtualizados = Counter.builder("etl.proposicoes.atualizadas")
            .tag("casa", "CAMARA").description("Proposições atualizadas da Câmara").register(meterRegistry);
        camaraIgnorados   = Counter.builder("etl.proposicoes.ignoradas")
            .tag("casa", "CAMARA").description("Proposições ignoradas (hash igual)").register(meterRegistry);
        camaraErros       = Counter.builder("etl.proposicoes.erros")
            .tag("casa", "CAMARA").description("Erros de processamento Câmara").register(meterRegistry);

        // Senado
        senadoInseridos   = Counter.builder("etl.proposicoes.inseridas")
            .tag("casa", "SENADO").description("Matérias inseridas do Senado").register(meterRegistry);
        senadoAtualizados = Counter.builder("etl.proposicoes.atualizadas")
            .tag("casa", "SENADO").description("Matérias atualizadas do Senado").register(meterRegistry);
        senadoIgnorados   = Counter.builder("etl.proposicoes.ignoradas")
            .tag("casa", "SENADO").description("Matérias ignoradas (hash igual)").register(meterRegistry);
        senadoErros       = Counter.builder("etl.proposicoes.erros")
            .tag("casa", "SENADO").description("Erros de processamento Senado").register(meterRegistry);

        // Timers
        camaraFullLoadTimer       = Timer.builder("etl.job.duracao")
            .tag("casa", "CAMARA").tag("tipo", "FULL").description("Duração de carga total Câmara").register(meterRegistry);
        camaraIncrementalTimer    = Timer.builder("etl.job.duracao")
            .tag("casa", "CAMARA").tag("tipo", "INCREMENTAL").register(meterRegistry);
        senadoFullLoadTimer       = Timer.builder("etl.job.duracao")
            .tag("casa", "SENADO").tag("tipo", "FULL").register(meterRegistry);
        senadoIncrementalTimer    = Timer.builder("etl.job.duracao")
            .tag("casa", "SENADO").tag("tipo", "INCREMENTAL").register(meterRegistry);

        log.info("EtlMetrics inicializado com {} medidores", meterRegistry.getMeters().size());
    }

    // ── API pública ────────────────────────────────────────────────────

    public void registrarInseridos(CasaLegislativa casa, int qtd) {
        getCounter(casa, "inseridos").increment(qtd);
    }

    public void registrarAtualizados(CasaLegislativa casa, int qtd) {
        getCounter(casa, "atualizados").increment(qtd);
    }

    public void registrarIgnorados(CasaLegislativa casa, int qtd) {
        getCounter(casa, "ignorados").increment(qtd);
    }

    public void registrarErros(CasaLegislativa casa, int qtd) {
        getCounter(casa, "erros").increment(qtd);
    }

    public Timer.Sample iniciarTimer() {
        return Timer.start(meterRegistry);
    }

    public void finalizarTimer(Timer.Sample sample, CasaLegislativa casa, String tipo) {
        sample.stop(getTimer(casa, tipo));
    }

    // ── Helpers privados ───────────────────────────────────────────────

    private Counter getCounter(CasaLegislativa casa, String tipo) {
        return switch (tipo) {
            case "inseridos"   -> casa == CasaLegislativa.CAMARA ? camaraInseridos   : senadoInseridos;
            case "atualizados" -> casa == CasaLegislativa.CAMARA ? camaraAtualizados : senadoAtualizados;
            case "ignorados"   -> casa == CasaLegislativa.CAMARA ? camaraIgnorados   : senadoIgnorados;
            case "erros"       -> casa == CasaLegislativa.CAMARA ? camaraErros       : senadoErros;
            default            -> throw new IllegalArgumentException("Tipo de contador inválido: " + tipo);
        };
    }

    private Timer getTimer(CasaLegislativa casa, String tipo) {
        if (casa == CasaLegislativa.CAMARA) {
            return "FULL".equalsIgnoreCase(tipo) ? camaraFullLoadTimer : camaraIncrementalTimer;
        } else {
            return "FULL".equalsIgnoreCase(tipo) ? senadoFullLoadTimer : senadoIncrementalTimer;
        }
    }
}
