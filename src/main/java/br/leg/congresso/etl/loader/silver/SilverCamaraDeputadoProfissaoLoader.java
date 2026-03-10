package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoProfissao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoProfissaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoProfissaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega profissões de deputados da Câmara na camada Silver
 * (silver.camara_deputado_profissao).
 *
 * Princípio Silver: passthrough fiel ao CSV deputadosProfissoes.csv.
 * Deduplicação por (id_deputado, titulo). Registros já existentes são
 * ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoProfissaoLoader {

    private final SilverCamaraDeputadoProfissaoRepository repository;

    /**
     * Persiste profissões de deputados, ignorando as já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo deputadosProfissoes.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraDeputadoProfissaoCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoProfissaoCSVRow row : rows) {
            if (row.getIdDeputado() == null || row.getIdDeputado().isBlank()
                    || row.getTitulo() == null || row.getTitulo().isBlank()) {
                continue;
            }

            if (!repository.existsByIdDeputadoAndTituloAndCodTipoProfissao(row.getIdDeputado(), row.getTitulo(),
                    row.getCodTipoProfissao())) {
                repository.save(rowToEntity(row, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara profissões deputado: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoProfissao rowToEntity(CamaraDeputadoProfissaoCSVRow row, UUID jobId) {
        return SilverCamaraDeputadoProfissao.builder()
                .etlJobId(jobId)
                .idDeputado(row.getIdDeputado())
                .titulo(row.getTitulo())
                .codTipoProfissao(row.getCodTipoProfissao())
                .dataHora(row.getDataHora())
                .build();
    }
}
