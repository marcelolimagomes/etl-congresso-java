package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento dos campos do CSV de deputados da Câmara.
 * URL: https://dadosabertos.camara.leg.br/arquivos/deputados/csv/deputados.csv
 * Separador: ponto-e-vírgula (;) | Colunas verificadas em 2025.
 *
 * Colunas reais do CSV: uri, nome, idLegislaturaInicial, idLegislaturaFinal,
 * nomeCivil, cpf, siglaSexo, urlRedeSocial, urlWebsite, dataNascimento,
 * dataFalecimento, ufNascimento, municipioNascimento.
 *
 * O camaraId é extraído da URI (último segmento numérico).
 */
@Data
public class CamaraDeputadoCSVRow {

    @CsvBindByName(column = "uri")
    private String uri;

    /** Nome parlamentar (coluna 'nome' no CSV). */
    @CsvBindByName(column = "nome")
    private String nome;

    @CsvBindByName(column = "nomeCivil")
    private String nomeCivil;

    @CsvBindByName(column = "cpf")
    private String cpf;

    /** Sexo: 'M' ou 'F' (coluna 'siglaSexo' no CSV). */
    @CsvBindByName(column = "siglaSexo")
    private String siglaSexo;

    @CsvBindByName(column = "urlRedeSocial")
    private String urlRedeSocial;

    @CsvBindByName(column = "urlWebsite")
    private String urlWebsite;

    @CsvBindByName(column = "dataNascimento")
    private String dataNascimento;

    @CsvBindByName(column = "dataFalecimento")
    private String dataFalecimento;

    @CsvBindByName(column = "ufNascimento")
    private String ufNascimento;

    @CsvBindByName(column = "municipioNascimento")
    private String municipioNascimento;

    /** Número da primeira legislatura (coluna 'idLegislaturaInicial' no CSV). */
    @CsvBindByName(column = "idLegislaturaInicial")
    private String idLegislaturaInicial;

    /** Número da última legislatura (coluna 'idLegislaturaFinal' no CSV). */
    @CsvBindByName(column = "idLegislaturaFinal")
    private String idLegislaturaFinal;
}
