package br.leg.congresso.etl.promoter;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.service.EtlJobControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serviço de promoção Silver → Gold.
 *
 * Orquestra a leitura de registros Silver pendentes (gold_sincronizado = false)
 * e invoca os Promoters específicos por casa legislativa para persistência no Gold.
 *
 * Pode ser chamado:
 * - Ao final de cada full load (para promover tudo que foi carregado)
 * - Como job independente (para reprocesar falhas ou resyncs parciais)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverToGoldService {

    private final SilverCamaraProposicaoRepository silverCamaraRepository;
    private final SilverSenadoMateriaRepository silverSenadoRepository;
    private final CamaraGoldPromoter camaraGoldPromoter;
    private final SenadoGoldPromoter senadoGoldPromoter;
    private final EtlJobControlService jobControlService;

    @Value("${etl.batch-size:500}")
    private int batchSize;

    /**
     * Promove todos os registros Silver da Câmara pendentes de promoção Gold.
     * Processa em lotes para evitar pressão de memória.
     *
     * @return EtlJobControl com os resultados da operação
     */
    public EtlJobControl promoverCamara() {
        log.info("[Silver→Gold] Iniciando promoção Silver Câmara → Gold");

        EtlJobControl job = jobControlService.iniciar(
            CasaLegislativa.CAMARA,
            TipoExecucao.PROMOCAO,
            Map.of("operacao", "silver_to_gold", "casa", "CAMARA")
        );

        try {
            List<SilverCamaraProposicao> pendentes = silverCamaraRepository.findByGoldSincronizadoFalse();
            log.info("[Silver→Gold] Câmara: {} registros pendentes de promoção", pendentes.size());

            for (int i = 0; i < pendentes.size(); i += batchSize) {
                int fim = Math.min(i + batchSize, pendentes.size());
                List<SilverCamaraProposicao> lote = new ArrayList<>(pendentes.subList(i, fim));
                try {
                    camaraGoldPromoter.promover(lote, job);
                } catch (Exception e) {
                    log.error("[Silver→Gold] Erro ao promover lote Câmara [{}-{}]: {}", i, fim, e.getMessage(), e);
                    lote.forEach(s -> job.incrementarErros());
                }
            }

            EtlJobControl finalizado = jobControlService.finalizar(job);
            log.info("[Silver→Gold] Câmara concluído: {} inseridos, {} atualizados, {} erros",
                finalizado.getTotalInserido(), finalizado.getTotalAtualizado(), finalizado.getTotalErros());
            return finalizado;

        } catch (Exception e) {
            log.error("[Silver→Gold] Falha crítica na promoção Câmara: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        }
    }

    /**
     * Promove todos os registros Silver do Senado pendentes de promoção Gold.
     *
     * @return EtlJobControl com os resultados da operação
     */
    public EtlJobControl promoverSenado() {
        log.info("[Silver→Gold] Iniciando promoção Silver Senado → Gold");

        EtlJobControl job = jobControlService.iniciar(
            CasaLegislativa.SENADO,
            TipoExecucao.PROMOCAO,
            Map.of("operacao", "silver_to_gold", "casa", "SENADO")
        );

        try {
            List<SilverSenadoMateria> pendentes = silverSenadoRepository.findByGoldSincronizadoFalse();
            log.info("[Silver→Gold] Senado: {} registros pendentes de promoção", pendentes.size());

            for (int i = 0; i < pendentes.size(); i += batchSize) {
                int fim = Math.min(i + batchSize, pendentes.size());
                List<SilverSenadoMateria> lote = new ArrayList<>(pendentes.subList(i, fim));
                try {
                    senadoGoldPromoter.promover(lote, job);
                } catch (Exception e) {
                    log.error("[Silver→Gold] Erro ao promover lote Senado [{}-{}]: {}", i, fim, e.getMessage(), e);
                    lote.forEach(s -> job.incrementarErros());
                }
            }

            EtlJobControl finalizado = jobControlService.finalizar(job);
            log.info("[Silver→Gold] Senado concluído: {} inseridos, {} atualizados, {} erros",
                finalizado.getTotalInserido(), finalizado.getTotalAtualizado(), finalizado.getTotalErros());
            return finalizado;

        } catch (Exception e) {
            log.error("[Silver→Gold] Falha crítica na promoção Senado: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        }
    }

    /**
     * Promove ambas as casas em sequência.
     * Conveniente para uso após um full load completo.
     */
    public void promoverTudo() {
        log.info("[Silver→Gold] Iniciando promoção completa (Câmara + Senado)");
        promoverCamara();
        promoverSenado();
        log.info("[Silver→Gold] Promoção completa concluída");
    }
}
