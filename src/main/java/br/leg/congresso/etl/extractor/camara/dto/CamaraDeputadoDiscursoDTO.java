package br.leg.congresso.etl.extractor.camara.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para resposta da API da Câmara — discursos de um deputado.
 * GET /api/v2/deputados/{id}/discursos
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamaraDeputadoDiscursoDTO {

    private String dataHoraInicio;
    private String dataHoraFim;
    private String tipoDiscurso;
    private String sumario;
    private String transcricao;
    private String keywords;
    private String urlTexto;
    private String urlAudio;
    private String urlVideo;
    private String uriEvento;
    private FaseEvento faseEvento;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FaseEvento {
        private String titulo;
        private String dataHoraInicio;
        private String dataHoraFim;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse {
        private List<CamaraDeputadoDiscursoDTO> dados;
        private List<Link> links;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Link {
            private String rel;
            private String href;
        }
    }
}
