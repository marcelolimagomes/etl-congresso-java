package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento do CSV de participação de deputados em órgãos da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/orgaosDeputados/csv/orgaosDeputados-L{leg}.csv
 * Separador: ponto-e-vírgula (;).
 *
 * Nota: CSV não possui colunas idDeputado/idOrgao diretamente;
 * os IDs são extraídos de uriDeputado e uriOrgao respectivamente.
 */
@Data
public class CamaraDeputadoOrgaoCSVRow {

    private String idDeputado;

    private String idOrgao;

    @CsvBindByName(column = "uriDeputado")
    private String uriDeputado;

    @CsvBindByName(column = "siglaOrgao")
    private String siglaOrgao;

    @CsvBindByName(column = "nomeOrgao")
    private String nomeOrgao;

    @CsvBindByName(column = "nomePublicacaoOrgao")
    private String nomePublicacao;

    @CsvBindByName(column = "cargo")
    private String titulo;

    private String codTitulo;

    @CsvBindByName(column = "dataInicio")
    private String dataInicio;

    @CsvBindByName(column = "dataFim")
    private String dataFim;

    @CsvBindByName(column = "uriOrgao")
    private String uriOrgao;

    public String getIdDeputado() {
        if (idDeputado == null && uriDeputado != null) {
            idDeputado = extrairIdDaUri(uriDeputado);
        }
        return idDeputado;
    }

    public String getIdOrgao() {
        if (idOrgao == null && uriOrgao != null) {
            idOrgao = extrairIdDaUri(uriOrgao);
        }
        return idOrgao;
    }

    private static String extrairIdDaUri(String uri) {
        if (uri != null && !uri.isBlank()) {
            int idx = uri.lastIndexOf('/');
            if (idx >= 0 && idx < uri.length() - 1) {
                return uri.substring(idx + 1);
            }
        }
        return null;
    }
}
