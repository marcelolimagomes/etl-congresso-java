package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoFrente;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoFrenteCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoFrenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega participação de deputados em frentes parlamentares na camada Silver
 * (silver.camara_deputado_frente).
 *
 * Princípio Silver: passthrough fiel ao CSV frentesDeputados.csv.
 * Deduplicação por (id_deputado, id_frente). Registros já existentes são
 * ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoFrenteLoader {

    private final SilverCamaraDeputadoFrenteRepository repository;

    /**
     * Persiste participações em frentes parlamentares, ignorando as já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo frentesDeputados.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraDeputadoFrenteCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoFrenteCSVRow row : rows) {
            if (row.getIdDeputado() == null || row.getIdDeputado().isBlank()
                    || row.getIdFrente() == null || row.getIdFrente().isBlank()) {
                continue;
            }

            if (!repository.existsByIdDeputadoAndIdFrente(row.getIdDeputado(), row.getIdFrente())) {
                repository.save(rowToEntity(row, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara frentes deputado: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoFrente rowToEntity(CamaraDeputadoFrenteCSVRow row, UUID jobId) {
        return SilverCamaraDeputadoFrente.builder()
                .etlJobId(jobId)
                .idDeputado(row.getIdDeputado())
                .idFrente(row.getIdFrente())
                .idLegislatura(row.getIdLegislatura())
                .titulo(row.getTitulo())
                .uri(row.getUri())
                .build();
    }
}
