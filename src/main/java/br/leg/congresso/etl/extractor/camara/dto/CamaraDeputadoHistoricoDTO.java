package br.leg.congresso.etl.extractor.camara.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para resposta da API da Câmara — histórico de status de um deputado.
 * GET /api/v2/deputados/{id}/historico
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamaraDeputadoHistoricoDTO {

    private String id;
    private Integer idLegislatura;
    private String nome;
    private String nomeEleitoral;
    private String email;
    private String siglaPartido;
    private String siglaUf;
    private String situacao;
    private String condicaoEleitoral;
    private String descricaoStatus;
    private String dataHora;
    private String uriPartido;
    private String urlFoto;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse {
        private List<CamaraDeputadoHistoricoDTO> dados;
        private List<Link> links;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Link {
            private String rel;
            private String href;
        }
    }
}
