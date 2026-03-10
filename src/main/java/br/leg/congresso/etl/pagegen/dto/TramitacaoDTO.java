package br.leg.congresso.etl.pagegen.dto;

import lombok.Builder;
import lombok.Value;

/**
 * DTO de tramitação para renderização de página estática.
 */
@Value
@Builder
public class TramitacaoDTO {
    String dataFormatada;
    String siglaOrgao;
    String descricaoOrgao;
    String descricao;
    String situacao;
    String despacho;
}
