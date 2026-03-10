package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de membros de grupos de trabalho da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/gruposMembros/csv/gruposMembros.csv
 * Separador: ponto-e-vírgula (;).
 */
@Data
public class CamaraGrupoMembroCSVRow {

    @CsvBindByName(column = "idDeputado")
    private String idDeputado;

    @CsvBindByName(column = "idGrupo")
    private String idGrupo;

    @CsvBindByName(column = "nomeParlamentar")
    private String nomeParlamentar;

    @CsvBindByName(column = "uri")
    private String uri;

    @CsvBindByName(column = "titulo")
    private String titulo;

    @CsvBindByName(column = "dataInicio")
    private String dataInicio;

    @CsvBindByName(column = "dataFim")
    private String dataFim;
}
