package br.leg.congresso.etl.loader.silver;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacao;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraVotacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega votações da Câmara dos Deputados na camada Silver
 * (silver.camara_votacao).
 *
 * Princípio Silver: passthrough fiel ao CSV votacoes-{ano}.csv — sem
 * normalização.
 * Deduplicação: votacao_id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraVotacoesLoader {

    private final SilverCamaraVotacaoRepository repository;

    @Transactional
    public int carregar(List<CamaraVotacaoCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        Set<String> idsLote = rows.stream()
                .map(CamaraVotacaoCSVRow::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        Set<String> idsExistentes = new HashSet<>(repository.findAllByVotacaoIdIn(idsLote).stream()
                .map(SilverCamaraVotacao::getVotacaoId)
                .toList());

        int inseridos = 0;
        for (CamaraVotacaoCSVRow row : rows) {
            if (row.getId() == null || row.getId().isBlank()) {
                continue;
            }

            if (!idsExistentes.contains(row.getId())) {
                SilverCamaraVotacao entity = rowToEntity(row, jobId);
                if (entity == null) {
                    continue;
                }
                repository.save(entity);
                idsExistentes.add(row.getId());
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara votações: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraVotacao rowToEntity(CamaraVotacaoCSVRow row, UUID jobId) {
        return SilverCamaraVotacao.builder()
                .etlJobId(jobId)
                .votacaoId(row.getId())
                .uri(row.getUri())
                .data(parseDate(row.getData()))
                .dataHoraRegistro(parseOffsetDateTime(row.getDataHoraRegistro()))
                .idOrgao(parseIntOrNull(row.getIdOrgao()))
                .uriOrgao(row.getUriOrgao())
                .siglaOrgao(row.getSiglaOrgao())
                .idEvento(parseIntOrNull(row.getIdEvento()))
                .uriEvento(row.getUriEvento())
                .aprovacao(parseShortOrNull(row.getAprovacao()))
                .votosSim(parseIntOrNull(row.getVotosSim()))
                .votosNao(parseIntOrNull(row.getVotosNao()))
                .votosOutros(parseIntOrNull(row.getVotosOutros()))
                .descricao(row.getDescricao())
                .ultimaAberturaVotacaoDataHoraRegistro(
                        parseOffsetDateTime(row.getUltimaAberturaVotacaoDataHoraRegistro()))
                .ultimaAberturaVotacaoDescricao(row.getUltimaAberturaVotacaoDescricao())
                .ultimaApresentacaoProposicaoDataHoraRegistro(
                        parseOffsetDateTime(row.getUltimaApresentacaoProposicaoDataHoraRegistro()))
                .ultimaApresentacaoProposicaoDescricao(row.getUltimaApresentacaoProposicaoDescricao())
                .ultimaApresentacaoProposicaoIdProposicao(
                        parseIntOrNull(row.getUltimaApresentacaoProposicaoIdProposicao()))
                .ultimaApresentacaoProposicaoUriProposicao(
                        row.getUltimaApresentacaoProposicaoUriProposicao())
                .build();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length())));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank())
            return null;
        String v = value.trim();
        try {
            return OffsetDateTime.parse(v);
        } catch (DateTimeParseException e) {
            // tenta sem offset (assume UTC)
            try {
                return OffsetDateTime.parse(v + "Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Short parseShortOrNull(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return Short.parseShort(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
