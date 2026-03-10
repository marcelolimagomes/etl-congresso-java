package br.leg.congresso.etl.pagegen.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO para renderização da página estática de um senador federal.
 * Monta-se a partir da entidade Silver {@code silver.senado_senador}.
 */
@Getter
@Builder
public class SenadorPageDTO {

    // ── Identidade ────────────────────────────────────────────────────────────

    private String codigoSenador;
    private String nomeParlamentar;
    private String nomeCompleto;
    private String sexo;

    // ── Mandato ───────────────────────────────────────────────────────────────

    private String partido;
    private String uf;
    /** "Titular" ou "Suplente" */
    private String participacao;
    private String legislatura;

    // ── Dados biográficos ─────────────────────────────────────────────────────

    private String dataNascimento;
    private String localNascimento;
    private String escolaridade;
    private String estadoCivil;

    // ── Profissões ────────────────────────────────────────────────────────────

    /** Profissões extraídas do JSON {@code det_profissoes}. */
    private List<String> profissoes;

    // ── Contato e links ───────────────────────────────────────────────────────

    private String email;
    private String urlFoto;
    private String urlPaginaParlamentar;
    private String paginaPessoal;
    private String facebook;
    private String twitter;

    // ── SEO / meta ────────────────────────────────────────────────────────────

    private String canonicalUrl;
    private String seoTitle;
    private String seoDescription;
    private String geradoEm;
}
