package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

/**
 * Mapeamento dos 12 campos do CSV de votos individuais de votações da Câmara.
 * URL:
 * https://dadosabertos.camara.leg.br/arquivos/votacoesVotos/csv/votacoesVotos-{ano}.csv
 * Separador: ponto-e-vírgula (;) | Colunas verificadas em 2024.
 */
@Data
public class CamaraVotacaoVotoCSVRow {

    @CsvBindByName(column = "idVotacao")
    private String idVotacao;

    @CsvBindByName(column = "uriVotacao")
    private String uriVotacao;

    @CsvBindByName(column = "dataHoraVoto")
    private String dataHoraVoto;

    @CsvBindByName(column = "voto")
    private String voto;

    @CsvBindByName(column = "deputado_id")
    private String deputadoId;

    @CsvBindByName(column = "deputado_uri")
    private String deputadoUri;

    @CsvBindByName(column = "deputado_nome")
    private String deputadoNome;

    @CsvBindByName(column = "deputado_siglaPartido")
    private String deputadoSiglaPartido;

    @CsvBindByName(column = "deputado_uriPartido")
    private String deputadoUriPartido;

    @CsvBindByName(column = "deputado_siglaUf")
    private String deputadoSiglaUf;

    @CsvBindByName(column = "deputado_idLegislatura")
    private String deputadoIdLegislatura;

    @CsvBindByName(column = "deputado_urlFoto")
    private String deputadoUrlFoto;
}
