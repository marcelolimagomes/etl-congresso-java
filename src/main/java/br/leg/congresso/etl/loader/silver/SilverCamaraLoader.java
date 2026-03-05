package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.silver.SilverCamaraProposicao;
import br.leg.congresso.etl.metrics.EtlMetrics;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoRepository;
import br.leg.congresso.etl.transformer.silver.SilverCamaraHashGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loader Silver para proposições da Câmara.
 * Persiste registros em silver.camara_proposicao via upsert por camara_id.
 * Estratégia: INSERT/UPDATE baseado em content_hash para evitar writes desnecessários.
 *
 * Princípio Silver: nenhuma transformação normalizadora — dados exatamente como vieram da fonte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraLoader {

    private final SilverCamaraProposicaoRepository repository;
    private final SilverCamaraHashGenerator hashGenerator;
    private final EtlMetrics etlMetrics;

    @Value("${etl.batch-size:500}")
    private int batchSize;

    /**
     * Persiste um lote de proposições Silver, usando o jobControl para rastreabilidade.
     * Hash é calculado sobre os campos-fonte; se hash não mudou, o registro é ignorado.
     *
     * @param proposicoes lista de SilverCamaraProposicao a persistir
     * @param jobControl  job ETL para rastreabilidade e contadores
     */
    @Transactional
    public void carregar(List<SilverCamaraProposicao> proposicoes, EtlJobControl jobControl) {
        if (proposicoes == null || proposicoes.isEmpty()) return;

        int inseridos = 0, atualizados = 0, ignorados = 0;

        // Pré-carrega todos os registros existentes em uma única query (evita N+1)
        Map<String, SilverCamaraProposicao> existentes = repository.findAllByCamaraIdIn(
            proposicoes.stream()
                .map(SilverCamaraProposicao::getCamaraId)
                .filter(id -> id != null)
                .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(SilverCamaraProposicao::getCamaraId, Function.identity()));

        for (SilverCamaraProposicao silver : proposicoes) {
            try {
                // Injeta referência ao job
                silver.setEtlJobId(jobControl.getId());

                // Gera hash sobre campos fonte
                String novoHash = hashGenerator.generate(silver);

                // Verifica existência pelo camara_id (batch pré-carregado)
                Optional<SilverCamaraProposicao> existente = (silver.getCamaraId() != null)
                    ? Optional.ofNullable(existentes.get(silver.getCamaraId()))
                    : Optional.empty();

                if (existente.isEmpty()) {
                    silver.setContentHash(novoHash);
                    silver.setGoldSincronizado(false);
                    repository.save(silver);
                    inseridos++;
                } else {
                    SilverCamaraProposicao atual = existente.get();
                    if (novoHash.equals(atual.getContentHash())) {
                        ignorados++;
                    } else {
                        // Atualiza campos fonte mantendo o ID original
                        silver.setId(atual.getId());
                        silver.setIngeridoEm(atual.getIngeridoEm());
                        silver.setContentHash(novoHash);
                        silver.setGoldSincronizado(false); // precisa re-promover ao Gold
                        repository.save(silver);
                        atualizados++;
                    }
                }
            } catch (Exception e) {
                log.warn("Erro ao persistir Silver camaraId={}: {}",
                    silver.getCamaraId(), e.getMessage());
                jobControl.incrementarErros();
            }
        }

        // Atualiza contadores do job
        for (int i = 0; i < inseridos; i++) jobControl.incrementarInserido();
        for (int i = 0; i < atualizados; i++) jobControl.incrementarAtualizado();
        for (int i = 0; i < ignorados; i++) jobControl.incrementarIgnorados();

        // Registra métricas
        if (inseridos > 0) etlMetrics.registrarInseridos(CasaLegislativa.CAMARA, inseridos);
        if (atualizados > 0) etlMetrics.registrarAtualizados(CasaLegislativa.CAMARA, atualizados);
        if (ignorados > 0) etlMetrics.registrarIgnorados(CasaLegislativa.CAMARA, ignorados);

        log.debug("[Silver Câmara] {} inseridos, {} atualizados, {} ignorados",
            inseridos, atualizados, ignorados);
    }

    /**
     * Persiste em lotes menores (para uso no extrator em chunks).
     */
    @Transactional
    public void carregarEmLotes(List<SilverCamaraProposicao> proposicoes, EtlJobControl jobControl) {
        if (proposicoes == null || proposicoes.isEmpty()) return;

        for (int i = 0; i < proposicoes.size(); i += batchSize) {
            int fim = Math.min(i + batchSize, proposicoes.size());
            carregar(new ArrayList<>(proposicoes.subList(i, fim)), jobControl);
        }
    }
}
