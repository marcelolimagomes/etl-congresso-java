-- V30: Tabela silver.camara_deputado
-- Silver Layer — Câmara Deputados: espelha o CSV deputados.csv + complemento GET /deputados/{id}

CREATE TABLE silver.camara_deputado (

    -- ── Controle ──────────────────────────────────────────────────────────────
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- ── Campos do CSV (deputados.csv) ─────────────────────────────────────────
    camara_id               VARCHAR(20),
    uri                     VARCHAR(500),
    nome_civil              VARCHAR(300),
    nome_parlamentar        VARCHAR(300),
    nome_eleitoral          VARCHAR(300),
    sexo                    VARCHAR(10),
    data_nascimento         VARCHAR(30),
    data_falecimento        VARCHAR(30),
    uf_nascimento           VARCHAR(5),
    municipio_nascimento    VARCHAR(200),
    cpf                     VARCHAR(20),
    escolaridade            VARCHAR(100),
    url_website             VARCHAR(500),
    url_foto                VARCHAR(500),
    primeira_legislatura    VARCHAR(10),
    ultima_legislatura      VARCHAR(10),

    -- ── Complemento API: GET /deputados/{id} (campos det_*) ──────────────────
    det_rede_social                 TEXT,
    det_status_id                   VARCHAR(20),
    det_status_id_legislatura       VARCHAR(10),
    det_status_nome                 VARCHAR(300),
    det_status_nome_eleitoral       VARCHAR(300),
    det_status_sigla_partido        VARCHAR(20),
    det_status_sigla_uf             VARCHAR(5),
    det_status_email                VARCHAR(200),
    det_status_situacao             VARCHAR(50),
    det_status_condicao_eleitoral   VARCHAR(50),
    det_status_descricao            VARCHAR(500),
    det_status_data                 VARCHAR(30),
    det_status_uri_partido          VARCHAR(500),
    det_status_url_foto             VARCHAR(500),
    det_gabinete_nome               VARCHAR(100),
    det_gabinete_predio             VARCHAR(100),
    det_gabinete_sala               VARCHAR(20),
    det_gabinete_andar              VARCHAR(20),
    det_gabinete_telefone           VARCHAR(30),
    det_gabinete_email              VARCHAR(200),

    CONSTRAINT pk_silver_camara_deputado           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_deputado_camara_id UNIQUE      (camara_id),
    CONSTRAINT fk_silver_camara_deputado_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado IS 'Silver — deputados da Câmara, fiel ao CSV deputados.csv + complemento GET /deputados/{id}';
COMMENT ON COLUMN silver.camara_deputado.camara_id        IS 'ID numérico do deputado na Câmara (campo "id" do CSV)';
COMMENT ON COLUMN silver.camara_deputado.origem_carga     IS 'CSV = full-load via arquivo; API = incremental via REST';
COMMENT ON COLUMN silver.camara_deputado.content_hash     IS 'SHA-256 dos campos fonte — usado para deduplicação Silver';
COMMENT ON COLUMN silver.camara_deputado.gold_sincronizado IS 'FALSE = pendente promoção para Gold; TRUE = já promovido';
COMMENT ON COLUMN silver.camara_deputado.det_status_id    IS 'NULL indica que o enriquecimento via GET /deputados/{id} ainda não foi executado';
