package br.leg.congresso.etl.loader.silver;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraVotacaoVoto;
import br.leg.congresso.etl.extractor.camara.dto.CamaraVotacaoVotoCSVRow;
import br.leg.congresso.etl.repository.silver.SilverCamaraVotacaoVotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega votos individuais de deputados em votações da Câmara na camada Silver
 * (silver.camara_votacao_voto).
 *
 * Princípio Silver: passthrough fiel ao CSV votacoesVotos-{ano}.csv.
 * Deduplicação: (id_votacao, deputado_id).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraVotacoesVotosLoader {

    private final SilverCamaraVotacaoVotoRepository repository;

    @Transactional
    public int carregar(List<CamaraVotacaoVotoCSVRow> rows, UUID jobId) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        Set<String> idsVotacao = rows.stream()
                .map(CamaraVotacaoVotoCSVRow::getIdVotacao)
                .filter(id -> id != null && !id.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        Set<String> chavesExistentes = repository.findAllByIdVotacaoIn(idsVotacao).stream()
                .map(e -> buildKey(e.getIdVotacao(), e.getDeputadoId()))
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        int inseridos = 0;
        for (CamaraVotacaoVotoCSVRow row : rows) {
            if (row.getIdVotacao() == null || row.getIdVotacao().isBlank()
                    || row.getDeputadoId() == null || row.getDeputadoId().isBlank()) {
                continue;
            }

            Integer deputadoId = parseIntOrNull(row.getDeputadoId());
            if (deputadoId == null) {
                continue;
            }

            String chave = buildKey(row.getIdVotacao(), deputadoId);
            if (!chavesExistentes.contains(chave)) {
                SilverCamaraVotacaoVoto entity = rowToEntity(row, jobId, deputadoId);
                if (entity == null) {
                    continue;
                }
                repository.save(entity);
                chavesExistentes.add(chave);
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara votações votos: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    private SilverCamaraVotacaoVoto rowToEntity(CamaraVotacaoVotoCSVRow row, UUID jobId, Integer deputadoId) {
        return SilverCamaraVotacaoVoto.builder()
                .etlJobId(jobId)
                .idVotacao(row.getIdVotacao())
                .uriVotacao(row.getUriVotacao())
                .dataHoraVoto(parseOffsetDateTime(row.getDataHoraVoto()))
                .voto(row.getVoto())
                .deputadoId(deputadoId)
                .deputadoUri(row.getDeputadoUri())
                .deputadoNome(row.getDeputadoNome())
                .deputadoSiglaPartido(row.getDeputadoSiglaPartido())
                .deputadoUriPartido(row.getDeputadoUriPartido())
                .deputadoSiglaUf(row.getDeputadoSiglaUf())
                .deputadoIdLegislatura(parseIntOrNull(row.getDeputadoIdLegislatura()))
                .deputadoUrlFoto(row.getDeputadoUrlFoto())
                .build();
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank())
            return null;
        String v = value.trim();
        try {
            return OffsetDateTime.parse(v);
        } catch (DateTimeParseException e) {
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

    private String buildKey(String idVotacao, Integer deputadoId) {
        return idVotacao + "|" + deputadoId;
    }
}
