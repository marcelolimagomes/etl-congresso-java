-- =============================================================================
-- V21 — Silver: silver.camara_votacao_orientacao
-- Fonte: votacoesOrientacoes-{ano}.csv (Câmara dos Deputados)
-- Colunas: 7 campos passthrough do CSV
-- Deduplicação: (id_votacao, sigla_bancada)
-- =============================================================================

CREATE TABLE IF NOT EXISTS silver.camara_votacao_orientacao (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id      UUID,
    ingerido_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    origem_carga    VARCHAR(10) NOT NULL DEFAULT 'CSV',

    -- Campos do CSV votacoesOrientacoes-{ano}.csv
    id_votacao       VARCHAR(60)  NOT NULL,
    uri_votacao      TEXT,
    sigla_orgao      VARCHAR(20),
    descricao        TEXT,
    sigla_bancada    VARCHAR(60),
    uri_bancada      TEXT,
    orientacao       VARCHAR(30),

    CONSTRAINT uq_silver_camara_votacao_orientacao UNIQUE (id_votacao, sigla_bancada)
);

CREATE INDEX IF NOT EXISTS idx_silver_camara_votacao_orientacao_votacao
    ON silver.camara_votacao_orientacao (id_votacao);
CREATE INDEX IF NOT EXISTS idx_silver_camara_votacao_orientacao_job
    ON silver.camara_votacao_orientacao (etl_job_id);
