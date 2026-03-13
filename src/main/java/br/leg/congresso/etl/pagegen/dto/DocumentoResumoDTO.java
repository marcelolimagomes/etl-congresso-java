package br.leg.congresso.etl.pagegen.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentoResumoDTO {
    String titulo;
    String descricao;
    String data;
    String url;
    String origem;
}