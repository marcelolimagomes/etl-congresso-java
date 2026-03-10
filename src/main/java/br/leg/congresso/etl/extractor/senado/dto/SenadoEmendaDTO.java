package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para o endpoint
 * {@code GET /dadosabertos/processo/emenda?codigoMateria={codigo}}.
 * Retorna array JSON diretamente (sem objeto wrapper).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoEmendaDTO {

    private String codigoEmenda;
    private String tipoEmenda;
    private String descricaoTipoEmenda;
    private String numeroEmenda;
    private String dataApresentacao;
    private String colegiadoApresentacao;
    private String turno;
    private String autorNome;
    private String autorCodigoParlamentar;
    private String autorTipo;
    private String ementa;
    private String inteiroTeorUrl;
}
