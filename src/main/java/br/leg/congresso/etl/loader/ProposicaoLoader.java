package br.leg.congresso.etl.loader;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.Proposicao;
import br.leg.congresso.etl.metrics.EtlMetrics;
import br.leg.congresso.etl.transformer.DeduplicationService;
import br.leg.congresso.etl.transformer.DeduplicationService.Acao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de carga responsável por persistir proposições no banco de dados.
 * Utiliza deduplicação por hash de conteúdo e atualiza os contadores do job.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProposicaoLoader {

    private final BatchUpsertHelper batchUpsertHelper;
    private final DeduplicationService deduplicationService;
    private final EtlMetrics etlMetrics;

    @Value("${etl.batch-size:500}")
    private int batchSize;

    /**
     * Persiste um lote de proposições, atualizando os contadores do job de controle.
     * Classifica cada proposição como INSERT, UPDATE ou SKIP via hash de conteúdo.
     *
     * @param proposicoes lista de proposições já enriquecidas com contentHash
     * @param jobControl  controle do job ETL para atualização de contadores
     */
    @Transactional
    public void carregar(List<Proposicao> proposicoes, EtlJobControl jobControl) {
        if (proposicoes == null || proposicoes.isEmpty()) return;

        List<Proposicao> paraInserir = new ArrayList<>();
        List<Proposicao> paraAtualizar = new ArrayList<>();

        for (Proposicao p : proposicoes) {
            Acao acao = deduplicationService.avaliar(p);
            switch (acao) {
                case INSERT -> paraInserir.add(p);
                case UPDATE -> paraAtualizar.add(p);
                case SKIP -> jobControl.incrementarIgnorados();
            }
        }

        // Processa inserções em lotes
        processar(paraInserir, jobControl, true);

        // Processa atualizações em lotes
        processar(paraAtualizar, jobControl, false);

        int ignorados = proposicoes.size() - paraInserir.size() - paraAtualizar.size();

        // Registra métricas Micrometer
        etlMetrics.registrarInseridos(jobControl.getOrigem(), paraInserir.size());
        etlMetrics.registrarAtualizados(jobControl.getOrigem(), paraAtualizar.size());
        etlMetrics.registrarIgnorados(jobControl.getOrigem(), ignorados);

        log.debug("Lote processado: {} inseridos, {} atualizados, {} ignorados",
                paraInserir.size(), paraAtualizar.size(), ignorados);
    }

    private void processar(List<Proposicao> lista, EtlJobControl jobControl, boolean isInsert) {
        if (lista.isEmpty()) return;

        for (int i = 0; i < lista.size(); i += batchSize) {
            int fim = Math.min(i + batchSize, lista.size());
            List<Proposicao> sub = lista.subList(i, fim);
            try {
                batchUpsertHelper.upsert(sub);
                sub.forEach(p -> {
                    jobControl.incrementarProcessado();
                    if (isInsert) {
                        jobControl.incrementarInserido();
                    } else {
                        jobControl.incrementarAtualizado();
                    }
                });
            } catch (Exception e) {
                log.error("Erro ao persistir lote de {} proposições: {}", sub.size(), e.getMessage(), e);
                sub.forEach(p -> jobControl.incrementarErros());
                etlMetrics.registrarErros(jobControl.getOrigem(), sub.size());
            }
        }
    }
}
