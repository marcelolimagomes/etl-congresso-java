package br.leg.congresso.etl.admin.dto;

import br.leg.congresso.etl.domain.EtlJobControl;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import br.leg.congresso.etl.domain.enums.StatusEtl;
import br.leg.congresso.etl.domain.enums.TipoExecucao;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta com informações de um job ETL.
 */
public record EtlStatusResponse(
        UUID id,
        CasaLegislativa casa,
        TipoExecucao tipoExecucao,
        StatusEtl status,
        LocalDateTime inicio,
        LocalDateTime fim,
        int totalProcessado,
        int totalInserido,
        int totalAtualizado,
        int totalIgnorados,
        int totalErros,
        String mensagemErro
) {
    public static EtlStatusResponse from(EtlJobControl job) {
        return new EtlStatusResponse(
            job.getId(),
            job.getOrigem(),
            job.getTipoExecucao(),
            job.getStatus(),
            job.getIniciadoEm(),
            job.getFinalizadoEm(),
            job.getTotalProcessado(),
            job.getTotalInserido(),
            job.getTotalAtualizado(),
            job.getTotalIgnorados(),
            job.getTotalErros(),
            job.getMensagemErro()
        );
    }
}
