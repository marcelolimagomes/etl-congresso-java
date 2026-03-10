package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de presenças de deputados em eventos da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/eventosPresencaDeputados/csv/eventosPresencaDeputados-{ano}.csv
 * Separador: ponto-e-vírgula (;).
 */
@Data
public class CamaraDeputadoPresencaEventoCSVRow {

    @CsvBindByName(column = "idDeputado")
    private String idDeputado;

    @CsvBindByName(column = "idEvento")
    private String idEvento;

    @CsvBindByName(column = "dataHoraInicio")
    private String dataHoraInicio;

    @CsvBindByName(column = "dataHoraFim")
    private String dataHoraFim;

    @CsvBindByName(column = "descricao")
    private String descricao;

    @CsvBindByName(column = "descricaoTipo")
    private String descricaoTipo;

    @CsvBindByName(column = "situacao")
    private String situacao;

    @CsvBindByName(column = "uriEvento")
    private String uriEvento;
}
