package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraMesaDiretora;
import br.leg.congresso.etl.extractor.camara.dto.CamaraMesaDiretoraCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraMesaDiretoraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega composição da Mesa Diretora da Câmara na camada Silver
 * (silver.camara_mesa_diretora).
 *
 * Princípio Silver: passthrough fiel ao CSV legislaturasMesas.csv.
 * Deduplicação por (id_deputado, titulo, id_legislatura).
 * Registros já existentes são ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraMesaDiretoraLoader {

    private final SilverCamaraMesaDiretoraRepository repository;

    /**
     * Persiste membros da Mesa Diretora, ignorando os já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo legislaturasMesas.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraMesaDiretoraCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraMesaDiretoraCSVRow row : rows) {
            if (row.getIdDeputado() == null || row.getIdDeputado().isBlank()
                    || row.getTitulo() == null || row.getTitulo().isBlank()
                    || row.getIdLegislatura() == null || row.getIdLegislatura().isBlank()) {
                continue;
            }

            if (!repository.existsByIdDeputadoAndIdLegislaturaAndTituloAndDataInicio(
                    row.getIdDeputado(), row.getIdLegislatura(), row.getTitulo(), row.getDataInicio())) {
                repository.save(rowToEntity(row, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara Mesa Diretora: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraMesaDiretora rowToEntity(CamaraMesaDiretoraCSVRow row, UUID jobId) {
        return SilverCamaraMesaDiretora.builder()
                .etlJobId(jobId)
                .idDeputado(row.getIdDeputado())
                .idLegislatura(row.getIdLegislatura())
                .titulo(row.getTitulo())
                .dataInicio(row.getDataInicio())
                .dataFim(row.getDataFim())
                .build();
    }
}
