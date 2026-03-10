package br.leg.congresso.etl.extractor.camara.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para resposta da API da Câmara — eventos de um deputado.
 * GET /api/v2/deputados/{id}/eventos
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamaraDeputadoEventoDTO {

    private String id;
    private String dataHoraInicio;
    private String dataHoraFim;
    private String descricao;
    private String descricaoTipo;
    private String situacao;
    private String localExterno;
    private String uri;
    private String urlRegistro;
    private LocalCamara localCamara;
    private List<Object> orgaos;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocalCamara {
        private String nome;
        private String predio;
        private String sala;
        private String andar;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse {
        private List<CamaraDeputadoEventoDTO> dados;
        private List<Link> links;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Link {
            private String rel;
            private String href;
        }
    }
}
