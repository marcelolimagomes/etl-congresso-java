package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para o endpoint
 * {@code GET /dadosabertos/processo/documento?codigoMateria={codigo}}.
 * Retorna array JSON diretamente (sem objeto wrapper).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoDocumentoDTO {

    private String codigoDocumento;
    private String tipoDocumento;
    private String descricaoTipoDocumento;
    private String dataDocumento;
    private String descricaoDocumento;
    private String urlDocumento;
    private String tipoConteudo;
    private String autorNome;
    private String autorCodigoParlamentar;
}
