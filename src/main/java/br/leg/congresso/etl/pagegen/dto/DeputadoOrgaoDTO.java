package br.leg.congresso.etl.pagegen.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeputadoOrgaoDTO {
    String sigla;
    String nome;
    String cargo;
    String periodo;
    String url;
}