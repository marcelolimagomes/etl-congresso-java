package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento dos 7 campos do CSV de orientações de bancada para votações.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/votacoesOrientacoes/csv/votacoesOrientacoes-{ano}.csv
 * Separador: ponto-e-vírgula (;) | Colunas verificadas em 2024.
 */
@Data
public class CamaraVotacaoOrientacaoCSVRow {

    @CsvBindByName(column = "idVotacao")
    private String idVotacao;

    @CsvBindByName(column = "uriVotacao")
    private String uriVotacao;

    @CsvBindByName(column = "siglaOrgao")
    private String siglaOrgao;

    @CsvBindByName(column = "descricao")
    private String descricao;

    @CsvBindByName(column = "siglaBancada")
    private String siglaBancada;

    @CsvBindByName(column = "uriBancada")
    private String uriBancada;

    @CsvBindByName(column = "orientacao")
    private String orientacao;
}
