package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para o endpoint de partidos com representação no Senado.
 * GET /senador/partidos.json
 *
 * Envelope: Partidos → Partido []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoPartidoDTO {

    @JsonProperty("Partidos")
    private Partidos partidos;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Partidos {
        @JsonProperty("Partido")
        private List<Partido> partido;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Partido {
        @JsonProperty("CodigoPartido")
        private String codigoPartido;

        @JsonProperty("SiglaPartido")
        private String siglaPartido;

        @JsonProperty("NomePartido")
        private String nomePartido;

        @JsonProperty("DataAtivacao")
        private String dataAtivacao;

        @JsonProperty("DataDesativacao")
        private String dataDesativacao;

        /** Fallback para APIs que usam DataCriacao/DataExtincao */
        @JsonProperty("DataCriacao")
        private String dataCriacao;

        @JsonProperty("DataExtincao")
        private String dataExtincao;
    }
}
