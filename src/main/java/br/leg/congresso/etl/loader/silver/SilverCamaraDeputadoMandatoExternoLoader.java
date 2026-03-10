package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoMandatoExterno;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoMandatoExternoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoMandatoExternoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega mandatos externos de deputados da Câmara na camada Silver
 * (silver.camara_deputado_mandato_externo).
 *
 * Princípio Silver: passthrough fiel à API /deputados/{id}/mandatosExternos.
 * Deduplicação por (camara_deputado_id, cargo, sigla_uf, ano_inicio). Registros
 * já existentes são ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoMandatoExternoLoader {

    private final SilverCamaraDeputadoMandatoExternoRepository repository;

    /**
     * Persiste mandatos externos, ignorando os já existentes.
     *
     * @param camaraDeputadoId ID do deputado
     * @param dtos             lista de DTOs retornados pela API
     * @param jobId            UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(String camaraDeputadoId, List<CamaraDeputadoMandatoExternoDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoMandatoExternoDTO dto : dtos) {
            if (dto.getCargo() == null || dto.getCargo().isBlank()) {
                continue;
            }

            if (!repository.existsByCamaraDeputadoIdAndCargoAndSiglaUfAndAnoInicio(
                    camaraDeputadoId, dto.getCargo(), dto.getSiglaUf(), dto.getAnoInicio())) {
                repository.save(dtoToEntity(camaraDeputadoId, dto, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara mandatos externos (dep={}): {} novos registros inseridos",
                    camaraDeputadoId, inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoMandatoExterno dtoToEntity(
            String camaraDeputadoId, CamaraDeputadoMandatoExternoDTO dto, UUID jobId) {
        return SilverCamaraDeputadoMandatoExterno.builder()
                .etlJobId(jobId)
                .camaraDeputadoId(camaraDeputadoId)
                .anoInicio(dto.getAnoInicio())
                .anoFim(dto.getAnoFim())
                .cargo(dto.getCargo())
                .siglaUf(dto.getSiglaUf())
                .municipio(dto.getMunicipio())
                .siglaPartidoEleicao(dto.getSiglaPartidoEleicao())
                .uriPartidoEleicao(dto.getUriPartidoEleicao())
                .build();
    }
}
