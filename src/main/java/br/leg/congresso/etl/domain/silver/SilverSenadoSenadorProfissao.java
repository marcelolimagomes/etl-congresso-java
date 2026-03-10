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
 * Entidade Silver — profissões por senador.
 * Fonte: API GET /senador/{codigo}/profissao.
 * Chave de deduplicação: (codigo_senador, codigo_profissao).
 */
@Entity
@Table(schema = "silver", name = "senado_senador_profissao", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_sen_prof", columnNames = {
        "codigo_senador", "codigo_profissao" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenadorProfissao {

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

    @Column(name = "codigo_profissao", length = 20)
    private String codigoProfissao;

    @Column(name = "descricao_profissao", length = 300)
    private String descricaoProfissao;

    @Column(name = "data_registro", length = 30)
    private String dataRegistro;
}
