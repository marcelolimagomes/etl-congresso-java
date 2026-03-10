package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverSenadoDocumento;
import br.leg.congresso.etl.domain.silver.SilverSenadoMateria;
import br.leg.congresso.etl.extractor.senado.dto.SenadoDocumentoDTO;
import br.leg.congresso.etl.repository.silver.SilverSenadoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega documentos de matérias do Senado na camada Silver
 * (silver.senado_documento).
 *
 * Princípio Silver: passthrough fiel ao payload de GET
 * /dadosabertos/processo/documento?codigoMateria={codigo}.
 * Deduplicação: (senado_materia_id, codigo_documento).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverSenadoDocumentoLoader {

    private final SilverSenadoDocumentoRepository repository;

    @Transactional
    public int carregar(SilverSenadoMateria materia, List<SenadoDocumentoDTO> documentos, UUID jobId) {
        if (materia == null || documentos == null || documentos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (SenadoDocumentoDTO dto : documentos) {
            String codigoDocumento = dto.getCodigoDocumento();
            if (codigoDocumento == null || codigoDocumento.isBlank()) {
                continue;
            }

            if (!repository.existsBySenadoMateriaIdAndCodigoDocumento(materia.getId(), codigoDocumento)) {
                repository.save(dtoToEntity(materia, dto, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Senado documentos: {} novos registros inseridos para codigo={}", inseridos,
                    materia.getCodigo());
        }

        return inseridos;
    }

    private SilverSenadoDocumento dtoToEntity(SilverSenadoMateria materia, SenadoDocumentoDTO dto, UUID jobId) {
        return SilverSenadoDocumento.builder()
                .senadoMateria(materia)
                .etlJobId(jobId)
                .codigoMateria(materia.getCodigo())
                .codigoDocumento(dto.getCodigoDocumento())
                .tipoDocumento(dto.getTipoDocumento())
                .descricaoTipoDocumento(dto.getDescricaoTipoDocumento())
                .dataDocumento(dto.getDataDocumento())
                .descricaoDocumento(dto.getDescricaoDocumento())
                .urlDocumento(dto.getUrlDocumento())
                .tipoConteudo(dto.getTipoConteudo())
                .autorNome(dto.getAutorNome())
                .autorCodigoParlamentar(dto.getAutorCodigoParlamentar())
                .build();
    }
}
