package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoOcupacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoOcupacaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoOcupacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega ocupações profissionais de deputados da Câmara na camada Silver
 * (silver.camara_deputado_ocupacao).
 *
 * Princípio Silver: passthrough fiel ao CSV deputadosOcupacoes.csv.
 * Deduplicação por (id_deputado, titulo, ano_inicio, entidade).
 * Registros já existentes são ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoOcupacaoLoader {

    private final SilverCamaraDeputadoOcupacaoRepository repository;

    /**
     * Persiste ocupações de deputados, ignorando as já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo deputadosOcupacoes.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraDeputadoOcupacaoCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoOcupacaoCSVRow row : rows) {
            if (row.getIdDeputado() == null || row.getIdDeputado().isBlank()
                    || row.getTitulo() == null || row.getTitulo().isBlank()) {
                continue;
            }

            if (!repository.existsByIdDeputadoAndTituloAndAnoInicio(
                    row.getIdDeputado(), row.getTitulo(),
                    row.getAnoInicio())) {
                repository.save(rowToEntity(row, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara ocupações deputado: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoOcupacao rowToEntity(CamaraDeputadoOcupacaoCSVRow row, UUID jobId) {
        return SilverCamaraDeputadoOcupacao.builder()
                .etlJobId(jobId)
                .idDeputado(row.getIdDeputado())
                .titulo(row.getTitulo())
                .anoInicio(row.getAnoInicio())
                .anoFim(row.getAnoFim())
                .entidade(row.getEntidade())
                .entidadeUF(row.getEntidadeUF())
                .entidadePais(row.getEntidadePais())
                .build();
    }
}
