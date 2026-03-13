package br.leg.congresso.etl.pagegen.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DespesaResumoDTO {
    int totalRegistros;
    String valorTotalFormatado;
    String anosCobertos;
    String principalTipoDespesa;
    int totalTiposDespesa;
}