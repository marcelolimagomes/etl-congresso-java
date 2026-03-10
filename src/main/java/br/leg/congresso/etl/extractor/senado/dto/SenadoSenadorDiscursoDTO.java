package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para discursos de um senador.
 * GET /senador/{codigo}/discursos.json
 *
 * Envelope: DiscursosParlamentar → Parlamentar → Pronunciamentos →
 * Pronunciamento []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorDiscursoDTO {

    @JsonProperty("DiscursosParlamentar")
    private DiscursosParlamentar discursosParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DiscursosParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("Pronunciamentos")
        private Pronunciamentos pronunciamentos;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pronunciamentos {
        @JsonProperty("Pronunciamento")
        private List<Pronunciamento> pronunciamento;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pronunciamento {
        @JsonProperty("CodigoPronunciamento")
        private String codigoPronunciamento;

        @JsonProperty("CodigoSessao")
        private String codigoSessao;

        @JsonProperty("DataPronunciamento")
        private String dataPronunciamento;

        @JsonProperty("Casa")
        private String casa;

        @JsonProperty("TipoSessao")
        private String tipoSessao;

        @JsonProperty("NumeroSessao")
        private String numeroSessao;

        @JsonProperty("TipoPronunciamento")
        private String tipoPronunciamento;

        @JsonProperty("TextoPronunciamento")
        private String textoPronunciamento;

        @JsonProperty("DuracaoAparte")
        private String duracaoAparte;

        @JsonProperty("UrlVideo")
        private String urlVideo;

        @JsonProperty("UrlAudio")
        private String urlAudio;
    }
}
