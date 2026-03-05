-- V10: Tabela silver.senado_materia
-- Silver Layer — Senado: pesquisa/lista + endpoint de detalhe + textos

CREATE TABLE silver.senado_materia (

    -- ── Controle ──────────────────────────────────────────────────────────────
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP,
    content_hash    VARCHAR(64),
    origem_carga    VARCHAR(30)  NOT NULL DEFAULT 'PESQUISA',
                    -- 'PESQUISA' | 'DETALHE' | 'INCREMENTAL'
    gold_sincronizado BOOLEAN    NOT NULL DEFAULT FALSE,

    -- ── Campos de pesquisa/lista.json ─────────────────────────────────────────
    codigo                      VARCHAR(20)   NOT NULL,
    identificacao_processo      VARCHAR(30),
    descricao_identificacao     VARCHAR(100),
    sigla                       VARCHAR(20),
    numero                      VARCHAR(20),  -- preservado com zeros à esq.
    ano                         INTEGER,
    ementa                      TEXT,
    autor                       VARCHAR(500),
    data                        VARCHAR(30),
    url_detalhe_materia         VARCHAR(500),

    -- ── Campos do endpoint de detalhe (/materia/{id}.json) ────────────────────
    det_sigla_casa_identificacao        VARCHAR(10),
    det_sigla_subtipo                   VARCHAR(20),
    det_descricao_subtipo               VARCHAR(200),
    det_descricao_objetivo_processo     VARCHAR(200),
    det_indicador_tramitando            VARCHAR(10),

    det_indexacao                       TEXT,
    det_casa_iniciadora                 VARCHAR(10),
    det_indicador_complementar          VARCHAR(10),
    det_natureza_codigo                 VARCHAR(20),
    det_natureza_nome                   VARCHAR(100),
    det_natureza_descricao              VARCHAR(200),
    det_sigla_casa_origem               VARCHAR(10),
    det_classificacoes                  JSONB,
    det_outras_informacoes              JSONB,

    -- ── Dados do endpoint /textos/{codigo}.json ───────────────────────────────
    url_texto                           VARCHAR(1000),
    data_texto                          VARCHAR(30),

    CONSTRAINT pk_silver_senado_materia PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_materia_codigo UNIQUE (codigo),
    CONSTRAINT fk_silver_senado_materia_job FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.senado_materia IS 'Silver — matérias do Senado, fiel à API pesquisa/lista + detalhe + textos';
COMMENT ON COLUMN silver.senado_materia.codigo IS 'Código único da matéria na API do Senado (campo "Codigo")';
COMMENT ON COLUMN silver.senado_materia.numero IS 'Número com zeros à esquerda, preservado como string da fonte';
COMMENT ON COLUMN silver.senado_materia.det_classificacoes IS 'Array de classificações temáticas (JSONB) fiel ao array da API';
COMMENT ON COLUMN silver.senado_materia.gold_sincronizado IS 'FALSE = pendente promoção para Gold; TRUE = já promovido';
