package br.leg.congresso.etl.pagegen.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeputadoFrenteDTO {
    String titulo;
    String legislatura;
    String url;
}