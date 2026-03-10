package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
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
        /**
         * Contém os campos de identificação: SiglaCasaIdentificacaoMateria,
         * SiglaSubtipoMateria, DescricaoSubtipoMateria, DescricaoObjetivoProcesso
         * e IndicadorTramitando.
         */
        @JsonProperty("IdentificacaoMateria")
        private IdentificacaoMateria identificacaoMateria;

        @JsonProperty("DadosBasicosMateria")
        private DadosBasicos dadosBasicosMateria;

        @JsonProperty("Classificacoes")
        private Classificacoes classificacoes;

        @JsonProperty("OutrasInformacoes")
        private OutrasInformacoes outrasInformacoes;

        @JsonProperty("NaturezaMateria")
        private Natureza naturezaMateria;

        @JsonProperty("CasaOrigem")
        private CasaOrigem casaOrigem;

        @JsonProperty("OrigemMateria")
        private CasaOrigem origemMateria;
    }

    /**
     * Campos do nó IdentificacaoMateria (irmão de DadosBasicosMateria na resposta
     * da API do Senado).
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoMateria {
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
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DadosBasicos {
        /** Palavras-chave separadas por ";" */
        @JsonProperty("IndexacaoMateria")
        private String indexacao;

        /** Sigla da casa iniciadora, quando disponível */
        @JsonProperty("SiglaCasaIniciadora")
        @JsonAlias("CasaIniciadoraNoLegislativo")
        private String siglaCasaIniciadora;

        /** Nome descritivo da casa iniciadora */
        @JsonProperty("NomeCasaIniciadora")
        private String nomeCasaIniciadora;

        /** "Sim" ou "Não" */
        @JsonProperty("IndicadorComplementar")
        private String indicadorComplementar;

        @JsonProperty("NaturezaMateria")
        private Natureza naturezaMateria;
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Classificacoes {
        @JsonProperty("Classificacao")
        private List<Classificacao> classificacao;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Classificacao {
        @JsonProperty("CodigoClasse")
        private String codigoClasse;

        @JsonProperty("DescricaoClasse")
        private String descricaoClasse;

        @JsonProperty("DescricaoClasseHierarquica")
        private String descricaoClasseHierarquica;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutrasInformacoes {
        @JsonProperty("Servico")
        private List<Servico> servico;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Servico {
        @JsonProperty("NomeServico")
        private String nomeServico;

        @JsonProperty("DescricaoServico")
        private String descricaoServico;

        @JsonProperty("UrlServico")
        private String urlServico;
    }
}
