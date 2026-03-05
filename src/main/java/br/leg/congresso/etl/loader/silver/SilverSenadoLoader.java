package br.leg.congresso.etl.loader.silver;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.metrics.EtlMetrics;
import br.leg.congresso.etl.repository.silver.SilverSenadoMateriaRepository;
import br.leg.congresso.etl.transformer.silver.SilverSenadoHashGenerator;
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
 * Loader Silver para matérias do Senado Federal.
 * Persiste registros em silver.senado_materia via upsert por codigo.
 * Estratégia: INSERT/UPDATE baseado em content_hash para evitar writes desnecessários.
 *
 * Princípio Silver: nenhuma transformação normalizadora — dados exatamente como vieram da API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoLoader {

    private final SilverSenadoMateriaRepository repository;
    private final SilverSenadoHashGenerator hashGenerator;
    private final EtlMetrics etlMetrics;

    @Value("${etl.batch-size:500}")
    private int batchSize;

    /**
     * Persiste um lote de matérias Silver, usando o jobControl para rastreabilidade.
     * Hash é calculado sobre os campos-fonte; se hash não mudou, o registro é ignorado.
     *
     * @param materias   lista de SilverSenadoMateria a persistir
     * @param jobControl job ETL para rastreabilidade e contadores
     */
    @Transactional
    public void carregar(List<SilverSenadoMateria> materias, EtlJobControl jobControl) {
        if (materias == null || materias.isEmpty()) return;

        int inseridos = 0, atualizados = 0, ignorados = 0;

        // Pré-carrega todos os registros existentes em uma única query (evita N+1)
        Map<String, SilverSenadoMateria> existentes = repository.findAllByCodigoIn(
            materias.stream()
                .map(SilverSenadoMateria::getCodigo)
                .filter(c -> c != null)
                .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(SilverSenadoMateria::getCodigo, Function.identity()));

        for (SilverSenadoMateria silver : materias) {
            try {
                silver.setEtlJobId(jobControl.getId());

                String novoHash = hashGenerator.generate(silver);

                Optional<SilverSenadoMateria> existente = (silver.getCodigo() != null)
                    ? Optional.ofNullable(existentes.get(silver.getCodigo()))
                    : Optional.empty();

                if (existente.isEmpty()) {
                    silver.setContentHash(novoHash);
                    silver.setGoldSincronizado(false);
                    repository.save(silver);
                    inseridos++;
                } else {
                    SilverSenadoMateria atual = existente.get();
                    if (novoHash.equals(atual.getContentHash())) {
                        ignorados++;
                    } else {
                        silver.setId(atual.getId());
                        silver.setIngeridoEm(atual.getIngeridoEm());
                        silver.setContentHash(novoHash);
                        silver.setGoldSincronizado(false);
                        // Preserva campos de enriquecimento (det_*) que não vêm na pesquisa
                        preservarCamposEnriquecimento(silver, atual);
                        repository.save(silver);
                        atualizados++;
                    }
                }
            } catch (Exception e) {
                log.warn("Erro ao persistir Silver Senado codigo={}: {}",
                    silver.getCodigo(), e.getMessage());
                jobControl.incrementarErros();
            }
        }

        for (int i = 0; i < inseridos; i++) jobControl.incrementarInserido();
        for (int i = 0; i < atualizados; i++) jobControl.incrementarAtualizado();
        for (int i = 0; i < ignorados; i++) jobControl.incrementarIgnorados();

        if (inseridos > 0) etlMetrics.registrarInseridos(CasaLegislativa.SENADO, inseridos);
        if (atualizados > 0) etlMetrics.registrarAtualizados(CasaLegislativa.SENADO, atualizados);
        if (ignorados > 0) etlMetrics.registrarIgnorados(CasaLegislativa.SENADO, ignorados);

        log.debug("[Silver Senado] {} inseridos, {} atualizados, {} ignorados",
            inseridos, atualizados, ignorados);
    }

    /**
     * Copia campos de enriquecimento (det_*, movimentações, urlTexto, dataTexto)
     * do registro existente para o novo, evitando perda de dados já enriquecidos.
     */
    private void preservarCamposEnriquecimento(SilverSenadoMateria novo, SilverSenadoMateria atual) {
        novo.setDetSiglaCasaIdentificacao(atual.getDetSiglaCasaIdentificacao());
        novo.setDetSiglaSubtipo(atual.getDetSiglaSubtipo());
        novo.setDetDescricaoSubtipo(atual.getDetDescricaoSubtipo());
        novo.setDetDescricaoObjetivoProcesso(atual.getDetDescricaoObjetivoProcesso());
        novo.setDetIndicadorTramitando(atual.getDetIndicadorTramitando());
        novo.setDetIndexacao(atual.getDetIndexacao());
        novo.setDetCasaIniciadora(atual.getDetCasaIniciadora());
        novo.setDetIndicadorComplementar(atual.getDetIndicadorComplementar());
        novo.setDetNaturezaCodigo(atual.getDetNaturezaCodigo());
        novo.setDetNaturezaNome(atual.getDetNaturezaNome());
        novo.setDetNaturezaDescricao(atual.getDetNaturezaDescricao());
        novo.setDetSiglaCasaOrigem(atual.getDetSiglaCasaOrigem());
        novo.setDetClassificacoes(atual.getDetClassificacoes());
        novo.setDetOutrasInformacoes(atual.getDetOutrasInformacoes());
        novo.setUrlTexto(atual.getUrlTexto());
        novo.setDataTexto(atual.getDataTexto());
    }

    /**
     * Persiste em lotes menores.
     */
    @Transactional
    public void carregarEmLotes(List<SilverSenadoMateria> materias, EtlJobControl jobControl) {
        if (materias == null || materias.isEmpty()) return;

        for (int i = 0; i < materias.size(); i += batchSize) {
            int fim = Math.min(i + batchSize, materias.size());
            carregar(new ArrayList<>(materias.subList(i, fim)), jobControl);
        }
    }
}
