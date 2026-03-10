package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de participação de deputados em órgãos da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/orgaosDeputados/csv/orgaosDeputados-L{leg}.csv
 * Separador: ponto-e-vírgula (;).
 */
@Data
public class CamaraDeputadoOrgaoCSVRow {

    @CsvBindByName(column = "idDeputado")
    private String idDeputado;

    @CsvBindByName(column = "idOrgao")
    private String idOrgao;

    @CsvBindByName(column = "siglaOrgao")
    private String siglaOrgao;

    @CsvBindByName(column = "nomeOrgao")
    private String nomeOrgao;

    @CsvBindByName(column = "nomePublicacao")
    private String nomePublicacao;

    @CsvBindByName(column = "titulo")
    private String titulo;

    @CsvBindByName(column = "codTitulo")
    private String codTitulo;

    @CsvBindByName(column = "dataInicio")
    private String dataInicio;

    @CsvBindByName(column = "dataFim")
    private String dataFim;

    @CsvBindByName(column = "uriOrgao")
    private String uriOrgao;
}
