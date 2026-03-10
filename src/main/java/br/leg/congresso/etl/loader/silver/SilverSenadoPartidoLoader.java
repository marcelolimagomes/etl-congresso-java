package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoPartido;
import br.leg.congresso.etl.extractor.senado.dto.SenadoPartidoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoPartidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega partidos do Senado Federal na camada Silver (silver.senado_partido).
 * Deduplicação por codigo_partido.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoPartidoLoader {

    private final SilverSenadoPartidoRepository repository;

    /**
     * Insere partidos inéditos; ignora os já existentes.
     *
     * @param partidos lista de partidos da API /senador/partidos
     * @param jobId    UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<SenadoPartidoDTO.Partido> partidos, UUID jobId) {
        if (partidos == null || partidos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoPartidoDTO.Partido partido : partidos) {
            if (partido.getCodigoPartido() == null || partido.getCodigoPartido().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoPartido(partido.getCodigoPartido())) {
                repository.save(toEntity(partido, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado partidos: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    // ── Conversão ─────────────────────────────────────────────────────────────

    private SilverSenadoPartido toEntity(SenadoPartidoDTO.Partido partido, UUID jobId) {
        String dataAtivacao = partido.getDataAtivacao() != null
                ? partido.getDataAtivacao()
                : partido.getDataCriacao();

        String dataDesativacao = partido.getDataDesativacao() != null
                ? partido.getDataDesativacao()
                : partido.getDataExtincao();

        return SilverSenadoPartido.builder()
                .etlJobId(jobId)
                .codigoPartido(partido.getCodigoPartido())
                .siglaPartido(partido.getSiglaPartido())
                .nomePartido(partido.getNomePartido())
                .dataAtivacao(dataAtivacao)
                .dataDesativacao(dataDesativacao)
                .build();
    }
}
