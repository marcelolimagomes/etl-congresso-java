package br.leg.congresso.etl.extractor.camara.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * DTO para resposta da API REST da Câmara ao consultar tramitações.
 * GET /api/v2/proposicoes/{id}/tramitacoes
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamaraTramitacaoDTO {

    private Integer sequencia;
    private String siglaOrgao;
    private String descricaoOrgao;
    private String dataHora;
    private String sequenciaTexto;
    private String siglaUltimoOrgao;
    private String uriUltimoOrgao;
    private String codTipoTramitacao;
    private String descricaoTramitacao;
    private String codSituacao;
    private String descricaoSituacao;
    private String despacho;
    private String url;
    private String ambito;
    private String apreciacao;

    /**
     * DTO para a resposta paginada de tramitações.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse {
        private List<CamaraTramitacaoDTO> dados;
    }
}
