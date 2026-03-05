package br.leg.congresso.etl.extractor.camara.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO para resposta da API REST da Câmara ao consultar proposições.
 * GET /api/v2/proposicoes/{id}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamaraProposicaoDTO {

    private Long id;
    private String uri;
    private String siglaTipo;
    private Integer codTipo;
    private Integer numero;
    private Integer ano;
    private String ementa;
    private String dataApresentacao;
    private String keywords;
    private String urlInteiroTeor;

    @JsonProperty("ultimoStatus")
    @JsonAlias({"statusProposicao"})
    private UltimoStatus ultimoStatus;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UltimoStatus {
        private String situacao;
        private String despacho;
        private String dataHora;
        private String descricaoSituacao;
        private String sequencia;
        private String siglaOrgao;
        private String uriOrgao;
        private String uriUltimoRelator;
        private String regime;
        private String descricaoTramitacao;
        /** Código do tipo de tramitação (ex: "130") */
        private String codTipoTramitacao;
        /** Código da situação (ex: "1140") */
        private String codSituacao;
        /** Âmbito (ex: "Plenário") */
        private String ambito;
        /** Forma de apreciação (ex: "Proposição Sujeita à Apreciação do Plenário") */
        private String apreciacao;
        /** URL da tramitação no sistema da Câmara */
        private String url;
    }

    /**
     * DTO para a resposta paginada da listagem de proposições.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse {
        private List<CamaraProposicaoDTO> dados;
        private List<Link> links;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Link {
            private String rel;
            private String href;
        }
    }
}
