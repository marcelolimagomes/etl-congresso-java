-- =============================================================================
-- V20 — Silver: silver.camara_votacao
-- Fonte: votacoes-{ano}.csv (Câmara dos Deputados)
-- Colunas: 20 campos passthrough do CSV
-- Deduplicação: votacao_id (string id da Câmara)
-- =============================================================================

CREATE TABLE IF NOT EXISTS silver.camara_votacao (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id      UUID,
    ingerido_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    origem_carga    VARCHAR(10) NOT NULL DEFAULT 'CSV',

    -- Campos do CSV votacoes-{ano}.csv
    votacao_id      VARCHAR(60)  NOT NULL,
    uri             TEXT,
    data            DATE,
    data_hora_registro          TIMESTAMPTZ,
    id_orgao        INTEGER,
    uri_orgao       TEXT,
    sigla_orgao     VARCHAR(20),
    id_evento       INTEGER,
    uri_evento      TEXT,
    aprovacao       SMALLINT,
    votos_sim       INTEGER,
    votos_nao       INTEGER,
    votos_outros    INTEGER,
    descricao       TEXT,

    -- Prefixo ultimaAberturaVotacao
    ultima_abertura_votacao_data_hora_registro  TIMESTAMPTZ,
    ultima_abertura_votacao_descricao           TEXT,

    -- Prefixo ultimaApresentacaoProposicao
    ultima_apresentacao_proposicao_data_hora_registro  TIMESTAMPTZ,
    ultima_apresentacao_proposicao_descricao           TEXT,
    ultima_apresentacao_proposicao_id_proposicao       INTEGER,
    ultima_apresentacao_proposicao_uri_proposicao      TEXT,

    CONSTRAINT uq_silver_camara_votacao UNIQUE (votacao_id)
);

CREATE INDEX IF NOT EXISTS idx_silver_camara_votacao_data    ON silver.camara_votacao (data);
CREATE INDEX IF NOT EXISTS idx_silver_camara_votacao_orgao   ON silver.camara_votacao (sigla_orgao);
CREATE INDEX IF NOT EXISTS idx_silver_camara_votacao_job     ON silver.camara_votacao (etl_job_id);
