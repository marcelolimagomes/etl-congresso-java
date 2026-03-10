package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para o endpoint de senadores afastados do Senado Federal.
 * GET /senador/afastados.json
 *
 * Envelope: AfastamentoParlamentar → Parlamentares → Parlamentar []
 * Cada Parlamentar contém IdentificacaoParlamentar + Afastamentos.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoAfastadoDTO {

    @JsonProperty("AfastamentoParlamentar")
    private AfastamentoParlamentar afastamentoParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AfastamentoParlamentar {
        @JsonProperty("Parlamentares")
        private Parlamentares parlamentares;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentares {
        @JsonProperty("Parlamentar")
        private List<Parlamentar> parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("Afastamentos")
        private Afastamentos afastamentos;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;

        @JsonProperty("NomeParlamentar")
        private String nomeParlamentar;

        @JsonProperty("UfParlamentar")
        private String ufParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Afastamentos {
        @JsonProperty("Afastamento")
        private List<Afastamento> afastamento;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Afastamento {
        @JsonProperty("DescricaoCausaAfastamento")
        private String descricaoCausaAfastamento;

        @JsonProperty("DataAfastamento")
        private String dataAfastamento;

        @JsonProperty("DataTerminoAfastamento")
        private String dataTerminoAfastamento;
    }
}
