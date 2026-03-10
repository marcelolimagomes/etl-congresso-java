package br.leg.congresso.etl.loader.silver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.silver.SilverCamaraDeputado;
import br.leg.congresso.etl.metrics.EtlMetrics;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoRepository;
import br.leg.congresso.etl.transformer.silver.SilverCamaraDeputadoHashGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Loader Silver para deputados da Câmara.
 * Persiste registros em silver.camara_deputado via upsert por camara_id.
 * Estratégia: INSERT/UPDATE baseado em content_hash para evitar writes
 * desnecessários.
 *
 * Princípio Silver: nenhuma transformação normalizadora — dados exatamente como
 * vieram da fonte.
 * Campos det_* (complemento API) são preservados no UPDATE para não
 * sobrescrever enriquecimento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoLoader {

    private final SilverCamaraDeputadoRepository repository;
    private final SilverCamaraDeputadoHashGenerator hashGenerator;
    private final EtlMetrics etlMetrics;

    @Value("${etl.batch-size:500}")
    private int batchSize;

    /**
     * Persiste um lote de deputados Silver, usando o jobControl para
     * rastreabilidade.
     * Hash é calculado sobre os campos-fonte do CSV; se hash não mudou, o registro
     * é ignorado.
     * Campos de enriquecimento (det_*) do registro existente são preservados no
     * UPDATE.
     *
     * @param deputados  lista de SilverCamaraDeputado a persistir
     * @param jobControl job ETL para rastreabilidade e contadores
     */
    @Transactional
    public void carregar(List<SilverCamaraDeputado> deputados, EtlJobControl jobControl) {
        if (deputados == null || deputados.isEmpty())
            return;

        int inseridos = 0, atualizados = 0, ignorados = 0;

        // Pré-carrega todos os registros existentes em uma única query (evita N+1)
        Map<String, SilverCamaraDeputado> existentes = repository.findAllByCamaraIdIn(
                deputados.stream()
                        .map(SilverCamaraDeputado::getCamaraId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SilverCamaraDeputado::getCamaraId, Function.identity()));

        for (SilverCamaraDeputado silver : deputados) {
            try {
                silver.setEtlJobId(jobControl.getId());

                String novoHash = hashGenerator.generate(silver);

                Optional<SilverCamaraDeputado> existente = (silver.getCamaraId() != null)
                        ? Optional.ofNullable(existentes.get(silver.getCamaraId()))
                        : Optional.empty();

                if (existente.isEmpty()) {
                    silver.setContentHash(novoHash);
                    silver.setGoldSincronizado(false);
                    repository.save(silver);
                    inseridos++;
                } else {
                    SilverCamaraDeputado atual = existente.get();
                    if (novoHash.equals(atual.getContentHash())) {
                        ignorados++;
                    } else {
                        // Atualiza campos CSV mantendo o ID e os campos det_* já enriquecidos
                        silver.setId(atual.getId());
                        silver.setIngeridoEm(atual.getIngeridoEm());
                        silver.setContentHash(novoHash);
                        silver.setGoldSincronizado(false);
                        // Preserva enriquecimento API existente
                        silver.setDetRedeSocial(atual.getDetRedeSocial());
                        silver.setDetStatusId(atual.getDetStatusId());
                        silver.setDetStatusIdLegislatura(atual.getDetStatusIdLegislatura());
                        silver.setDetStatusNome(atual.getDetStatusNome());
                        silver.setDetStatusNomeEleitoral(atual.getDetStatusNomeEleitoral());
                        silver.setDetStatusSiglaPartido(atual.getDetStatusSiglaPartido());
                        silver.setDetStatusSiglaUf(atual.getDetStatusSiglaUf());
                        silver.setDetStatusEmail(atual.getDetStatusEmail());
                        silver.setDetStatusSituacao(atual.getDetStatusSituacao());
                        silver.setDetStatusCondicaoEleitoral(atual.getDetStatusCondicaoEleitoral());
                        silver.setDetStatusDescricao(atual.getDetStatusDescricao());
                        silver.setDetStatusData(atual.getDetStatusData());
                        silver.setDetStatusUriPartido(atual.getDetStatusUriPartido());
                        silver.setDetStatusUrlFoto(atual.getDetStatusUrlFoto());
                        silver.setDetGabineteNome(atual.getDetGabineteNome());
                        silver.setDetGabinetePredio(atual.getDetGabinetePredio());
                        silver.setDetGabineteSala(atual.getDetGabineteSala());
                        silver.setDetGabineteAndar(atual.getDetGabineteAndar());
                        silver.setDetGabineteTelefone(atual.getDetGabineteTelefone());
                        silver.setDetGabineteEmail(atual.getDetGabineteEmail());
                        repository.save(silver);
                        atualizados++;
                    }
                }
            } catch (Exception e) {
                log.warn("Erro ao persistir Silver deputado camaraId={}: {}",
                        silver.getCamaraId(), e.getMessage());
                jobControl.incrementarErros();
            }
        }

        for (int i = 0; i < inseridos; i++)
            jobControl.incrementarInserido();
        for (int i = 0; i < atualizados; i++)
            jobControl.incrementarAtualizado();
        for (int i = 0; i < ignorados; i++)
            jobControl.incrementarIgnorados();

        if (inseridos > 0)
            etlMetrics.registrarInseridos(CasaLegislativa.CAMARA, inseridos);
        if (atualizados > 0)
            etlMetrics.registrarAtualizados(CasaLegislativa.CAMARA, atualizados);
        if (ignorados > 0)
            etlMetrics.registrarIgnorados(CasaLegislativa.CAMARA, ignorados);

        log.debug("[Silver Câmara Deputados] {} inseridos, {} atualizados, {} ignorados",
                inseridos, atualizados, ignorados);
    }

    /**
     * Persiste em lotes menores (para uso no extrator em chunks).
     */
    @Transactional
    public void carregarEmLotes(List<SilverCamaraDeputado> deputados, EtlJobControl jobControl) {
        if (deputados == null || deputados.isEmpty())
            return;

        for (int i = 0; i < deputados.size(); i += batchSize) {
            int fim = Math.min(i + batchSize, deputados.size());
            carregar(new ArrayList<>(deputados.subList(i, fim)), jobControl);
        }
    }

    /**
     * Persiste o enriquecimento det_* de um deputado já existente (merge do
     * objeto desanexado com os campos det_* preenchidos).
     * Chamado pelo {@code CamaraDeputadoApiExtractor.enriquecerDetalhesTodos()}.
     *
     * @param deputado entidade com os campos det_* atualizados
     */
    @Transactional
    public void salvarEnriquecimento(SilverCamaraDeputado deputado) {
        repository.save(deputado);
    }
}
