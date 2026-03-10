package br.leg.congresso.etl.loader.silver;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoTema;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoTemaCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoTemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega temas de proposições da Câmara na camada Silver
 * (silver.camara_proposicao_tema).
 *
 * Princípio Silver: passthrough fiel ao CSV proposicoesTemas-{ano}.csv —
 * sem normalização.
 *
 * Deduplicação: chave composta (uri_proposicao, cod_tema).
 * Registros já existentes são ignorados (idempotente).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraTemasLoader {

    private final SilverCamaraProposicaoTemaRepository repository;

    /**
     * Persiste os temas de proposições, ignorando os já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo proposicoesTemas-{ano}.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraProposicaoTemaCSVRow> rows, java.util.UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraProposicaoTemaCSVRow row : rows) {
            if (row.getUriProposicao() == null || row.getUriProposicao().isBlank()
                    || row.getCodTema() == null) {
                continue;
            }

            Integer codTema = parseIntOrNull(row.getCodTema());
            if (codTema == null) {
                continue;
            }

            boolean jaExiste = repository.existsByUriProposicaoAndCodTema(
                    row.getUriProposicao(), codTema);

            if (!jaExiste) {
                repository.save(rowToEntity(row, jobId, codTema));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara temas: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraProposicaoTema rowToEntity(CamaraProposicaoTemaCSVRow row,
            java.util.UUID jobId,
            Integer codTema) {
        return SilverCamaraProposicaoTema.builder()
                .etlJobId(jobId)
                .uriProposicao(row.getUriProposicao())
                .siglaTipo(row.getSiglaTipo())
                .numero(parseIntOrNull(row.getNumero()))
                .ano(parseIntOrNull(row.getAno()))
                .codTema(codTema)
                .tema(row.getTema())
                .relevancia(parseIntOrNull(row.getRelevancia()))
                .build();
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
