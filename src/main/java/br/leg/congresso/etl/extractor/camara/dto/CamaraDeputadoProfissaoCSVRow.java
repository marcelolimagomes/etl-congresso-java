package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de profissões declaradas de deputados da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/deputadosProfissoes/csv/deputadosProfissoes.csv
 * Separador: ponto-e-vírgula (;).
 */
@Data
public class CamaraDeputadoProfissaoCSVRow {

    @CsvBindByName(column = "id")
    private String idDeputado;

    @CsvBindByName(column = "titulo")
    private String titulo;

    @CsvBindByName(column = "codTipoProfissao")
    private String codTipoProfissao;

    @CsvBindByName(column = "dataHora")
    private String dataHora;
}
