package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoEmenda;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoEmendaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoEmendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega emendas de matérias do Senado na camada Silver
 * (silver.senado_emenda).
 *
 * Princípio Silver: passthrough fiel ao payload de GET
 * /dadosabertos/processo/emenda?codigoMateria={codigo}.
 * Deduplicação: (senado_materia_id, codigo_emenda).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoEmendaLoader {

    private final SilverSenadoEmendaRepository repository;

    @Transactional
    public int carregar(SilverSenadoMateria materia, List<SenadoEmendaDTO> emendas, UUID jobId) {
        if (materia == null || emendas == null || emendas.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoEmendaDTO dto : emendas) {
            String codigoEmenda = dto.getCodigoEmenda();
            if (codigoEmenda == null || codigoEmenda.isBlank()) {
                continue;
            }

            if (!repository.existsBySenadoMateriaIdAndCodigoEmenda(materia.getId(), codigoEmenda)) {
                repository.save(dtoToEntity(materia, dto, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado emendas: {} novos registros inseridos para codigo={}", inseridos,
                    materia.getCodigo());
        }

        return inseridos;
    }

    private SilverSenadoEmenda dtoToEntity(SilverSenadoMateria materia, SenadoEmendaDTO dto, UUID jobId) {
        return SilverSenadoEmenda.builder()
                .senadoMateria(materia)
                .etlJobId(jobId)
                .codigoMateria(materia.getCodigo())
                .codigoEmenda(dto.getCodigoEmenda())
                .tipoEmenda(dto.getTipoEmenda())
                .descricaoTipoEmenda(dto.getDescricaoTipoEmenda())
                .numeroEmenda(dto.getNumeroEmenda())
                .dataApresentacao(dto.getDataApresentacao())
                .colegiadoApresentacao(dto.getColegiadoApresentacao())
                .turno(dto.getTurno())
                .autorNome(dto.getAutorNome())
                .autorCodigoParlamentar(dto.getAutorCodigoParlamentar())
                .autorTipo(dto.getAutorTipo())
                .ementa(dto.getEmenta())
                .inteiroTeorUrl(dto.getInteiroTeorUrl())
                .build();
    }
}
