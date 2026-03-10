package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para profissões de um senador.
 * GET /senador/{codigo}/profissao.json
 *
 * Envelope: ProfissaosParlamentar → Parlamentar → Profissoes → Profissao []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorProfissaoDTO {

    @JsonProperty("ProfissaosParlamentar")
    private ProfissaosParlamentar profissaosParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfissaosParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("Profissoes")
        private Profissoes profissoes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profissoes {
        @JsonProperty("Profissao")
        private List<Profissao> profissao;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profissao {
        @JsonProperty("CodigoProfissao")
        private String codigoProfissao;

        @JsonProperty("DescricaoProfissao")
        private String descricaoProfissao;

        @JsonProperty("DataRegistro")
        private String dataRegistro;
    }
}
