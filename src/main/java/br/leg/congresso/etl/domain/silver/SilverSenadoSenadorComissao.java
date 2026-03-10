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
 * Entidade Silver — comissões por senador.
 * Fonte: API GET /senador/{codigo}/comissoes.
 * Chave de deduplicação: (codigo_senador, codigo_comissao,
 * data_inicio_participacao).
 */
@Entity
@Table(schema = "silver", name = "senado_senador_comissao", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_sen_comissao", columnNames = {
        "codigo_senador", "codigo_comissao", "data_inicio_participacao" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenadorComissao {

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

    @Column(name = "codigo_senador", length = 20)
    private String codigoSenador;

    @Column(name = "codigo_comissao", length = 20)
    private String codigoComissao;

    @Column(name = "sigla_comissao", length = 30)
    private String siglaComissao;

    @Column(name = "nome_comissao", length = 300)
    private String nomeComissao;

    @Column(name = "cargo", length = 100)
    private String cargo;

    @Column(name = "data_inicio_participacao", length = 30)
    private String dataInicioParticipacao;

    @Column(name = "data_termino_participacao", length = 30)
    private String dataTerminoParticipacao;

    @Column(name = "ativo", length = 20)
    private String ativo;
}
