package br.leg.congresso.etl.extractor.camara.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para resposta da API da Câmara — mandatos externos de um deputado.
 * GET /api/v2/deputados/{id}/mandatosExternos
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamaraDeputadoMandatoExternoDTO {

    private String anoInicio;
    private String anoFim;
    private String cargo;
    private String siglaUf;
    private String municipio;
    private String siglaPartidoEleicao;
    private String uriPartidoEleicao;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse {
        private List<CamaraDeputadoMandatoExternoDTO> dados;
        private List<Link> links;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Link {
            private String rel;
            private String href;
        }
    }
}
