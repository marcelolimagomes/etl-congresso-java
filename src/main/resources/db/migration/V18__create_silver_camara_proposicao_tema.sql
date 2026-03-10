-- V18: Tabela silver.camara_proposicao_tema
-- Silver Layer — Câmara temas: espelha CSV proposicoesTemas-{ano}.csv
-- Colunas: uriProposicao;siglaTipo;numero;ano;codTema;tema;relevancia (verificado 2024)

CREATE TABLE silver.camara_proposicao_tema (

    -- ── Controle ──────────────────────────────────────────────────────────────
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    camara_proposicao_id    UUID,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP   NOT NULL DEFAULT NOW(),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'CSV',

    -- ── Campos do CSV proposicoesTemas-{ano}.csv (espelho fiel) ───────────────
    uri_proposicao          VARCHAR(500),
    sigla_tipo              VARCHAR(20),
    numero                  INTEGER,
    ano                     INTEGER,
    cod_tema                INTEGER,
    tema                    VARCHAR(200),
    relevancia              INTEGER,

    CONSTRAINT pk_silver_camara_proposicao_tema PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_proposicao_tema UNIQUE (uri_proposicao, cod_tema),
    CONSTRAINT fk_silver_camara_proposicao_tema_proposicao FOREIGN KEY (camara_proposicao_id)
        REFERENCES silver.camara_proposicao(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_silver_camara_proposicao_tema_job FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

CREATE INDEX idx_silver_camara_proposicao_tema_proposicao_id
    ON silver.camara_proposicao_tema(camara_proposicao_id);

CREATE INDEX idx_silver_camara_proposicao_tema_uri_proposicao
    ON silver.camara_proposicao_tema(uri_proposicao);

CREATE INDEX idx_silver_camara_proposicao_tema_etl_job
    ON silver.camara_proposicao_tema(etl_job_id);

COMMENT ON TABLE silver.camara_proposicao_tema IS
    'Silver — classificação temática de proposições da Câmara, fiel ao CSV proposicoesTemas-{ano}.csv';
