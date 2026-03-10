package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraGrupoMembro;
import br.leg.congresso.etl.extractor.camara.dto.CamaraGrupoMembroCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraGrupoMembroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega membros de grupos de trabalho da Câmara na camada Silver
 * (silver.camara_grupo_membro).
 *
 * Princípio Silver: passthrough fiel ao CSV gruposMembros.csv.
 * Deduplicação por (id_deputado, id_grupo). Registros já existentes são
 * ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraGrupoMembroLoader {

    private final SilverCamaraGrupoMembroRepository repository;

    /**
     * Persiste membros de grupos, ignorando os já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo gruposMembros.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraGrupoMembroCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraGrupoMembroCSVRow row : rows) {
            if (row.getIdDeputado() == null || row.getIdDeputado().isBlank()
                    || row.getIdGrupo() == null || row.getIdGrupo().isBlank()) {
                continue;
            }

            if (!repository.existsByIdDeputadoAndIdGrupoAndDataInicio(row.getIdDeputado(), row.getIdGrupo(),
                    row.getDataInicio())) {
                repository.save(rowToEntity(row, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara Grupos membros: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraGrupoMembro rowToEntity(CamaraGrupoMembroCSVRow row, UUID jobId) {
        return SilverCamaraGrupoMembro.builder()
                .etlJobId(jobId)
                .idDeputado(row.getIdDeputado())
                .idGrupo(row.getIdGrupo())
                .nomeParlamentar(row.getNomeParlamentar())
                .uri(row.getUri())
                .titulo(row.getTitulo())
                .dataInicio(row.getDataInicio())
                .dataFim(row.getDataFim())
                .build();
    }
}
