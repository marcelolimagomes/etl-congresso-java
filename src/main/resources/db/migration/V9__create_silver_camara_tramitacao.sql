-- V9: Tabela silver.camara_tramitacao
-- Silver Layer — Câmara tramitações: espelha o payload de GET /api/v2/proposicoes/{id}/tramitacoes

CREATE TABLE silver.camara_tramitacao (

    -- ── Controle ──────────────────────────────────────────────────────────────
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    camara_proposicao_id    UUID        NOT NULL,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP   NOT NULL DEFAULT NOW(),
    content_hash            VARCHAR(64),

    -- ── Campos do payload /tramitacoes ────────────────────────────────────────
    sequencia                   INTEGER,
    data_hora                   VARCHAR(50),
    sigla_orgao                 VARCHAR(50),
    uri_orgao                   VARCHAR(500),
    uri_ultimo_relator          VARCHAR(500),
    regime                      VARCHAR(200),
    descricao_tramitacao        TEXT,
    cod_tipo_tramitacao         VARCHAR(20),
    descricao_situacao          VARCHAR(500),
    cod_situacao                INTEGER,
    despacho                    TEXT,
    url                         VARCHAR(500),
    ambito                      VARCHAR(200),
    apreciacao                  VARCHAR(200),

    CONSTRAINT pk_silver_camara_tramitacao PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_tramitacao UNIQUE (camara_proposicao_id, sequencia),
    CONSTRAINT fk_silver_camara_tramitacao_proposicao FOREIGN KEY (camara_proposicao_id)
        REFERENCES silver.camara_proposicao(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_silver_camara_tramitacao_job FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.camara_tramitacao IS 'Silver — tramitações Câmara, fiel ao endpoint /proposicoes/{id}/tramitacoes';
