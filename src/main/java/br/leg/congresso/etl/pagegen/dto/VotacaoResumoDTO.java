package br.leg.congresso.etl.pagegen.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VotacaoResumoDTO {
    String data;
    String orgao;
    String resultado;
    String descricao;
    String placar;
}