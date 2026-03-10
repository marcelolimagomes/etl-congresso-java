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
 * Entidade Silver — histórico de status de deputados da Câmara.
 * Fonte: API GET /deputados/{id}/historico.
 * Chave de deduplicação: (camara_deputado_id, data_hora, id_legislatura).
 */
@Entity
@Table(schema = "silver", name = "camara_deputado_historico", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_dep_historico_nat_key", columnNames = {
        "camara_deputado_id", "data_hora", "id_legislatura" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputadoHistorico {

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

    // ── Campos da API /deputados/{id}/historico ────────────────────────────────
    @Column(name = "camara_deputado_id", length = 20)
    private String camaraDeputadoId;

    @Column(name = "camara_id_registro", length = 20)
    private String camaraIdRegistro;

    @Column(name = "id_legislatura", length = 10)
    private String idLegislatura;

    @Column(name = "nome", length = 300)
    private String nome;

    @Column(name = "nome_eleitoral", length = 300)
    private String nomeEleitoral;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "sigla_partido", length = 20)
    private String siglaPartido;

    @Column(name = "sigla_uf", length = 5)
    private String siglaUf;

    @Column(name = "situacao", length = 50)
    private String situacao;

    @Column(name = "condicao_eleitoral", length = 50)
    private String condicaoEleitoral;

    @Column(name = "descricao_status", columnDefinition = "TEXT")
    private String descricaoStatus;

    @Column(name = "data_hora", length = 30)
    private String dataHora;

    @Column(name = "uri_partido", length = 500)
    private String uriPartido;

    @Column(name = "url_foto", length = 500)
    private String urlFoto;
}
