package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraProposicaoAutor;
import br.leg.congresso.etl.extractor.camara.dto.CamaraProposicaoAutorCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraProposicaoAutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega autores de proposições da Câmara na camada Silver
 * (silver.camara_proposicao_autor).
 *
 * Princípio Silver: passthrough fiel ao CSV proposicoesAutores-{ano}.csv —
 * sem normalização.
 *
 * Deduplicação: chave composta (uri_proposicao, nome_autor, ordem_assinatura).
 * Registros já existentes são ignorados (idempotente).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraAutoresLoader {

    private final SilverCamaraProposicaoAutorRepository repository;

    /**
     * Persiste os autores de proposições, ignorando os já existentes.
     *
     * @param rows  lista de linhas CSV do arquivo proposicoesAutores-{ano}.csv
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<CamaraProposicaoAutorCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraProposicaoAutorCSVRow row : rows) {
            if (row.getUriProposicao() == null || row.getUriProposicao().isBlank()
                    || row.getNomeAutor() == null || row.getNomeAutor().isBlank()) {
                continue;
            }

            Integer ordemAssinatura = parseIntOrNull(row.getOrdemAssinatura());

            boolean jaExiste = repository.existsByUriProposicaoAndNomeAutorAndOrdemAssinatura(
                    row.getUriProposicao(), row.getNomeAutor(), ordemAssinatura);

            if (!jaExiste) {
                repository.save(rowToEntity(row, jobId, ordemAssinatura));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara autores: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraProposicaoAutor rowToEntity(CamaraProposicaoAutorCSVRow row,
            UUID jobId,
            Integer ordemAssinatura) {
        return SilverCamaraProposicaoAutor.builder()
                .etlJobId(jobId)
                .idProposicao(row.getIdProposicao())
                .uriProposicao(row.getUriProposicao())
                .idDeputadoAutor(row.getIdDeputadoAutor())
                .uriAutor(row.getUriAutor())
                .codTipoAutor(row.getCodTipoAutor())
                .tipoAutor(row.getTipoAutor())
                .nomeAutor(row.getNomeAutor())
                .siglaPartidoAutor(row.getSiglaPartidoAutor())
                .uriPartidoAutor(row.getUriPartidoAutor())
                .siglaUfAutor(row.getSiglaUfAutor())
                .ordemAssinatura(ordemAssinatura)
                .proponente(parseIntOrNull(row.getProponente()))
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
