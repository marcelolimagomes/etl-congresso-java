package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de participação de deputados em frentes parlamentares.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/frentesDeputados/csv/frentesDeputados.csv
 * Separador: ponto-e-vírgula (;).
 * Nota: campo "id" do CSV refere-se ao id da frente (mapeado como idFrente).
 */
@Data
public class CamaraDeputadoFrenteCSVRow {

    @CsvBindByName(column = "idDeputado")
    private String idDeputado;

    /** id da frente parlamentar (coluna "id" no CSV) */
    @CsvBindByName(column = "id")
    private String idFrente;

    @CsvBindByName(column = "idLegislatura")
    private String idLegislatura;

    @CsvBindByName(column = "titulo")
    private String titulo;

    @CsvBindByName(column = "uri")
    private String uri;
}
