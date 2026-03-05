package br.leg.congresso.etl.admin.dto;

import br.leg.congresso.etl.domain.EtlErrorLog;
import br.leg.congresso.etl.domain.enums.CasaLegislativa;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para um registro de erro de um job ETL.
 */
public record EtlErrorResponse(
        UUID id,
        UUID jobId,
        CasaLegislativa casa,
        String tipoErro,
        String endpoint,
        String mensagem,
        int tentativas,
        LocalDateTime criadoEm
) {
    public static EtlErrorResponse from(EtlErrorLog log) {
        return new EtlErrorResponse(
            log.getId(),
            log.getJob() != null ? log.getJob().getId() : null,
            log.getOrigem(),
            log.getTipoErro(),
            log.getEndpoint(),
            log.getMensagem(),
            log.getTentativas(),
            log.getCriadoEm()
        );
    }
}
