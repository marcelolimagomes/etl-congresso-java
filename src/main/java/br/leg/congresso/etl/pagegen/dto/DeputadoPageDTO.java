package br.leg.congresso.etl.pagegen.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO para renderização da página estática de um deputado federal.
 * Monta-se a partir da entidade Silver {@code silver.camara_deputado}.
 */
@Getter
@Builder
public class DeputadoPageDTO {

    // ── Identidade ────────────────────────────────────────────────────────────

    private String camaraId;
    private String nomeParlamentar;
    private String nomeCivil;
    private String nomeEleitoral;
    private String sexo;

    // ── Mandato atual ─────────────────────────────────────────────────────────

    private String partido;
    private String uf;
    private String situacao;
    private String condicaoEleitoral;
    private String descricaoSituacao;

    // ── Dados biográficos ─────────────────────────────────────────────────────

    private String dataNascimento;
    private String localNascimento; // município + UF
    private String escolaridade;

    // ── Contato ───────────────────────────────────────────────────────────────

    private String email;
    private String urlFoto;
    private String urlWebsite;

    // ── Gabinete ──────────────────────────────────────────────────────────────

    private String gabineteNome;
    private String gabineteEndereco; // "Prédio X, Sala Y, Andar Z"
    private String gabineteEmail;
    private String gabineteTelefone;

    // ── Redes sociais ─────────────────────────────────────────────────────────

    /** Redes sociais extraídas do JSON {@code det_rede_social}. */
    private List<String> redesSociais;

    // ── Histórico legislativo ─────────────────────────────────────────────────

    private String primeiraLegislatura;
    private String ultimaLegislatura;
    private List<DeputadoOrgaoDTO> orgaos;
    private List<DeputadoFrenteDTO> frentes;
    private DespesaResumoDTO despesasResumo;
    private List<ProposicaoResumoDTO> proposicoesRecentes;

    // ── SEO / meta ────────────────────────────────────────────────────────────

    private String canonicalUrl;
    private String seoTitle;
    private String seoDescription;
    private String schemaOrgPersonJson;
    private String schemaOrgBreadcrumbJson;
    private String geradoEm;
}
