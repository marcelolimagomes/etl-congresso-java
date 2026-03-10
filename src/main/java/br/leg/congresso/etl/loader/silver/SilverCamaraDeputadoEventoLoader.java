package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoEvento;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoEventoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega eventos de deputados da Câmara na camada Silver
 * (silver.camara_deputado_evento).
 *
 * Princípio Silver: passthrough fiel à API /deputados/{id}/eventos.
 * Deduplicação por (camara_deputado_id, id_evento). Registros já existentes são
 * ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoEventoLoader {

    private final SilverCamaraDeputadoEventoRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Persiste eventos, ignorando os já existentes.
     *
     * @param camaraDeputadoId ID do deputado
     * @param dtos             lista de DTOs retornados pela API
     * @param jobId            UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(String camaraDeputadoId, List<CamaraDeputadoEventoDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoEventoDTO dto : dtos) {
            if (dto.getId() == null || dto.getId().isBlank()) {
                continue;
            }

            if (!repository.existsByCamaraDeputadoIdAndIdEvento(camaraDeputadoId, dto.getId())) {
                repository.save(dtoToEntity(camaraDeputadoId, dto, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara eventos (dep={}): {} novos registros inseridos", camaraDeputadoId, inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoEvento dtoToEntity(
            String camaraDeputadoId, CamaraDeputadoEventoDTO dto, UUID jobId) {
        CamaraDeputadoEventoDTO.LocalCamara local = dto.getLocalCamara();
        String orgaosJson = null;
        if (dto.getOrgaos() != null && !dto.getOrgaos().isEmpty()) {
            try {
                orgaosJson = objectMapper.writeValueAsString(dto.getOrgaos());
            } catch (JsonProcessingException e) {
                log.warn("[Silver] Falha ao serializar orgaos para evento {}: {}", dto.getId(), e.getMessage());
            }
        }
        return SilverCamaraDeputadoEvento.builder()
                .etlJobId(jobId)
                .camaraDeputadoId(camaraDeputadoId)
                .idEvento(dto.getId())
                .dataHoraInicio(dto.getDataHoraInicio())
                .dataHoraFim(dto.getDataHoraFim())
                .descricao(dto.getDescricao())
                .descricaoTipo(dto.getDescricaoTipo())
                .situacao(dto.getSituacao())
                .localExterno(dto.getLocalExterno())
                .uri(dto.getUri())
                .urlRegistro(dto.getUrlRegistro())
                .localCamaraNome(local != null ? local.getNome() : null)
                .localCamaraPredio(local != null ? local.getPredio() : null)
                .localCamaraSala(local != null ? local.getSala() : null)
                .localCamaraAndar(local != null ? local.getAndar() : null)
                .orgaos(orgaosJson)
                .build();
    }
}
