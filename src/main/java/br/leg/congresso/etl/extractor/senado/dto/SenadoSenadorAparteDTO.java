package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para apartes de um senador.
 * GET /senador/{codigo}/apartes.json
 *
 * Envelope: ApartesParlamentar → Parlamentar → Apartes → Aparte []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorAparteDTO {

    @JsonProperty("ApartesParlamentar")
    private ApartesParlamentar apartesParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApartesParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("Apartes")
        private Apartes apartes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Apartes {
        @JsonProperty("Aparte")
        private List<Aparte> aparte;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aparte {
        @JsonProperty("CodigoAparte")
        private String codigoAparte;

        @JsonProperty("CodigoPronunciamentoPrincipal")
        private String codigoPronunciamentoPrincipal;

        @JsonProperty("CodigoSessao")
        private String codigoSessao;

        @JsonProperty("DataPronunciamento")
        private String dataPronunciamento;

        @JsonProperty("Casa")
        private String casa;

        @JsonProperty("TextoAparte")
        private String textoAparte;

        @JsonProperty("UrlVideo")
        private String urlVideo;
    }
}
