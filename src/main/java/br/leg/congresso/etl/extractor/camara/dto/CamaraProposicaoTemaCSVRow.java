package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento dos 7 campos do CSV de temas de proposições da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/proposicoesTemas/csv/proposicoesTemas-{ano}.csv
 * Separador: ponto-e-vírgula (;) | Colunas verificadas em 2024.
 */
@Data
public class CamaraProposicaoTemaCSVRow {

    @CsvBindByName(column = "uriProposicao")
    private String uriProposicao;

    @CsvBindByName(column = "siglaTipo")
    private String siglaTipo;

    @CsvBindByName(column = "numero")
    private String numero;

    @CsvBindByName(column = "ano")
    private String ano;

    @CsvBindByName(column = "codTema")
    private String codTema;

    @CsvBindByName(column = "tema")
    private String tema;

    @CsvBindByName(column = "relevancia")
    private String relevancia;
}
