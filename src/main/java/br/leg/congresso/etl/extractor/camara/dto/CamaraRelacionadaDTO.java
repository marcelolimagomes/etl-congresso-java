package br.leg.congresso.etl.extractor.camara.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para a resposta da API de proposições relacionadas.
 * GET /api/v2/proposicoes/{id}/relacionadas
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamaraRelacionadaDTO {

    private Long id;
    private String uri;
    private String siglaTipo;
    private Integer codTipo;
    private String numero;
    private String ano;
    private String ementa;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse {
        private List<CamaraRelacionadaDTO> dados;
    }
}
