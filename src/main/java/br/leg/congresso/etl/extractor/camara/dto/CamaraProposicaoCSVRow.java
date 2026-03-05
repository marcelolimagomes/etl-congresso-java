package br.leg.congresso.etl.extractor.camara.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 * Mapeamento dos 31 campos do CSV de proposições da Câmara dos Deputados.
 * URL: https://dadosabertos.camara.leg.br/arquivos/proposicoes/csv/proposicoes-{ano}.csv
 * Separador: ponto-e-vírgula (;) | Encoding: UTF-8 com BOM | Formato estável 2001-2026.
 *
 * ATENÇÃO: Os campos ultimoStatus_* devem usar exatamente os nomes das colunas do CSV.
 * Versão anterior usava 'ultimaSituacao*' (incorreto) — corrigido aqui.
 */
@Data
public class CamaraProposicaoCSVRow {

    // ── Identificação ─────────────────────────────────────────────────────
    @CsvBindByName(column = "id")
    private String id;

    @CsvBindByName(column = "uri")
    private String uri;

    @CsvBindByName(column = "siglaTipo")
    private String siglaTipo;

    @CsvBindByName(column = "numero")
    private String numero;

    @CsvBindByName(column = "ano")
    private String ano;

    @CsvBindByName(column = "codTipo")
    private String codTipo;

    @CsvBindByName(column = "descricaoTipo")
    private String descricaoTipo;

    // ── Descrição ──────────────────────────────────────────────────────────
    @CsvBindByName(column = "ementa")
    private String ementa;

    @CsvBindByName(column = "ementaDetalhada")
    private String ementaDetalhada;

    @CsvBindByName(column = "keywords")
    private String keywords;

    @CsvBindByName(column = "dataApresentacao")
    private String dataApresentacao;

    // ── URIs e referências ─────────────────────────────────────────────────
    @CsvBindByName(column = "uriOrgaoNumerador")
    private String uriOrgaoNumerador;

    @CsvBindByName(column = "uriPropAnterior")
    private String uriPropAnterior;

    @CsvBindByName(column = "uriPropPrincipal")
    private String uriPropPrincipal;

    @CsvBindByName(column = "uriPropPosterior")
    private String uriPropPosterior;

    @CsvBindByName(column = "urlInteiroTeor")
    private String urlInteiroTeor;

    @CsvBindByName(column = "urnFinal")
    private String urnFinal;

    // ── Último status (prefixo ultimoStatus_*) ─────────────────────────────
    @CsvBindByName(column = "ultimoStatus_dataHora")
    private String ultimoStatusDataHora;

    @CsvBindByName(column = "ultimoStatus_sequencia")
    private String ultimoStatusSequencia;

    @CsvBindByName(column = "ultimoStatus_uriRelator")
    private String ultimoStatusUriRelator;

    @CsvBindByName(column = "ultimoStatus_idOrgao")
    private String ultimoStatusIdOrgao;

    @CsvBindByName(column = "ultimoStatus_siglaOrgao")
    private String ultimoStatusSiglaOrgao;

    @CsvBindByName(column = "ultimoStatus_uriOrgao")
    private String ultimoStatusUriOrgao;

    @CsvBindByName(column = "ultimoStatus_regime")
    private String ultimoStatusRegime;

    @CsvBindByName(column = "ultimoStatus_descricaoTramitacao")
    private String ultimoStatusDescricaoTramitacao;

    @CsvBindByName(column = "ultimoStatus_idTipoTramitacao")
    private String ultimoStatusIdTipoTramitacao;

    @CsvBindByName(column = "ultimoStatus_descricaoSituacao")
    private String ultimoStatusDescricaoSituacao;

    @CsvBindByName(column = "ultimoStatus_idSituacao")
    private String ultimoStatusIdSituacao;

    @CsvBindByName(column = "ultimoStatus_despacho")
    private String ultimoStatusDespacho;

    @CsvBindByName(column = "ultimoStatus_apreciacao")
    private String ultimoStatusApreciacao;

    @CsvBindByName(column = "ultimoStatus_url")
    private String ultimoStatusUrl;
}
