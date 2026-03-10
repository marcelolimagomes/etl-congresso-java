package br.leg.congresso.etl.loader.silver;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.leg.congresso.etl.domain.silver.SilverCamaraDeputadoDiscurso;
import br.leg.congresso.etl.extractor.camara.dto.CamaraDeputadoDiscursoDTO;
import br.leg.congresso.etl.repository.silver.SilverCamaraDeputadoDiscursoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Carrega discursos de deputados da Câmara na camada Silver
 * (silver.camara_deputado_discurso).
 *
 * Princípio Silver: passthrough fiel à API /deputados/{id}/discursos.
 * Deduplicação por (camara_deputado_id, data_hora_inicio, tipo_discurso).
 * Registros já existentes são ignorados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SilverCamaraDeputadoDiscursoLoader {

    private final SilverCamaraDeputadoDiscursoRepository repository;

    /**
     * Persiste discursos, ignorando os já existentes.
     *
     * @param camaraDeputadoId ID do deputado
     * @param dtos             lista de DTOs retornados pela API
     * @param jobId            UUID do job ETL corrente
     * @return número de registros efetivamente inseridos
     */
    @Transactional
    public int carregar(String camaraDeputadoId, List<CamaraDeputadoDiscursoDTO> dtos, UUID jobId) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }

        int inseridos = 0;
        for (CamaraDeputadoDiscursoDTO dto : dtos) {
            if (dto.getDataHoraInicio() == null || dto.getDataHoraInicio().isBlank()) {
                continue;
            }

            if (!repository.existsByCamaraDeputadoIdAndDataHoraInicioAndTipoDiscurso(
                    camaraDeputadoId, dto.getDataHoraInicio(), dto.getTipoDiscurso())) {
                repository.save(dtoToEntity(camaraDeputadoId, dto, jobId));
                inseridos++;
            }
        }

        if (inseridos > 0) {
            log.debug("[Silver] Câmara discursos (dep={}): {} novos registros inseridos", camaraDeputadoId, inseridos);
        }

        return inseridos;
    }

    private SilverCamaraDeputadoDiscurso dtoToEntity(
            String camaraDeputadoId, CamaraDeputadoDiscursoDTO dto, UUID jobId) {
        CamaraDeputadoDiscursoDTO.FaseEvento fase = dto.getFaseEvento();
        return SilverCamaraDeputadoDiscurso.builder()
                .etlJobId(jobId)
                .camaraDeputadoId(camaraDeputadoId)
                .dataHoraInicio(dto.getDataHoraInicio())
                .dataHoraFim(dto.getDataHoraFim())
                .tipoDiscurso(dto.getTipoDiscurso())
                .sumario(dto.getSumario())
                .transcricao(dto.getTranscricao())
                .keywords(dto.getKeywords())
                .urlTexto(dto.getUrlTexto())
                .urlAudio(dto.getUrlAudio())
                .urlVideo(dto.getUrlVideo())
                .uriEvento(dto.getUriEvento())
                .faseEventoTitulo(fase != null ? fase.getTitulo() : null)
                .faseEventoDataHoraInicio(fase != null ? fase.getDataHoraInicio() : null)
                .faseEventoDataHoraFim(fase != null ? fase.getDataHoraFim() : null)
                .build();
    }
}
