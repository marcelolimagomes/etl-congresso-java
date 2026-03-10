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
 * Entidade Silver — despesas CEAP da Câmara dos Deputados.
 * Espelha o CSV Ano-{ano}.csv (Cota Parlamentar — CEAP).
 * Chave de deduplicação: (camara_deputado_id, cod_documento, num_documento,
 * parcela).
 */
@Entity
@Table(schema = "silver", name = "camara_despesa", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_despesa_nat_key", columnNames = {
        "camara_deputado_id", "cod_documento", "num_documento", "parcela" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDespesa {

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
    private String origemCarga = "CSV";

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Campos do CSV Ano-{ano}.csv (CEAP) ────────────────────────────────────
    @Column(name = "camara_deputado_id", length = 20)
    private String camaraDeputadoId;

    @Column(name = "ano", length = 10)
    private String ano;

    @Column(name = "mes", length = 5)
    private String mes;

    @Column(name = "tipo_despesa", length = 300)
    private String tipoDespesa;

    @Column(name = "cod_documento", length = 20)
    private String codDocumento;

    @Column(name = "tipo_documento", length = 100)
    private String tipoDocumento;

    @Column(name = "cod_tipo_documento", length = 10)
    private String codTipoDocumento;

    @Column(name = "data_documento", length = 30)
    private String dataDocumento;

    @Column(name = "num_documento", length = 50)
    private String numDocumento;

    @Column(name = "parcela", length = 10)
    private String parcela;

    @Column(name = "valor_documento", length = 30)
    private String valorDocumento;

    @Column(name = "valor_glosa", length = 30)
    private String valorGlosa;

    @Column(name = "valor_liquido", length = 30)
    private String valorLiquido;

    @Column(name = "nome_fornecedor", length = 300)
    private String nomeFornecedor;

    @Column(name = "cnpj_cpf_fornecedor", length = 20)
    private String cnpjCpfFornecedor;

    @Column(name = "num_ressarcimento", length = 50)
    private String numRessarcimento;

    @Column(name = "url_documento", length = 500)
    private String urlDocumento;

    @Column(name = "cod_lote", length = 20)
    private String codLote;
}
