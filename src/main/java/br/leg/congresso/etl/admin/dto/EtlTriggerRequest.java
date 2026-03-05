package br.leg.congresso.etl.admin.dto;

import java.time.LocalDate;

/**
 * DTO de requisição para acionar manualmente um job ETL.
 */
public record EtlTriggerRequest(
        Integer anoInicio,
        Integer anoFim,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean forcar
) {
    public EtlTriggerRequest {
        // sem validações obrigatórias — são todos opcionais
    }
}
