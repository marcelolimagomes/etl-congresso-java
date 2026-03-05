-- V1__create_proposicao.sql
-- Tabela principal do ETL: proposições e matérias legislativas normalizadas

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE proposicao (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    casa              VARCHAR(20)  NOT NULL,
    tipo              VARCHAR(50)  NOT NULL,
    sigla             VARCHAR(20),
    numero            INTEGER,
    ano               INTEGER,
    ementa            TEXT,
    situacao          VARCHAR(500),
    despacho_atual    VARCHAR(1000),
    data_apresentacao DATE,
    data_atualizacao  TIMESTAMP,
    status_final      VARCHAR(100),
    virou_lei         BOOLEAN      NOT NULL DEFAULT FALSE,
    id_origem         VARCHAR(50),
    uri_origem        VARCHAR(500),
    url_inteiro_teor  VARCHAR(1000),
    keywords          TEXT,
    content_hash      VARCHAR(64),
    criado_em         TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMP,

    CONSTRAINT pk_proposicao          PRIMARY KEY (id),
    CONSTRAINT uq_proposicao_natural  UNIQUE (casa, sigla, numero, ano)
);

COMMENT ON TABLE  proposicao                IS 'Proposições e matérias legislativas normalizadas (Câmara + Senado)';
COMMENT ON COLUMN proposicao.casa           IS 'Casa de origem: CAMARA ou SENADO';
COMMENT ON COLUMN proposicao.tipo           IS 'Tipo normalizado: LEI_ORDINARIA, LEI_COMPLEMENTAR, MEDIDA_PROVISORIA, EMENDA_CONSTITUCIONAL, DECRETO_LEGISLATIVO, RESOLUCAO';
COMMENT ON COLUMN proposicao.content_hash   IS 'Hash SHA-256 dos campos relevantes para detecção de mudanças (idempotência)';
COMMENT ON COLUMN proposicao.id_origem      IS 'Identificador na API de origem: id numérico (Câmara) ou código alfanumérico (Senado)';
