package br.leg.congresso.etl.loader.silver;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.domain.silver.SilverSenadoRelatoria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoRelatoriaDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoRelatoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega relatorias de matérias do Senado na camada Silver
 * (silver.senado_relatoria).
 *
 * Princípio Silver: passthrough fiel ao payload de
 * GET /dadosabertos/processo/relatoria?codigoMateria={codigo} — sem
 * normalização.
 *
 * Deduplicação: chave composta (senado_materia_id, id_relatoria).
 * Relatorias já existentes são ignoradas (imutáveis após registro).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoRelatoriaLoader {

    private final SilverSenadoRelatoriaRepository repository;

    /**
     * Persiste as relatorias de uma matéria Silver, ignorando as já existentes.
     *
     * @param materia    entidade Silver da matéria
     * @param relatorias lista de relatorias retornadas pela API do Senado
     * @return número de relatorias efetivamente inseridas
     */
    @Transactional
    public int carregar(SilverSenadoMateria materia, List<SenadoRelatoriaDTO> relatorias) {
        if (materia == null || relatorias == null || relatorias.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoRelatoriaDTO dto : relatorias) {
            if (dto.getId() == null) {
                continue;
            }

            boolean jaExiste = repository.existsBySenadoMateriaIdAndIdRelatoria(
                    materia.getId(), dto.getId());

            if (!jaExiste) {
                repository.save(dtoToEntity(materia, dto));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado relatoria: {} novas relatorias para codigo={}", inseridos, materia.getCodigo());
        }

        return inseridos;
    }

    private SilverSenadoRelatoria dtoToEntity(SilverSenadoMateria materia, SenadoRelatoriaDTO dto) {
        return SilverSenadoRelatoria.builder()
                .senadoMateria(materia)
                .idRelatoria(dto.getId())
                .casaRelator(dto.getCasaRelator())
                .idTipoRelator(dto.getIdTipoRelator())
                .descricaoTipoRelator(dto.getDescricaoTipoRelator())
                .dataDesignacao(dto.getDataDesignacao())
                .dataDestituicao(dto.getDataDestituicao())
                .descricaoTipoEncerramento(dto.getDescricaoTipoEncerramento())
                .idProcesso(dto.getIdProcesso())
                .identificacaoProcesso(dto.getIdentificacaoProcesso())
                .tramitando(dto.getTramitando())
                .codigoParlamentar(dto.getCodigoParlamentar())
                .nomeParlamentar(dto.getNomeParlamentar())
                .nomeCompleto(dto.getNomeCompleto())
                .sexoParlamentar(dto.getSexoParlamentar())
                .formaTratamentoParlamentar(dto.getFormaTratamentoParlamentar())
                .siglaPartidoParlamentar(dto.getSiglaPartidoParlamentar())
                .ufParlamentar(dto.getUfParlamentar())
                .codigoColegiado(dto.getCodigoColegiado())
                .siglaCasa(dto.getSiglaCasa())
                .siglaColegiado(dto.getSiglaColegiado())
                .nomeColegiado(dto.getNomeColegiado())
                .codigoTipoColegiado(dto.getCodigoTipoColegiado())
                .build();
    }
}
