package br.leg.congresso.etl.pagegen.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProposicaoResumoDTO {
    String titulo;
    String ementa;
    String situacao;
    String url;
    String casa;
    String casaLabel;
}