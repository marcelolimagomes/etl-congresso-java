package br.leg.congresso.etl.extractor.senado.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para o endpoint
 * {@code GET /dadosabertos/votacao?codigoMateria={codigo}}.
 * Retorna array JSON diretamente (sem objeto wrapper).
 *
 * Os votos individuais de cada parlamentar são armazenados como JSONB
 * (rawVotosParlamentares)
 * para preservação fiel da estrutura da API sem transformação.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoVotacaoDTO {

    private String codigoSessao;
    private String siglaCasa;
    private String codigoSessaoVotacao;
    private String sequencialSessao;
    private String dataSessao;
    private String descricaoVotacao;
    private String resultado;
    private String descricaoResultado;
    private Integer totalVotosSim;
    private Integer totalVotosNao;
    private Integer totalVotosAbstencao;
    private String indicadorVotacaoSecreta;

    /** Lista de votos dos parlamentares — armazenada como JSONB na entidade. */
    private List<Object> votosParlamentares;
}
