package br.leg.congresso.etl.extractor.senado.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * DTO genérico para endpoints de referência/domínio do Senado.
 *
 * Compartilhado pelos endpoints:
 * - GET /dadosabertos/processo/tipos-situacao → campos: codigo, descricao
 * - GET /dadosabertos/processo/tipos-decisao → campos: codigo, descricao
 * - GET /dadosabertos/processo/tipos-autor → campos: codigo, descricao
 * - GET /dadosabertos/processo/siglas → campos: sigla, descricao, classe
 * - GET /dadosabertos/processo/classes → campos: codigo, descricao, classePai
 * - GET /dadosabertos/processo/assuntos → campos: codigo, assuntoGeral,
 * assuntoEspecifico
 *
 * @JsonIgnoreProperties garante que campos desconhecidos da API não causem
 *                       erro.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SenadoRefDTO {

    /**
     * Código do tipo (usado em tipos-situacao, tipos-decisao, tipos-autor, classes,
     * assuntos)
     */
    private String codigo;

    /** Descrição genérica (maioria dos endpoints) */
    private String descricao;

    /** Sigla (endpoint siglas) */
    private String sigla;

    /** Classe/categoria associada (endpoint siglas) */
    private String classe;

    /** Código da classe pai (endpoint classes — hierarquia) */
    private String classePai;

    /** Assunto geral (endpoint assuntos) */
    private String assuntoGeral;

    /** Assunto específico (endpoint assuntos) */
    private String assuntoEspecifico;
}
