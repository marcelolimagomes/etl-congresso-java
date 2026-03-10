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
 * Entidade Silver — filiações partidárias por senador.
 * Fonte: API GET /senador/{codigo}/filiacoes.
 * Chave de deduplicação: (codigo_senador, codigo_filiacao).
 */
@Entity
@Table(schema = "silver", name = "senado_senador_filiacao", uniqueConstraints = @UniqueConstraint(name = "uq_silver_senado_sen_filiacao", columnNames = {
        "codigo_senador", "codigo_filiacao" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverSenadoSenadorFiliacao {

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

    @Column(name = "codigo_filiacao", length = 20)
    private String codigoFiliacao;

    @Column(name = "codigo_partido", length = 20)
    private String codigoPartido;

    @Column(name = "sigla_partido", length = 20)
    private String siglaPartido;

    @Column(name = "nome_partido", length = 200)
    private String nomePartido;

    @Column(name = "data_inicio_filiacao", length = 30)
    private String dataInicioFiliacao;

    @Column(name = "data_termino_filiacao", length = 30)
    private String dataTerminoFiliacao;
}
