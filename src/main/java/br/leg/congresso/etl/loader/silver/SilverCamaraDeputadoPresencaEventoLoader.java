package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoPresencaEvento;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoPresencaEventoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoPresencaEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega presenças de deputados em eventos da Câmara na camada Silver
 * (silver.camara_deputado_presenca_evento).
 *
 * Princípio Silver: passthrough fiel ao CSV eventosPresencaDeputados-{ano}.csv.
 * Deduplicação por (id_deputado, id_evento). Registros já existentes são
 * ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoPresencaEventoLoader {

    private final SilverCamaraDeputadoPresencaEventoRepository repository;

    /**
     * Persiste presenças em eventos, ignorando as já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo
     *              eventosPresencaDeputados-{ano}.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraDeputadoPresencaEventoCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoPresencaEventoCSVRow row : rows) {
            if (row.getIdDeputado() == null || row.getIdDeputado().isBlank()
                    || row.getIdEvento() == null || row.getIdEvento().isBlank()) {
                continue;
            }

            if (!repository.existsByIdDeputadoAndIdEvento(row.getIdDeputado(), row.getIdEvento())) {
                repository.save(rowToEntity(row, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara presenças em eventos: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoPresencaEvento rowToEntity(
            CamaraDeputadoPresencaEventoCSVRow row, UUID jobId) {
        return SilverCamaraDeputadoPresencaEvento.builder()
                .etlJobId(jobId)
                .idDeputado(row.getIdDeputado())
                .idEvento(row.getIdEvento())
                .dataHoraInicio(row.getDataHoraInicio())
                .dataHoraFim(row.getDataHoraFim())
                .descricao(row.getDescricao())
                .descricaoTipo(row.getDescricaoTipo())
                .situacao(row.getSituacao())
                .uriEvento(row.getUriEvento())
                .build();
    }
}
