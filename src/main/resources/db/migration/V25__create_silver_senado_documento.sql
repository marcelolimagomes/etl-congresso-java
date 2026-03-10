-- Fase 6: Silver Senado — Documentos (API /processo/documento)
CREATE TABLE IF NOT EXISTS silver.senado_documento (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos da resposta (espelho fiel do endpoint /processo/documento)
    codigo_documento            VARCHAR(50),
    tipo_documento              VARCHAR(200),
    descricao_tipo_documento    VARCHAR(500),
    data_documento              VARCHAR(30),
    descricao_documento         TEXT,
    url_documento               VARCHAR(1000),
    tipo_conteudo               VARCHAR(100),
    autor_nome                  VARCHAR(500),
    autor_codigo_parlamentar    VARCHAR(50),

    -- Relacionamento com silver.senado_materia
    senado_materia_id           UUID REFERENCES silver.senado_materia(id) ON DELETE SET NULL,
    codigo_materia              VARCHAR(50),

    CONSTRAINT uq_senado_documento UNIQUE (senado_materia_id, codigo_documento)
);

CREATE INDEX IF NOT EXISTS idx_senado_documento_job       ON silver.senado_documento(etl_job_id);
CREATE INDEX IF NOT EXISTS idx_senado_documento_materia   ON silver.senado_documento(senado_materia_id);
CREATE INDEX IF NOT EXISTS idx_senado_documento_codigo    ON silver.senado_documento(codigo_documento);
