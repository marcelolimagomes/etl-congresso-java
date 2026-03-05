package br.leg.congresso.etl.service;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.EtlErrorLog;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.StatusEtl;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.repository.EtlErrorLogRepository;
import br.leg.congresso.etl.repository.EtlJobControlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço que gerencia o ciclo de vida dos jobs ETL (controle de execução e logs de erro).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtlJobControlService {

    private final EtlJobControlRepository jobControlRepository;
    private final EtlErrorLogRepository errorLogRepository;

    /**
     * Inicia um novo job ETL e persiste o registro de controle.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EtlJobControl iniciar(CasaLegislativa casa, TipoExecucao tipo, Map<String, Object> parametros) {
        EtlJobControl job = new EtlJobControl();
        job.setOrigem(casa);
        job.setTipoExecucao(tipo);
        job.setStatus(StatusEtl.RUNNING);
        job.setParametros(parametros);

        EtlJobControl saved = jobControlRepository.save(job);
        log.info("Job ETL iniciado: id={}, casa={}, tipo={}", saved.getId(), casa, tipo);
        return saved;
    }

    /**
     * Finaliza o job com status SUCCESS, PARTIAL ou FAILED conforme contagem de erros.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EtlJobControl finalizar(EtlJobControl job) {
        StatusEtl statusFinal;
        if (job.getTotalErros() > 0 && job.getTotalProcessado() == 0) {
            statusFinal = StatusEtl.FAILED;
        } else if (job.getTotalErros() > 0) {
            statusFinal = StatusEtl.PARTIAL;
        } else {
            statusFinal = StatusEtl.SUCCESS;
        }
        job.finalizar(statusFinal);

        EtlJobControl saved = jobControlRepository.save(job);
        log.info("Job ETL finalizado: id={}, status={}, inseridos={}, atualizados={}, ignorados={}, erros={}",
            saved.getId(), saved.getStatus(),
            saved.getTotalInserido(), saved.getTotalAtualizado(),
            saved.getTotalIgnorados(), saved.getTotalErros());
        return saved;
    }

    /**
     * Marca o job como FAILED com mensagem de erro.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void falhar(EtlJobControl job, String mensagemErro) {
        job.finalizar(StatusEtl.FAILED);
        job.setMensagemErro(mensagemErro);
        jobControlRepository.save(job);
        log.error("Job ETL falhou: id={}, causa={}", job.getId(), mensagemErro);
    }

    /**
     * Registra um erro de processamento no log de erros do job.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarErro(EtlJobControl job, CasaLegislativa casa,
                               String tipoErro, String endpoint, String mensagem) {
        EtlErrorLog errorLog = new EtlErrorLog();
        errorLog.setJob(job);
        errorLog.setOrigem(casa);
        errorLog.setTipoErro(tipoErro);
        errorLog.setEndpoint(endpoint);
        errorLog.setMensagem(mensagem);
        errorLogRepository.save(errorLog);
    }

    /**
     * Lista os últimos N jobs de uma casa legislativa ordenados por data decrescente.
     */
    @Transactional(readOnly = true)
    public List<EtlJobControl> listarRecentes(int limite) {
        return jobControlRepository.findRecentes(
            org.springframework.data.domain.PageRequest.of(0, limite)
        );
    }

    /**
     * Verifica se há um job em execução para a casa informada.
     */
    @Transactional(readOnly = true)
    public boolean isRunning(CasaLegislativa casa) {
        return jobControlRepository.existsByOrigemAndStatus(casa, StatusEtl.RUNNING);
    }

    /**
     * Busca um job pelo ID.
     */
    @Transactional(readOnly = true)
    public Optional<EtlJobControl> buscarPorId(UUID id) {
        return jobControlRepository.findById(id);
    }

    /**
     * Lista os erros de um job específico.
     */
    @Transactional(readOnly = true)
    public List<EtlErrorLog> buscarErros(UUID jobId) {
        return errorLogRepository.findByJobIdOrderByCriadoEmDesc(jobId);
    }
}

