-- V8: Cria o schema silver e a tabela camara_proposicao
-- Silver Layer — Câmara: espelha os 31 campos do CSV + complemento da API de detalhe

CREATE SCHEMA IF NOT EXISTS silver;

CREATE TABLE silver.camara_proposicao (

    -- ── Controle interno ─────────────────────────────────────────────────────
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP,
    content_hash    VARCHAR(64),
    origem_carga    VARCHAR(20)  NOT NULL DEFAULT 'CSV',  -- 'CSV' | 'API'
    gold_sincronizado BOOLEAN    NOT NULL DEFAULT FALSE,

    -- ── Campos do CSV (31 colunas originais) ─────────────────────────────────
    camara_id               VARCHAR(20),          -- CSV: "id"
    uri                     VARCHAR(500),
    sigla_tipo              VARCHAR(20),
    numero                  INTEGER,
    ano                     INTEGER,
    cod_tipo                INTEGER,
    descricao_tipo          VARCHAR(200),
    ementa                  TEXT,
    ementa_detalhada        TEXT,
    keywords                TEXT,
    data_apresentacao       VARCHAR(50),          -- preservado como string
    uri_orgao_numerador     VARCHAR(500),
    uri_prop_anterior       VARCHAR(500),
    uri_prop_principal      VARCHAR(500),
    uri_prop_posterior      VARCHAR(500),
    url_inteiro_teor        VARCHAR(1000),
    urn_final               VARCHAR(500),

    -- Último status (prefixo ultimo_status_*)
    ultimo_status_data_hora             VARCHAR(50),
    ultimo_status_sequencia             INTEGER,
    ultimo_status_uri_relator           VARCHAR(500),
    ultimo_status_id_orgao              INTEGER,
    ultimo_status_sigla_orgao           VARCHAR(50),
    ultimo_status_uri_orgao             VARCHAR(500),
    ultimo_status_regime                VARCHAR(200),
    ultimo_status_descricao_tramitacao  TEXT,
    ultimo_status_id_tipo_tramitacao    VARCHAR(20),
    ultimo_status_descricao_situacao    VARCHAR(500),
    ultimo_status_id_situacao           INTEGER,
    ultimo_status_despacho              TEXT,
    ultimo_status_apreciacao            VARCHAR(200),
    ultimo_status_url                   VARCHAR(500),

    -- ── Campos complementares do endpoint de detalhe (API) ───────────────────
    status_data_hora                    VARCHAR(50),
    status_sequencia                    INTEGER,
    status_sigla_orgao                  VARCHAR(50),
    status_uri_orgao                    VARCHAR(500),
    status_uri_ultimo_relator           VARCHAR(500),
    status_regime                       VARCHAR(200),
    status_descricao_tramitacao         TEXT,
    status_cod_tipo_tramitacao          VARCHAR(20),
    status_descricao_situacao           VARCHAR(500),
    status_cod_situacao                 INTEGER,
    status_despacho                     TEXT,
    status_url                          VARCHAR(500),
    status_ambito                       VARCHAR(200),
    status_apreciacao                   VARCHAR(200),
    uri_autores                         VARCHAR(500),
    texto                               TEXT,
    justificativa                       TEXT,

    CONSTRAINT pk_silver_camara_proposicao PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_proposicao_camara_id UNIQUE (camara_id),
    CONSTRAINT fk_silver_camara_proposicao_job FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON SCHEMA silver IS 'Camada Silver do padrão medalhão — dados fiéis à fonte antes da normalização Gold';
COMMENT ON TABLE  silver.camara_proposicao IS 'Silver — proposições da Câmara, fiel ao CSV (31 campos) + complemento da API de detalhe';
COMMENT ON COLUMN silver.camara_proposicao.camara_id IS 'PK da Câmara (campo "id" do CSV / payload da API)';
COMMENT ON COLUMN silver.camara_proposicao.origem_carga IS 'CSV = full-load via arquivo; API = incremental via REST';
COMMENT ON COLUMN silver.camara_proposicao.content_hash IS 'SHA-256 dos campos fonte — usado para deduplicação Silver';
COMMENT ON COLUMN silver.camara_proposicao.gold_sincronizado IS 'FALSE = pendente promoção para Gold; TRUE = já promovido';
