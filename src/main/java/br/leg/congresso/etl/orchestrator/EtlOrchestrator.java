package br.leg.congresso.etl.orchestrator;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.repository.EtlFileControlRepository;
import br.leg.congresso.etl.service.EtlJobControlService;
import br.leg.congresso.etl.service.EtlLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * Fachada principal do orquestrador ETL.
 * Delega para FullLoadOrchestrator ou IncrementalLoadOrchestrator conforme o tipo de execução.
 * Garante que não haja execuções concorrentes para a mesma casa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtlOrchestrator {

    private final FullLoadOrchestrator fullLoad;
    private final IncrementalLoadOrchestrator incrementalLoad;
    private final EtlJobControlService jobControlService;
    private final EtlFileControlRepository fileControlRepository;
    private final EtlLockService lockService;

    // ── Câmara ────────────────────────────────────────────────────────

    /**
     * Carga total da Câmara dos Deputados para os anos informados.
     * Se os anos não forem informados, usa os padrões configurados.
     */
    public EtlJobControl fullLoadCamara(Integer anoInicio, Integer anoFim) {
        return executarComLock(CasaLegislativa.CAMARA, () -> {
            int inicio = anoInicio != null ? anoInicio : fullLoad.getCamaraAnoInicio();
            int fim    = anoFim    != null ? anoFim    : fullLoad.getAnoAtual();
            return fullLoad.executarCamara(inicio, fim);
        });
    }

    /**
     * Carga incremental da Câmara para o intervalo de datas.
     */
    public EtlJobControl incrementalCamara(LocalDate dataInicio, LocalDate dataFim) {
        return executarComLock(CasaLegislativa.CAMARA,
            () -> incrementalLoad.executarCamara(dataInicio, dataFim));
    }

    // ── Senado ────────────────────────────────────────────────────────

    /**
     * Carga total do Senado Federal para os anos informados.
     */
    public EtlJobControl fullLoadSenado(Integer anoInicio, Integer anoFim) {
        return executarComLock(CasaLegislativa.SENADO, () -> {
            int inicio = anoInicio != null ? anoInicio : fullLoad.getSenadoAnoInicio();
            int fim    = anoFim    != null ? anoFim    : fullLoad.getAnoAtual();
            return fullLoad.executarSenado(inicio, fim);
        });
    }

    /**
     * Carga incremental do Senado para o intervalo de datas.
     */
    public EtlJobControl incrementalSenado(LocalDate dataInicio, LocalDate dataFim) {
        return executarComLock(CasaLegislativa.SENADO,
            () -> incrementalLoad.executarSenado(dataInicio, dataFim));
    }

    // ── Utilitários ───────────────────────────────────────────────────

    private void checkRunning(CasaLegislativa casa) {
        if (jobControlService.isRunning(casa)) {
            throw new IllegalStateException(
                "Já existe um job ETL em execução para " + casa + ". Aguarde a conclusão antes de iniciar outro."
            );
        }
    }

    // ── Reprocessamento ───────────────────────────────────────────────

    /**
     * Marca o controle de arquivo do ano informado para reprocessamento
     * e re-executa a carga completa da Câmara para esse ano.
     */
    @Transactional
    public EtlJobControl reprocessarCamara(int ano) {
        return executarComLock(CasaLegislativa.CAMARA, () -> {
            int marcados = fileControlRepository.marcarParaReprocessamento(CasaLegislativa.CAMARA, ano);
            log.info("Reprocessamento Câmara: {} arquivo(s) marcado(s) para o ano {}", marcados, ano);
            return fullLoad.executarCamara(ano, ano);
        });
    }

    /**
     * Marca o controle de arquivo da data informada para reprocessamento
     * e re-executa a carga incremental do Senado para esse dia.
     */
    @Transactional
    public EtlJobControl reprocessarSenado(LocalDate data) {
        return executarComLock(CasaLegislativa.SENADO, () -> {
            int marcados = fileControlRepository.marcarParaReprocessamentoPorData(CasaLegislativa.SENADO, data);
            log.info("Reprocessamento Senado: {} registro(s) marcado(s) para {}", marcados, data);
            return incrementalLoad.executarSenado(data, data);
        });
    }

    private EtlJobControl executarComLock(CasaLegislativa casa, Supplier<EtlJobControl> acao) {
        String recurso = "etl:" + casa.name().toLowerCase();
        if (!lockService.tryAcquire(recurso)) {
            throw new IllegalStateException(
                "Não foi possível adquirir lock distribuído para " + casa + ". Execução concorrente detectada."
            );
        }

        try {
            checkRunning(casa);
            return acao.get();
        } finally {
            lockService.release(recurso);
        }
    }
}
