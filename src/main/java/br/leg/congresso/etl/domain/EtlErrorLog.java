package br.leg.congresso.etl.domain;

import br.leg.congresso.etl.domain.enums.CasaLegislativa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "etl_error_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtlErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private EtlJobControl job;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CasaLegislativa origem;

    @Column(name = "tipo_erro", length = 100)
    private String tipoErro;

    @Column(length = 500)
    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false)
    @Builder.Default
    private int tentativas = 1;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
