-- =============================================================================
-- V23 — Silver: silver.camara_proposicao_relacionada
-- Fonte: API GET /api/v2/proposicoes/{id}/relacionadas (Câmara dos Deputados)
-- Deduplicação: (proposicao_id, relacionada_id)
-- =============================================================================

CREATE TABLE IF NOT EXISTS silver.camara_proposicao_relacionada (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    origem_carga            VARCHAR(10) NOT NULL DEFAULT 'API',

    -- ID da proposição de origem (camara_id do registro pai)
    proposicao_id           VARCHAR(100) NOT NULL,

    -- Campos da proposição relacionada (espelho fiel da API)
    relacionada_id          INTEGER      NOT NULL,
    relacionada_uri         VARCHAR(500),
    relacionada_sigla_tipo  VARCHAR(20),
    relacionada_numero      VARCHAR(20),
    relacionada_ano         VARCHAR(10),
    relacionada_ementa      TEXT,
    relacionada_cod_tipo    VARCHAR(20),

    -- Relacionamento opcional com silver.camara_proposicao
    camara_proposicao_id    UUID REFERENCES silver.camara_proposicao(id) ON DELETE SET NULL,

    CONSTRAINT uq_silver_camara_proposicao_relacionada
        UNIQUE (proposicao_id, relacionada_id)
);

CREATE INDEX IF NOT EXISTS idx_silver_camara_proposicao_relacionada_proposicao
    ON silver.camara_proposicao_relacionada (proposicao_id);
CREATE INDEX IF NOT EXISTS idx_silver_camara_proposicao_relacionada_job
    ON silver.camara_proposicao_relacionada (etl_job_id);
