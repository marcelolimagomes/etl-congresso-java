-- Fase 7: Silver Senado — Votações (API /votacao?codigoMateria={codigo})
CREATE TABLE IF NOT EXISTS silver.senado_votacao (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos do endpoint /votacao (espelho fiel)
    codigo_sessao               VARCHAR(50),
    sigla_casa                  VARCHAR(10),
    codigo_sessao_votacao       VARCHAR(50),
    sequencial_sessao           VARCHAR(50),
    data_sessao                 VARCHAR(30),
    descricao_votacao           TEXT,
    resultado                   VARCHAR(200),
    descricao_resultado         TEXT,
    total_votos_sim             INTEGER,
    total_votos_nao             INTEGER,
    total_votos_abstencao       INTEGER,
    indicador_votacao_secreta   VARCHAR(10),
    votos_parlamentares         JSONB,

    -- Relacionamento com silver.senado_materia
    senado_materia_id           UUID REFERENCES silver.senado_materia(id) ON DELETE SET NULL,
    codigo_materia              VARCHAR(50),

    CONSTRAINT uq_senado_votacao UNIQUE (senado_materia_id, codigo_sessao_votacao, sequencial_sessao)
);

CREATE INDEX IF NOT EXISTS idx_senado_votacao_job       ON silver.senado_votacao(etl_job_id);
CREATE INDEX IF NOT EXISTS idx_senado_votacao_materia   ON silver.senado_votacao(senado_materia_id);
CREATE INDEX IF NOT EXISTS idx_senado_votacao_sessao    ON silver.senado_votacao(codigo_sessao_votacao);
