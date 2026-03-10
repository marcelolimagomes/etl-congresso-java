package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTOs para os endpoints de listagem de senadores do Senado Federal.
 * GET /senador/lista/atual.json
 * GET /senador/lista/legislatura/{leg}.json
 *
 * Estrutura do envelope JSON (PascalCase):
 * ListaParlamentarEmExercicio → Parlamentares → Parlamentar []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorListaDTO {

    @JsonProperty("ListaParlamentarEmExercicio")
    private ListaParlamentarEmExercicio listaParlamentarEmExercicio;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListaParlamentarEmExercicio {
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

        @JsonProperty("Mandato")
        private Mandato mandato;
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

        @JsonProperty("SexoParlamentar")
        private String sexoParlamentar;

        @JsonProperty("UfParlamentar")
        private String ufParlamentar;

        @JsonProperty("SiglaPartidoParlamentar")
        private String siglaPartidoParlamentar;

        @JsonProperty("UrlFotoParlamentar")
        private String urlFotoParlamentar;

        @JsonProperty("UrlPaginaParlamentar")
        private String urlPaginaParlamentar;

        @JsonProperty("EmailParlamentar")
        private String emailParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mandato {
        @JsonProperty("CodigoMandato")
        private String codigoMandato;

        @JsonProperty("DescricaoParticipacao")
        private String descricaoParticipacao;

        @JsonProperty("DataDesignacao")
        private String dataDesignacao;

        @JsonProperty("CodigoLegislatura")
        private String codigoLegislatura;
    }
}
