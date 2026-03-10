package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para o endpoint de detalhe do senador.
 * GET /senador/{codigo}.json
 *
 * Envelope: DetalheParlamentar → Parlamentar
 * Usado para preencher as colunas det_* em silver.senado_senador.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorDetalheDTO {

    @JsonProperty("DetalheParlamentar")
    private DetalheParlamentar detalheParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetalheParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("DadosBasicosParlamentar")
        private DadosBasicosParlamentar dadosBasicosParlamentar;

        @JsonProperty("OutrasInformacoes")
        private OutrasInformacoes outrasInformacoes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;

        @JsonProperty("NomeParlamentar")
        private String nomeParlamentar;

        @JsonProperty("NomeCompletoParlamentar")
        private String nomeCompletoParlamentar;

        @JsonProperty("UrlFotoParlamentar")
        private String urlFotoParlamentar;

        @JsonProperty("UrlPaginaParlamentar")
        private String urlPaginaParlamentar;

        @JsonProperty("UrlPaginaParticular")
        private String urlPaginaParticular;

        @JsonProperty("EmailParlamentar")
        private String emailParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DadosBasicosParlamentar {
        @JsonProperty("DataNascimento")
        private String dataNascimento;

        @JsonProperty("Naturalidade")
        private String naturalidade;

        @JsonProperty("UfNaturalidade")
        private String ufNaturalidade;

        @JsonProperty("EstadoCivil")
        private String estadoCivil;

        @JsonProperty("Escolaridade")
        private String escolaridade;
    }

    /** Informações adicionais: redes sociais, etc. */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutrasInformacoes {
        @JsonProperty("Facebook")
        private String facebook;

        @JsonProperty("Twitter")
        private String twitter;
    }
}
