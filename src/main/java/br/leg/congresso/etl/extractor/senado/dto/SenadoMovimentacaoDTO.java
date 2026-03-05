package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO para movimentações (tramitações) de matéria do Senado Federal.
 * GET /dadosabertos/materia/{id}/movimentacoes
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoMovimentacaoDTO {

    @JsonProperty("MovimentacoesMateriaResponse")
    private MovimentacoesResponse movimentacoesResponse;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MovimentacoesResponse {
        @JsonProperty("Materia")
        private MateriaMovimentacoes materia;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MateriaMovimentacoes {
        @JsonProperty("Movimentacoes")
        private MovimentacoesWrapper movimentacoes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MovimentacoesWrapper {
        @JsonProperty("Movimentacao")
        private List<Movimentacao> movimentacao;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Movimentacao {
        @JsonProperty("SequenciaMovimentacao")
        private String sequenciaMovimentacao;

        @JsonProperty("DataMovimentacao")
        private String dataMovimentacao;

        @JsonProperty("DescricaoMovimentacao")
        private String descricaoMovimentacao;

        @JsonProperty("DescricaoSituacao")
        private String descricaoSituacao;

        @JsonProperty("Despacho")
        private String despacho;

        @JsonProperty("Ambito")
        private String ambito;

        @JsonProperty("Local")
        private Local local;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Local {
        @JsonProperty("SiglaLocal")
        private String siglaLocal;

        @JsonProperty("NomeLocal")
        private String nomeLocal;
    }
}
