package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO para matéria legislativa retornada pela API do Senado Federal.
 * Estrutura baseada em GET /dadosabertos/materia/pesquisa/lista
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoMateriaDTO {

    @JsonProperty("PesquisaBasicaMateria")
    private PesquisaBasicaResponse pesquisaBasicaResponse;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PesquisaBasicaResponse {
        @JsonProperty("Materias")
        private MateriasWrapper materias;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MateriasWrapper {
        @JsonProperty("Materia")
        private List<Materia> materia;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Materia {
        @JsonAlias({"Codigo", "CodigoMateria"})
        @JsonProperty("CodigoMateria")
        private String codigoMateria;

        @JsonAlias({"Sigla", "SiglaSubtipoMateria"})
        @JsonProperty("SiglaSubtipoMateria")
        private String siglaSubtipoMateria;

        @JsonAlias({"Numero", "NumeroMateria"})
        @JsonProperty("NumeroMateria")
        private String numeroMateria;

        @JsonAlias({"Ano", "AnoMateria"})
        @JsonProperty("AnoMateria")
        private String anoMateria;

        @JsonAlias({"Data", "DataApresentacao"})
        @JsonProperty("DataApresentacao")
        private String dataApresentacao;

        @JsonAlias({"Ementa", "EmentaMateria"})
        @JsonProperty("EmentaMateria")
        private String ementaMateria;

        @JsonProperty("ExplicacaoEmentaMateria")
        private String explicacaoEmentaMateria;

        @JsonProperty("SituacaoAtualMateria")
        private SituacaoAtualMateria situacaoAtualMateria;

        @JsonProperty("IdentificacaoMateria")
        private IdentificacaoMateria identificacaoMateria;

        @JsonProperty("DataUltimaAtualizacao")
        private String dataUltimaAtualizacao;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SituacaoAtualMateria {
        @JsonProperty("DescricaoSituacao")
        private String descricaoSituacao;

        @JsonProperty("NomeLocal")
        private String nomeLocal;

        @JsonProperty("DataSituacaoAtual")
        private String dataSituacaoAtual;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentificacaoMateria {
        @JsonProperty("DescricaoIdentificacaoMateria")
        private String descricaoIdentificacaoMateria;

        @JsonProperty("SiglaCasaIdentificacaoMateria")
        private String siglaCasaIdentificacaoMateria;

        @JsonProperty("NomeCasaIdentificacaoMateria")
        private String nomeCasaIdentificacaoMateria;
    }
}
