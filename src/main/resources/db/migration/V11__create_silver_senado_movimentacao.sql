-- V11: Tabela silver.senado_movimentacao
-- Silver Layer — Senado movimentações: espelha /dadosabertos/materia/{id}/movimentacoes.json

CREATE TABLE silver.senado_movimentacao (

    -- ── Controle ──────────────────────────────────────────────────────────────
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    senado_materia_id       UUID         NOT NULL,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP    NOT NULL DEFAULT NOW(),
    content_hash            VARCHAR(64),

    -- ── Campos do payload /movimentacoes ──────────────────────────────────────
    sequencia_movimentacao      VARCHAR(20),
    data_movimentacao           VARCHAR(30),
    identificacao_tramitacao    VARCHAR(30),
    descricao_movimentacao      TEXT,
    descricao_situacao          VARCHAR(500),
    despacho                    TEXT,
    ambito                      VARCHAR(200),
    sigla_local                 VARCHAR(100),
    nome_local                  VARCHAR(500),

    CONSTRAINT pk_silver_senado_movimentacao PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_movimentacao UNIQUE (senado_materia_id, sequencia_movimentacao),
    CONSTRAINT fk_silver_senado_movimentacao_materia FOREIGN KEY (senado_materia_id)
        REFERENCES silver.senado_materia(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_silver_senado_movimentacao_job FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.senado_movimentacao IS 'Silver — movimentações Senado, fiel ao endpoint /movimentacoes';
