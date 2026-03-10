package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento dos 20 campos do CSV de votações da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/votacoes/csv/votacoes-{ano}.csv
 * Separador: ponto-e-vírgula (;) | Colunas verificadas em 2024.
 */
@Data
public class CamaraVotacaoCSVRow {

    @CsvBindByName(column = "id")
    private String id;

    @CsvBindByName(column = "uri")
    private String uri;

    @CsvBindByName(column = "data")
    private String data;

    @CsvBindByName(column = "dataHoraRegistro")
    private String dataHoraRegistro;

    @CsvBindByName(column = "idOrgao")
    private String idOrgao;

    @CsvBindByName(column = "uriOrgao")
    private String uriOrgao;

    @CsvBindByName(column = "siglaOrgao")
    private String siglaOrgao;

    @CsvBindByName(column = "idEvento")
    private String idEvento;

    @CsvBindByName(column = "uriEvento")
    private String uriEvento;

    @CsvBindByName(column = "aprovacao")
    private String aprovacao;

    @CsvBindByName(column = "votosSim")
    private String votosSim;

    @CsvBindByName(column = "votosNao")
    private String votosNao;

    @CsvBindByName(column = "votosOutros")
    private String votosOutros;

    @CsvBindByName(column = "descricao")
    private String descricao;

    @CsvBindByName(column = "ultimaAberturaVotacao_dataHoraRegistro")
    private String ultimaAberturaVotacaoDataHoraRegistro;

    @CsvBindByName(column = "ultimaAberturaVotacao_descricao")
    private String ultimaAberturaVotacaoDescricao;

    @CsvBindByName(column = "ultimaApresentacaoProposicao_dataHoraRegistro")
    private String ultimaApresentacaoProposicaoDataHoraRegistro;

    @CsvBindByName(column = "ultimaApresentacaoProposicao_descricao")
    private String ultimaApresentacaoProposicaoDescricao;

    @CsvBindByName(column = "ultimaApresentacaoProposicao_idProposicao")
    private String ultimaApresentacaoProposicaoIdProposicao;

    @CsvBindByName(column = "ultimaApresentacaoProposicao_uriProposicao")
    private String ultimaApresentacaoProposicaoUriProposicao;
}
