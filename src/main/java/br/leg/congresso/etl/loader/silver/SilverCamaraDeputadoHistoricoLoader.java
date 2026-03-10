package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoHistorico;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoHistoricoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoHistoricoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega histórico de deputados da Câmara na camada Silver
 * (silver.camara_deputado_historico).
 *
 * Princípio Silver: passthrough fiel à API /deputados/{id}/historico.
 * Deduplicação por (camara_deputado_id, data_hora, id_legislatura). Registros
 * já existentes são ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoHistoricoLoader {

    private final SilverCamaraDeputadoHistoricoRepository repository;

    /**
     * Persiste entradas de histórico, ignorando as já existentes.
     *
     * @param camaraDeputadoId ID do deputado
     * @param dtos             lista de DTOs retornados pela API
     * @param jobId            UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(String camaraDeputadoId, List<CamaraDeputadoHistoricoDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoHistoricoDTO dto : dtos) {
            if (dto.getDataHora() == null || dto.getDataHora().isBlank()) {
                continue;
            }

            String idLegStr = dto.getIdLegislatura() != null ? String.valueOf(dto.getIdLegislatura()) : null;

            if (!repository.existsByCamaraDeputadoIdAndDataHoraAndIdLegislatura(
                    camaraDeputadoId, dto.getDataHora(), idLegStr)) {
                repository.save(dtoToEntity(camaraDeputadoId, dto, idLegStr, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara histórico (dep={}): {} novos registros inseridos", camaraDeputadoId, inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoHistorico dtoToEntity(
            String camaraDeputadoId, CamaraDeputadoHistoricoDTO dto, String idLegStr, UUID jobId) {
        return SilverCamaraDeputadoHistorico.builder()
                .etlJobId(jobId)
                .camaraDeputadoId(camaraDeputadoId)
                .camaraIdRegistro(dto.getId())
                .idLegislatura(idLegStr)
                .nome(dto.getNome())
                .nomeEleitoral(dto.getNomeEleitoral())
                .email(dto.getEmail())
                .siglaPartido(dto.getSiglaPartido())
                .siglaUf(dto.getSiglaUf())
                .situacao(dto.getSituacao())
                .condicaoEleitoral(dto.getCondicaoEleitoral())
                .descricaoStatus(dto.getDescricaoStatus())
                .dataHora(dto.getDataHora())
                .uriPartido(dto.getUriPartido())
                .urlFoto(dto.getUrlFoto())
                .build();
    }
}
