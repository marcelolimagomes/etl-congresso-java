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
 * Entidade Silver — cargos por senador.
 * Fonte: API GET /senador/{codigo}/cargos.
 * Chave de deduplicação: (codigo_senador, codigo_cargo, data_inicio).
 */
@Entity
@Table(schema = "silver", name = "senado_senador_cargo", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_sen_cargo", columnNames = {
        "codigo_senador", "codigo_cargo", "data_inicio" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenadorCargo {

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

    @Column(name = "codigo_cargo", length = 20)
    private String codigoCargo;

    @Column(name = "descricao_cargo", length = 300)
    private String descricaoCargo;

    @Column(name = "tipo_cargo", length = 100)
    private String tipoCargo;

    @Column(name = "comissao_ou_orgao", length = 300)
    private String comissaoOuOrgao;

    @Column(name = "data_inicio", length = 30)
    private String dataInicio;

    @Column(name = "data_fim", length = 30)
    private String dataFim;
}
