-- =============================================================================
-- V22 — Silver: silver.camara_votacao_voto
-- Fonte: votacoesVotos-{ano}.csv (Câmara dos Deputados)
-- Colunas: 12 campos passthrough do CSV
-- Deduplicação: (id_votacao, deputado_id)
-- =============================================================================

CREATE TABLE IF NOT EXISTS silver.camara_votacao_voto (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id      UUID,
    ingerido_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    origem_carga    VARCHAR(10) NOT NULL DEFAULT 'CSV',

    -- Campos do CSV votacoesVotos-{ano}.csv
    id_votacao              VARCHAR(60)  NOT NULL,
    uri_votacao             TEXT,
    data_hora_voto          TIMESTAMPTZ,
    voto                    VARCHAR(30),

    -- Prefixo deputado
    deputado_id             INTEGER      NOT NULL,
    deputado_uri            TEXT,
    deputado_nome           VARCHAR(200),
    deputado_sigla_partido  VARCHAR(20),
    deputado_uri_partido    TEXT,
    deputado_sigla_uf       VARCHAR(4),
    deputado_id_legislatura INTEGER,
    deputado_url_foto       TEXT,

    CONSTRAINT uq_silver_camara_votacao_voto UNIQUE (id_votacao, deputado_id)
);

CREATE INDEX IF NOT EXISTS idx_silver_camara_votacao_voto_votacao
    ON silver.camara_votacao_voto (id_votacao);
CREATE INDEX IF NOT EXISTS idx_silver_camara_votacao_voto_deputado
    ON silver.camara_votacao_voto (deputado_id);
CREATE INDEX IF NOT EXISTS idx_silver_camara_votacao_voto_job
    ON silver.camara_votacao_voto (etl_job_id);
