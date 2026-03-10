package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de composição da Mesa Diretora da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/legislaturasMesas/csv/legislaturasMesas.csv
 * Separador: ponto-e-vírgula (;).
 */
@Data
public class CamaraMesaDiretoraCSVRow {

    @CsvBindByName(column = "idDeputado")
    private String idDeputado;

    @CsvBindByName(column = "idLegislatura")
    private String idLegislatura;

    @CsvBindByName(column = "titulo")
    private String titulo;

    @CsvBindByName(column = "dataInicio")
    private String dataInicio;

    @CsvBindByName(column = "dataFim")
    private String dataFim;
}
