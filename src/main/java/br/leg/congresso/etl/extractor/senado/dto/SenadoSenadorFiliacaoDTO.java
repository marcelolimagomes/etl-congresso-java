package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para filiações partidárias de um senador.
 * GET /senador/{codigo}/filiacoes.json
 *
 * Envelope: FiliacaoParlamentar → Parlamentar → Filiacoes → Filiacao []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorFiliacaoDTO {

    @JsonProperty("FiliacaoParlamentar")
    private FiliacaoParlamentar filiacaoParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FiliacaoParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("Filiacoes")
        private Filiacoes filiacoes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Filiacoes {
        @JsonProperty("Filiacao")
        private List<Filiacao> filiacao;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Filiacao {
        @JsonProperty("CodigoFiliacao")
        private String codigoFiliacao;

        @JsonProperty("Partido")
        private Partido partido;

        @JsonProperty("DataFiliacao")
        private String dataFiliacao;

        @JsonProperty("DataDesfiliacao")
        private String dataDesfiliacao;
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
    }
}
