package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de membros de grupos de trabalho da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/gruposMembros/csv/gruposMembros.csv
 * Separador: ponto-e-vírgula (;).
 *
 * Nota: CSV não possui coluna idDeputado diretamente;
 * o ID é extraído de membro_uri.
 */
@Data
public class CamaraGrupoMembroCSVRow {

    private String idDeputado;

    @CsvBindByName(column = "idGrupo")
    private String idGrupo;

    @CsvBindByName(column = "membro_nome")
    private String nomeParlamentar;

    @CsvBindByName(column = "membro_uri")
    private String uri;

    @CsvBindByName(column = "membro_cargo")
    private String titulo;

    @CsvBindByName(column = "membro_datainicio")
    private String dataInicio;

    @CsvBindByName(column = "membro_datafim")
    private String dataFim;

    public String getIdDeputado() {
        if (idDeputado == null && uri != null) {
            int idx = uri.lastIndexOf('/');
            if (idx >= 0 && idx < uri.length() - 1) {
                idDeputado = uri.substring(idx + 1);
            }
        }
        return idDeputado;
    }
}
