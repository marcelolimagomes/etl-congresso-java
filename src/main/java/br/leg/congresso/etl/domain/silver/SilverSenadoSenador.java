package br.leg.congresso.etl.domain.silver;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidade Silver — senadores do Senado Federal.
 * Fonte: API GET /senador/lista/atual + complemento GET /senador/{codigo}.
 * Chave de deduplicação: (codigo_senador).
 */
@Entity
@Table(schema = "silver", name = "senado_senador", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_senador_nat_key", columnNames = {
        "codigo_senador" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @Column(name = "origem_carga", length = 20, nullable = false)
    @Builder.Default
    private String origemCarga = "API";

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Colunas da listagem (GET /senador/lista/atual) ─────────────────────────

    @Column(name = "codigo_senador", length = 20)
    private String codigoSenador;

    @Column(name = "nome_parlamentar", length = 300)
    private String nomeParlamentar;

    @Column(name = "nome_civil", length = 300)
    private String nomeCivil;

    @Column(name = "sexo", length = 5)
    private String sexo;

    @Column(name = "uf_parlamentar", length = 5)
    private String ufParlamentar;

    @Column(name = "participacao", length = 5)
    private String participacao;

    @Column(name = "partido_parlamentar", length = 100)
    private String partidoParlamentar;

    @Column(name = "sigla_partido_parlamentar", length = 20)
    private String siglaPartidoParlamentar;

    @Column(name = "data_designacao", length = 30)
    private String dataDesignacao;

    @Column(name = "codigo_legislatura", length = 10)
    private String codigoLegislatura;

    // ── Colunas de complemento API (GET /senador/{codigo}) ────────────────────

    @Column(name = "det_nome_completo", length = 300)
    private String detNomeCompleto;

    @Column(name = "det_data_nascimento", length = 30)
    private String detDataNascimento;

    @Column(name = "det_local_nascimento", length = 200)
    private String detLocalNascimento;

    @Column(name = "det_estado_civil", length = 50)
    private String detEstadoCivil;

    @Column(name = "det_escolaridade", length = 100)
    private String detEscolaridade;

    @Column(name = "det_contato_email", length = 200)
    private String detContatoEmail;

    @Column(name = "det_url_foto", length = 500)
    private String detUrlFoto;

    @Column(name = "det_url_pagina_parlamentar", length = 500)
    private String detUrlPaginaParlamentar;

    @Column(name = "det_pagina", length = 500)
    private String detPagina;

    @Column(name = "det_facebook", length = 500)
    private String detFacebook;

    @Column(name = "det_twitter", length = 500)
    private String detTwitter;

    @Column(name = "det_profissoes", columnDefinition = "TEXT")
    private String detProfissoes;
}
