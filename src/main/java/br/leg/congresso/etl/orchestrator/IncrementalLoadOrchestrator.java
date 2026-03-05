package br.leg.congresso.etl.orchestrator;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.TipoExecucao;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.camara.CamaraApiExtractor;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoDTO;
import br.leg.congresso.etl.extractor.camara.mapper.CamaraProposicaoMapper;
import br.leg.congresso.etl.extractor.senado.SenadoApiExtractor;
import br.leg.congresso.etl.extractor.senado.dto.SenadoMateriaDTO;
import br.leg.congresso.etl.extractor.senado.mapper.SenadoMateriaMapper;
import br.leg.congresso.etl.loader.silver.SilverCamaraLoader;
import br.leg.congresso.etl.loader.silver.SilverEnrichmentService;
import br.leg.congresso.etl.loader.silver.SilverSenadoLoader;
import br.leg.congresso.etl.promoter.SilverToGoldService;
import br.leg.congresso.etl.service.EtlJobControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Orquestrador de carga incremental para Câmara e Senado.
 * Busca apenas proposições atualizadas num intervalo de datas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncrementalLoadOrchestrator {

    private final CamaraApiExtractor camaraApiExtractor;
    private final SenadoApiExtractor senadoApiExtractor;
    private final CamaraProposicaoMapper camaraMapper;
    private final SenadoMateriaMapper senadoMapper;
    private final SilverCamaraLoader silverCamaraLoader;
    private final SilverSenadoLoader silverSenadoLoader;
    private final SilverEnrichmentService silverEnrichmentService;
    private final SilverToGoldService silverToGoldService;
    private final EtlJobControlService jobControlService;

    @Value("${etl.incremental.camara.lookback-days:7}")
    private int camaraLookbackDays;

    @Value("${etl.incremental.senado.lookback-days:7}")
    private int senadoLookbackDays;

    /**
     * Executa a carga incremental da Câmara para o intervalo informado.
     * Se as datas não forem informadas, usa o lookback-days configurado.
     */
    public EtlJobControl executarCamara(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now().minusDays(camaraLookbackDays);
        LocalDate fim    = dataFim    != null ? dataFim    : LocalDate.now();

        log.info("Carga incremental Câmara: {} a {}", inicio, fim);

        EtlJobControl job = jobControlService.iniciar(
            CasaLegislativa.CAMARA,
            TipoExecucao.INCREMENTAL,
            Map.of("dataInicio", inicio.toString(), "dataFim", fim.toString())
        );

        try {
            List<CamaraProposicaoDTO> proposicoesRaw = camaraApiExtractor.extractRawByDateRange(inicio, fim);
            log.info("[Silver] Câmara incremental raw: {} proposições encontradas", proposicoesRaw.size());

            List<SilverCamaraProposicao> silver = proposicoesRaw.stream()
                .map(camaraMapper::apiDtoToSilver)
                .toList();

            silverCamaraLoader.carregarEmLotes(silver, job);
            silverEnrichmentService.enriquecerTramitacoesCamara(silver);

            // Gold é alimentado exclusivamente via promoção Silver → Gold
            silverToGoldService.promoverCamara();

            return jobControlService.finalizar(job);

        } catch (Exception e) {
            log.error("Falha na carga incremental Câmara: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        }
    }

    /**
     * Executa a carga incremental do Senado para o intervalo informado.
     */
    public EtlJobControl executarSenado(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now().minusDays(senadoLookbackDays);
        LocalDate fim    = dataFim    != null ? dataFim    : LocalDate.now();

        log.info("Carga incremental Senado: {} a {}", inicio, fim);

        EtlJobControl job = jobControlService.iniciar(
            CasaLegislativa.SENADO,
            TipoExecucao.INCREMENTAL,
            Map.of("dataInicio", inicio.toString(), "dataFim", fim.toString())
        );

        try {
            List<SenadoMateriaDTO.Materia> materiasRaw = senadoApiExtractor.extractRawByDateRange(inicio, fim);
            log.info("[Silver] Senado incremental raw: {} matérias encontradas", materiasRaw.size());

            List<SilverSenadoMateria> silver = materiasRaw.stream()
                .map(senadoMapper::materiaToSilver)
                .toList();

            silverSenadoLoader.carregarEmLotes(silver, job);
            silverEnrichmentService.enriquecerDetalhesSenado();
            silverEnrichmentService.enriquecerMovimentacoesSenado(silver);

            // Gold é alimentado exclusivamente via promoção Silver → Gold
            silverToGoldService.promoverSenado();

            return jobControlService.finalizar(job);

        } catch (Exception e) {
            log.error("Falha na carga incremental Senado: {}", e.getMessage(), e);
            jobControlService.falhar(job, e.getMessage());
            throw e;
        }
    }
}
