package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO para o endpoint
 * {@code GET /dadosabertos/processo/relatoria?codigoMateria={codigo}}.
 * Retorna array JSON diretamente (sem objeto wrapper).
 *
 * @see <a href=
 *      "https://legis.senado.leg.br/dadosabertos/processo/relatoria">Senado
 *      API</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoRelatoriaDTO {

    private Long id;
    private String casaRelator;
    private Long idTipoRelator;
    private String descricaoTipoRelator;
    private String dataDesignacao;
    private String dataDestituicao;
    private String descricaoTipoEncerramento;
    private Long idProcesso;
    private Long codigoMateria;
    private String identificacaoProcesso;
    private String ementaProcesso;
    private String tramitando;
    private Long codigoParlamentar;
    private String nomeParlamentar;
    private String nomeCompleto;
    private String sexoParlamentar;
    private String formaTratamentoParlamentar;
    private String siglaPartidoParlamentar;
    private String ufParlamentar;
    private Long codigoColegiado;
    private String siglaCasa;
    private String siglaColegiado;
    private String nomeColegiado;
    private Long codigoTipoColegiado;
}
