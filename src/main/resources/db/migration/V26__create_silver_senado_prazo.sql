-- Fase 6: Silver Senado — Prazos (API /processo/prazo)
CREATE TABLE IF NOT EXISTS silver.senado_prazo (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos da resposta (espelho fiel do endpoint /processo/prazo)
    tipo_prazo                  VARCHAR(200),
    data_inicio                 VARCHAR(30),
    data_fim                    VARCHAR(30),
    descricao                   TEXT,
    colegiado                   VARCHAR(200),
    situacao                    VARCHAR(100),

    -- Relacionamento com silver.senado_materia
    senado_materia_id           UUID REFERENCES silver.senado_materia(id) ON DELETE SET NULL,
    codigo_materia              VARCHAR(50),

    CONSTRAINT uq_senado_prazo UNIQUE (senado_materia_id, tipo_prazo, data_inicio)
);

CREATE INDEX IF NOT EXISTS idx_senado_prazo_job       ON silver.senado_prazo(etl_job_id);
CREATE INDEX IF NOT EXISTS idx_senado_prazo_materia   ON silver.senado_prazo(senado_materia_id);
