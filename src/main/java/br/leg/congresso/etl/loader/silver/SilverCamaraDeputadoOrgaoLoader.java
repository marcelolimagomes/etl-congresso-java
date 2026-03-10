package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoOrgao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoOrgaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoOrgaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega participação de deputados em órgãos da Câmara na camada Silver
 * (silver.camara_deputado_orgao).
 *
 * Princípio Silver: passthrough fiel ao CSV orgaosDeputados-L{leg}.csv.
 * Deduplicação por (id_deputado, id_orgao, data_inicio).
 * Registros já existentes são ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoOrgaoLoader {

    private final SilverCamaraDeputadoOrgaoRepository repository;

    /**
     * Persiste participações em órgãos de deputados, ignorando as já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo orgaosDeputados-L{leg}.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraDeputadoOrgaoCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoOrgaoCSVRow row : rows) {
            if (row.getIdDeputado() == null || row.getIdDeputado().isBlank()
                    || row.getIdOrgao() == null || row.getIdOrgao().isBlank()) {
                continue;
            }

            if (!repository.existsByIdDeputadoAndIdOrgaoAndDataInicio(
                    row.getIdDeputado(), row.getIdOrgao(), row.getDataInicio())) {
                repository.save(rowToEntity(row, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara órgãos deputado: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoOrgao rowToEntity(CamaraDeputadoOrgaoCSVRow row, UUID jobId) {
        return SilverCamaraDeputadoOrgao.builder()
                .etlJobId(jobId)
                .idDeputado(row.getIdDeputado())
                .idOrgao(row.getIdOrgao())
                .siglaOrgao(row.getSiglaOrgao())
                .nomeOrgao(row.getNomeOrgao())
                .nomePublicacao(row.getNomePublicacao())
                .titulo(row.getTitulo())
                .codTitulo(row.getCodTitulo())
                .dataInicio(row.getDataInicio())
                .dataFim(row.getDataFim())
                .uriOrgao(row.getUriOrgao())
                .build();
    }
}
