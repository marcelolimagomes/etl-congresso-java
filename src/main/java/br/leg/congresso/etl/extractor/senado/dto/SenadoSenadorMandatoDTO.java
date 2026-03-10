package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para mandatos de um senador.
 * GET /senador/{codigo}/mandatos.json
 *
 * Envelope: MandatoParlamentar → Parlamentar → Mandatos → Mandato []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorMandatoDTO {

    @JsonProperty("MandatoParlamentar")
    private MandatoParlamentar mandatoParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MandatoParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("Mandatos")
        private Mandatos mandatos;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mandatos {
        @JsonProperty("Mandato")
        private List<Mandato> mandato;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mandato {
        @JsonProperty("CodigoMandato")
        private String codigoMandato;

        @JsonProperty("DescricaoMandato")
        private String descricaoMandato;

        @JsonProperty("UfParlamentar")
        private String ufParlamentar;

        @JsonProperty("DescricaoParticipacao")
        private String descricaoParticipacao;

        @JsonProperty("DataInicio")
        private String dataInicio;

        @JsonProperty("DataFim")
        private String dataFim;

        @JsonProperty("DataDesignacao")
        private String dataDesignacao;

        @JsonProperty("DataTermino")
        private String dataTermino;

        @JsonProperty("EntrouEmExercicio")
        private String entrouEmExercicio;

        @JsonProperty("DataExercicio")
        private String dataExercicio;
    }
}
