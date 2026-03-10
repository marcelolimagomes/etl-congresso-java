package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoTipoUsoPalavra;
import br.leg.congresso.etl.extractor.senado.dto.SenadoTipoUsoPalavraDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoTipoUsoPalavraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega tipos de uso de palavra do Senado Federal na camada Silver
 * (silver.senado_tipo_uso_palavra).
 * Deduplicação por codigo_tipo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoTipoUsoPalavraLoader {

    private final SilverSenadoTipoUsoPalavraRepository repository;

    /**
     * Insere tipos inéditos; ignora os já existentes.
     *
     * @param tipos lista de tipos da API /senador/lista/tiposUsoPalavra
     * @param jobId UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(List<SenadoTipoUsoPalavraDTO.TipoUsoPalavra> tipos, UUID jobId) {
        if (tipos == null || tipos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoTipoUsoPalavraDTO.TipoUsoPalavra tipo : tipos) {
            if (tipo.getCodigo() == null || tipo.getCodigo().isBlank()) {
                continue;
            }

            if (!repository.existsByCodigoTipo(tipo.getCodigo())) {
                repository.save(toEntity(tipo, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado tipos de uso de palavra: {} novos registros inseridos", inseridos);
        }

        return inseridos;
    }

    // ── Conversão ─────────────────────────────────────────────────────────────

    private SilverSenadoTipoUsoPalavra toEntity(SenadoTipoUsoPalavraDTO.TipoUsoPalavra tipo, UUID jobId) {
        return SilverSenadoTipoUsoPalavra.builder()
                .etlJobId(jobId)
                .codigoTipo(tipo.getCodigo())
                .descricaoTipo(tipo.getDescricao())
                .abreviatura(tipo.getSigla())
                .build();
    }
}
