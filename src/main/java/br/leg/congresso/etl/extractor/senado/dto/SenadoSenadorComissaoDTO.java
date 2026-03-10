package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * DTO para comissões de um senador.
 * GET /senador/{codigo}/comissoes.json
 *
 * Envelope: MembroComissaoParlamentar → Parlamentar → Comissoes → Comissao []
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoSenadorComissaoDTO {

    @JsonProperty("MembroComissaoParlamentar")
    private MembroComissaoParlamentar membroComissaoParlamentar;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MembroComissaoParlamentar {
        @JsonProperty("Parlamentar")
        private Parlamentar parlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parlamentar {
        @JsonProperty("IdentificacaoParlamentar")
        private IdentificacaoParlamentar identificacaoParlamentar;

        @JsonProperty("MembroComissoes")
        private MembroComissoes membroComissoes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoParlamentar {
        @JsonProperty("CodigoParlamentar")
        private String codigoParlamentar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MembroComissoes {
        @JsonProperty("Comissao")
        private List<Comissao> comissao;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Comissao {
        @JsonProperty("CodigoComissao")
        private String codigoComissao;

        @JsonProperty("SiglaComissao")
        private String siglaComissao;

        @JsonProperty("NomeComissao")
        private String nomeComissao;

        @JsonProperty("DescricaoParticipacao")
        private String descricaoParticipacao;

        @JsonProperty("DataInicio")
        private String dataInicio;

        @JsonProperty("DataFim")
        private String dataFim;

        @JsonProperty("IndicadorAtividade")
        private String indicadorAtividade;
    }
}
