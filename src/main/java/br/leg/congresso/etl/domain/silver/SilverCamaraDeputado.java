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
 * Entidade Silver para deputados da Câmara dos Deputados.
 * Espelha fielmente o CSV deputados.csv (16 campos) + complemento do endpoint
 * GET /deputados/{id} (campos det_*).
 *
 * Princípio Silver: sem transformações — os dados ficam exatamente como vieram
 * da fonte.
 * Indicador de enriquecimento: det_status_id IS NULL = pendente chamada GET
 * /deputados/{id}.
 */
@Entity
@Table(schema = "silver", name = "camara_deputado", uniqueConstraints = @UniqueConstraint(name = "uq_silver_camara_deputado_camara_id", columnNames = "camara_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SilverCamaraDeputado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** ID do job ETL que gerou este registro */
    @Column(name = "etl_job_id")
    private UUID etlJobId;

    @CreationTimestamp
    @Column(name = "ingerido_em", updatable = false, nullable = false)
    private LocalDateTime ingeridoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** SHA-256 dos campos fonte — usado para deduplicação */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /** Origem da carga: 'CSV' (full-load) ou 'API' (incremental) */
    @Column(name = "origem_carga", length = 20, nullable = false)
    @Builder.Default
    private String origemCarga = "CSV";

    /**
     * Flag de controle: FALSE = pendente promoção para Gold; TRUE = já promovido
     */
    @Column(name = "gold_sincronizado", nullable = false)
    @Builder.Default
    private boolean goldSincronizado = false;

    // ── Campos do CSV (deputados.csv) ─────────────────────────────────────────

    /** ID numérico do deputado na Câmara (coluna "id" do CSV) */
    @Column(name = "camara_id", length = 20)
    private String camaraId;

    @Column(length = 500)
    private String uri;

    @Column(name = "nome_civil", length = 300)
    private String nomeCivil;

    @Column(name = "nome_parlamentar", length = 300)
    private String nomeParlamentar;

    @Column(name = "nome_eleitoral", length = 300)
    private String nomeEleitoral;

    @Column(length = 10)
    private String sexo;

    @Column(name = "data_nascimento", length = 30)
    private String dataNascimento;

    @Column(name = "data_falecimento", length = 30)
    private String dataFalecimento;

    @Column(name = "uf_nascimento", length = 5)
    private String ufNascimento;

    @Column(name = "municipio_nascimento", length = 200)
    private String municipioNascimento;

    @Column(length = 20)
    private String cpf;

    @Column(length = 100)
    private String escolaridade;

    @Column(name = "url_website", length = 500)
    private String urlWebsite;

    @Column(name = "url_foto", length = 500)
    private String urlFoto;

    @Column(name = "primeira_legislatura", length = 10)
    private String primeiraLegislatura;

    @Column(name = "ultima_legislatura", length = 10)
    private String ultimaLegislatura;

    // ── Complemento API: GET /deputados/{id} (campos det_*) ──────────────────

    /** Redes sociais — JSON array serializado. NULL = enriquecimento pendente */
    @Column(name = "det_rede_social", columnDefinition = "TEXT")
    private String detRedeSocial;

    /**
     * ultimoStatus.id — NULL indica que GET /deputados/{id} ainda não foi chamado
     */
    @Column(name = "det_status_id", length = 20)
    private String detStatusId;

    @Column(name = "det_status_id_legislatura", length = 10)
    private String detStatusIdLegislatura;

    @Column(name = "det_status_nome", length = 300)
    private String detStatusNome;

    @Column(name = "det_status_nome_eleitoral", length = 300)
    private String detStatusNomeEleitoral;

    @Column(name = "det_status_sigla_partido", length = 20)
    private String detStatusSiglaPartido;

    @Column(name = "det_status_sigla_uf", length = 5)
    private String detStatusSiglaUf;

    @Column(name = "det_status_email", length = 200)
    private String detStatusEmail;

    @Column(name = "det_status_situacao", length = 50)
    private String detStatusSituacao;

    @Column(name = "det_status_condicao_eleitoral", length = 50)
    private String detStatusCondicaoEleitoral;

    @Column(name = "det_status_descricao", length = 500)
    private String detStatusDescricao;

    @Column(name = "det_status_data", length = 30)
    private String detStatusData;

    @Column(name = "det_status_uri_partido", length = 500)
    private String detStatusUriPartido;

    @Column(name = "det_status_url_foto", length = 500)
    private String detStatusUrlFoto;

    @Column(name = "det_gabinete_nome", length = 100)
    private String detGabineteNome;

    @Column(name = "det_gabinete_predio", length = 100)
    private String detGabinetePredio;

    @Column(name = "det_gabinete_sala", length = 20)
    private String detGabineteSala;

    @Column(name = "det_gabinete_andar", length = 20)
    private String detGabineteAndar;

    @Column(name = "det_gabinete_telefone", length = 30)
    private String detGabineteTelefone;

    @Column(name = "det_gabinete_email", length = 200)
    private String detGabineteEmail;

    public String getContatoEmail() {
        if (detStatusEmail != null && !detStatusEmail.isBlank()) {
            return detStatusEmail;
        }
        if (detGabineteEmail != null && !detGabineteEmail.isBlank()) {
            return detGabineteEmail;
        }
        return null;
    }
}
