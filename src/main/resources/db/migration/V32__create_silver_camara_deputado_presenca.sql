-- V32: Sub-tabela Silver — presenças de deputados em eventos (C6)
-- Silver Layer — espelha o CSV eventosPresencaDeputados-{ano}.csv

CREATE TABLE silver.camara_deputado_presenca_evento (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos do CSV eventosPresencaDeputados-{ano}.csv
    id_deputado         VARCHAR(20),
    id_evento           VARCHAR(20),
    data_hora_inicio    VARCHAR(30),
    data_hora_fim       VARCHAR(30),
    descricao           TEXT,
    descricao_tipo      VARCHAR(100),
    situacao            VARCHAR(50),
    uri_evento          VARCHAR(500),

    CONSTRAINT pk_silver_camara_deputado_presenca_evento           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_deputado_presenca_evento_nat_key   UNIQUE      (id_deputado, id_evento),
    CONSTRAINT fk_silver_camara_deputado_presenca_evento_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_presenca_evento IS 'Silver — presenças de deputados em eventos (CSV eventosPresencaDeputados-{ano}.csv)';
COMMENT ON COLUMN silver.camara_deputado_presenca_evento.id_deputado IS 'id do deputado na Câmara';
COMMENT ON COLUMN silver.camara_deputado_presenca_evento.id_evento   IS 'id do evento na Câmara';
