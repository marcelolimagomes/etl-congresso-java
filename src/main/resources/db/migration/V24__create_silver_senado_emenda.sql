-- Fase 6: Silver Senado — Emendas (API /processo/emenda)
CREATE TABLE IF NOT EXISTS silver.senado_emenda (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos da resposta (espelho fiel do endpoint /processo/emenda)
    codigo_emenda               VARCHAR(50),
    tipo_emenda                 VARCHAR(100),
    descricao_tipo_emenda       VARCHAR(200),
    numero_emenda               VARCHAR(50),
    data_apresentacao           VARCHAR(30),
    colegiado_apresentacao      VARCHAR(200),
    turno                       VARCHAR(50),
    autor_nome                  VARCHAR(500),
    autor_codigo_parlamentar    VARCHAR(50),
    autor_tipo                  VARCHAR(100),
    ementa                      TEXT,
    inteiro_teor_url            VARCHAR(1000),

    -- Relacionamento com silver.senado_materia
    senado_materia_id           UUID REFERENCES silver.senado_materia(id) ON DELETE SET NULL,
    codigo_materia              VARCHAR(50),

    CONSTRAINT uq_senado_emenda UNIQUE (senado_materia_id, codigo_emenda)
);

CREATE INDEX IF NOT EXISTS idx_senado_emenda_job       ON silver.senado_emenda(etl_job_id);
CREATE INDEX IF NOT EXISTS idx_senado_emenda_materia   ON silver.senado_emenda(senado_materia_id);
CREATE INDEX IF NOT EXISTS idx_senado_emenda_codigo    ON silver.senado_emenda(codigo_emenda);
