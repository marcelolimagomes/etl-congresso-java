package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para o endpoint de tipos de uso da palavra no Senado.
 * GET /senador/lista/tiposUsoPalavra.json
 *
 * Envelope: TiposUsoPalavra → TipoUsoPalavra []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoTipoUsoPalavraDTO {

    @JsonProperty("TiposUsoPalavra")
    private TiposUsoPalavra tiposUsoPalavra;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TiposUsoPalavra {
        @JsonProperty("TipoUsoPalavra")
        private List<TipoUsoPalavra> tipoUsoPalavra;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TipoUsoPalavra {
        @JsonProperty("Codigo")
        private String codigo;

        @JsonProperty("Descricao")
        private String descricao;

        @JsonProperty("Sigla")
        private String sigla;

        @JsonProperty("IndicadorAtivo")
        private String indicadorAtivo;
    }
}
