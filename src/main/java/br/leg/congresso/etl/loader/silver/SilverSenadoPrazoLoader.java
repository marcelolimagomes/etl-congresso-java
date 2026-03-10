package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoPrazo;
import br.leg.congresso.etl.extractor.senado.dto.SenadoPrazoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoPrazoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega prazos de matérias do Senado na camada Silver (silver.senado_prazo).
 *
 * Princípio Silver: passthrough fiel ao payload de GET
 * /dadosabertos/processo/prazo?codigoMateria={codigo}.
 * Deduplicação: (senado_materia_id, tipo_prazo, data_inicio).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoPrazoLoader {

    private final SilverSenadoPrazoRepository repository;

    @Transactional
    public int carregar(SilverSenadoMateria materia, List<SenadoPrazoDTO> prazos, UUID jobId) {
        if (materia == null || prazos == null || prazos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoPrazoDTO dto : prazos) {
            String tipoPrazo = dto.getTipoPrazo();
            String dataInicio = dto.getDataInicio();
            if (tipoPrazo == null || tipoPrazo.isBlank() || dataInicio == null || dataInicio.isBlank()) {
                continue;
            }

            if (!repository.existsBySenadoMateriaIdAndTipoPrazoAndDataInicio(
                    materia.getId(), tipoPrazo, dataInicio)) {
                repository.save(dtoToEntity(materia, dto, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado prazos: {} novos registros inseridos para codigo={}", inseridos,
                    materia.getCodigo());
        }

        return inseridos;
    }

    private SilverSenadoPrazo dtoToEntity(SilverSenadoMateria materia, SenadoPrazoDTO dto, UUID jobId) {
        return SilverSenadoPrazo.builder()
                .senadoMateria(materia)
                .etlJobId(jobId)
                .codigoMateria(materia.getCodigo())
                .tipoPrazo(dto.getTipoPrazo())
                .dataInicio(dto.getDataInicio())
                .dataFim(dto.getDataFim())
                .descricao(dto.getDescricao())
                .colegiado(dto.getColegiado())
                .situacao(dto.getSituacao())
                .build();
    }
}
