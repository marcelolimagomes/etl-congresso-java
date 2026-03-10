package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de ocupações profissionais de deputados da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/deputadosOcupacoes/csv/deputadosOcupacoes.csv
 * Separador: ponto-e-vírgula (;).
 */
@Data
public class CamaraDeputadoOcupacaoCSVRow {

    @CsvBindByName(column = "idDeputado")
    private String idDeputado;

    @CsvBindByName(column = "titulo")
    private String titulo;

    @CsvBindByName(column = "anoInicio")
    private String anoInicio;

    @CsvBindByName(column = "anoFim")
    private String anoFim;

    @CsvBindByName(column = "entidade")
    private String entidade;

    @CsvBindByName(column = "entidadeUF")
    private String entidadeUF;

    @CsvBindByName(column = "entidadePais")
    private String entidadePais;
}
