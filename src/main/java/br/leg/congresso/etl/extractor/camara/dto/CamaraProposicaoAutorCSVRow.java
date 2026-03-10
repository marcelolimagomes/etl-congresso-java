package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento dos 12 campos do CSV de autores de proposições da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/proposicoesAutores/csv/proposicoesAutores-{ano}.csv
 * Separador: ponto-e-vírgula (;) | Colunas verificadas em 2024.
 */
@Data
public class CamaraProposicaoAutorCSVRow {

    @CsvBindByName(column = "idProposicao")
    private String idProposicao;

    @CsvBindByName(column = "uriProposicao")
    private String uriProposicao;

    @CsvBindByName(column = "idDeputadoAutor")
    private String idDeputadoAutor;

    @CsvBindByName(column = "uriAutor")
    private String uriAutor;

    @CsvBindByName(column = "codTipoAutor")
    private String codTipoAutor;

    @CsvBindByName(column = "tipoAutor")
    private String tipoAutor;

    @CsvBindByName(column = "nomeAutor")
    private String nomeAutor;

    @CsvBindByName(column = "siglaPartidoAutor")
    private String siglaPartidoAutor;

    @CsvBindByName(column = "uriPartidoAutor")
    private String uriPartidoAutor;

    @CsvBindByName(column = "siglaUFAutor")
    private String siglaUfAutor;

    @CsvBindByName(column = "ordemAssinatura")
    private String ordemAssinatura;

    @CsvBindByName(column = "proponente")
    private String proponente;
}
