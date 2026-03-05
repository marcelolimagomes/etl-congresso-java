package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para o endpoint de detalhe de matéria do Senado Federal.
 * GET /dadosabertos/materia/{codMateria}.json
 *
 * Preserva fidelidade à fonte: campos mapeados com @JsonIgnoreProperties
 * para tolerar evolução da API sem quebrar o pipeline.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoDetalheDTO {

    @JsonProperty("DetalheMateria")
    private DetalheMateria detalheMateria;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetalheMateria {
        @JsonProperty("Materia")
        private MateriaDetalhe materia;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MateriaDetalhe {
        @JsonProperty("DadosBasicosMateria")
        private DadosBasicos dadosBasicosMateria;

        @JsonProperty("NaturezaMateria")
        private Natureza naturezaMateria;

        @JsonProperty("CasaOrigem")
        private CasaOrigem casaOrigem;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DadosBasicos {
        /** Ex: "SF" (Senado Federal) ou "CD" (Câmara dos Deputados) */
        @JsonProperty("SiglaCasaIdentificacaoMateria")
        private String siglaCasaIdentificacao;

        /** Ex: "PL", "PLS", "MPV" */
        @JsonProperty("SiglaSubtipoMateria")
        private String siglaSubtipo;

        /** Ex: "Projeto de Lei" */
        @JsonProperty("DescricaoSubtipoMateria")
        private String descricaoSubtipo;

        @JsonProperty("DescricaoObjetivoProcesso")
        private String descricaoObjetivoProcesso;

        /** "Sim" ou "Não" */
        @JsonProperty("IndicadorTramitando")
        private String indicadorTramitando;

        /** Palavras-chave separadas por ";" */
        @JsonProperty("IndexacaoMateria")
        private String indexacao;

        /** Sigla da casa iniciadora, quando disponível */
        @JsonProperty("SiglaCasaIniciadora")
        private String siglaCasaIniciadora;

        /** Nome descritivo da casa iniciadora */
        @JsonProperty("NomeCasaIniciadora")
        private String nomeCasaIniciadora;

        /** "Sim" ou "Não" */
        @JsonProperty("IndicadorComplementar")
        private String indicadorComplementar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Natureza {
        @JsonProperty("CodigoNatureza")
        private String codigoNatureza;

        @JsonProperty("NomeNatureza")
        private String nomeNatureza;

        @JsonProperty("DescricaoNatureza")
        private String descricaoNatureza;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CasaOrigem {
        @JsonProperty("SiglaCasaOrigem")
        private String siglaCasaOrigem;
    }
}
