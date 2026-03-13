package br.leg.congresso.etl.pagegen.dto;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * DTO principal para renderização de página estática de proposição/matéria.
 * Contém todos os dados necessários para gerar o HTML via Thymeleaf.
 */
@Value
@Builder
public class ProposicaoPageDTO {

    // ── Identificação ─────────────────────────────────────────────────────────
    /** "camara" | "senado" */
    String casa;
    /** "Câmara dos Deputados" | "Senado Federal" */
    String casaLabel;
    /** ID original na API de origem */
    String idOriginal;
    String siglaTipo;
    String descricaoTipo;
    String numero;
    Integer ano;

    // ── Conteúdo ──────────────────────────────────────────────────────────────
    String ementa;
    String ementaDetalhada;
    String situacaoDescricao;
    /** "tramitando" | "encerrada" | "desconhecida" */
    String situacaoTramitacao;
    /** Data formatada "dd/MM/yyyy" */
    String dataApresentacao;
    String orgaoAtual;
    String regime;
    String urlInteiroTeor;
    @Builder.Default
    List<String> keywords = List.of();
    @Builder.Default
    List<String> temas = List.of();

    // ── Autores ───────────────────────────────────────────────────────────────
    String autoriaResumo;
    @Builder.Default
    List<AutorDTO> autores = List.of();

    // ── Tramitação ────────────────────────────────────────────────────────────
    @Builder.Default
    List<TramitacaoDTO> tramitacoes = List.of();

    // ── Conteúdo relacionado ─────────────────────────────────────────────────
    @Builder.Default
    List<DocumentoResumoDTO> documentos = List.of();
    @Builder.Default
    List<ProposicaoResumoDTO> relacionadas = List.of();
    @Builder.Default
    List<VotacaoResumoDTO> votacoes = List.of();

    // ── SEO ───────────────────────────────────────────────────────────────────
    String canonicalUrl;
    String seoTitle;
    String seoDescription;
    /** JSON-LD completo pré-serializado (não escapar ao injetar no template) */
    String schemaOrgLegislationJson;
    String schemaOrgBreadcrumbJson;
    /** ISO 8601 para Schema.org datePublished */
    String dataPublicacaoIso;
    /** ISO 8601 para Schema.org dateModified */
    String dataAtualizacaoIso;

    // ── Controle ──────────────────────────────────────────────────────────────
    /** Timestamp ISO de quando a página foi gerada */
    String geradoEm;
    /** JSON completo da ProposicaoCompleta para hidratação Vue (opcional) */
    String proposicaoJsonEmbutido;
}
