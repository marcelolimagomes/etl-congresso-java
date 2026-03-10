package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para o endpoint
 * {@code GET /dadosabertos/processo/prazo?codigoMateria={codigo}}.
 * Retorna array JSON diretamente (sem objeto wrapper).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoPrazoDTO {

    private String tipoPrazo;
    private String dataInicio;
    private String dataFim;
    private String descricao;
    private String colegiado;
    private String situacao;
}
