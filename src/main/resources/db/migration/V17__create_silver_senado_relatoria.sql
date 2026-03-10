-- V17: Tabela silver.senado_relatoria
-- Silver Layer — Senado relatoria: espelha /dadosabertos/processo/relatoria?codigoMateria={codigo}

CREATE TABLE silver.senado_relatoria (

    -- ── Controle ──────────────────────────────────────────────────────────────
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    senado_materia_id   UUID        NOT NULL,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- ── Identificação da relatoria ────────────────────────────────────────────
    id_relatoria                BIGINT          NOT NULL,
    casa_relator                VARCHAR(10),
    id_tipo_relator             BIGINT,
    descricao_tipo_relator      VARCHAR(200),
    data_designacao             VARCHAR(20),
    data_destituicao            VARCHAR(20),
    descricao_tipo_encerramento VARCHAR(300),
    id_processo                 BIGINT,
    identificacao_processo      VARCHAR(100),
    tramitando                  VARCHAR(5),

    -- ── Campos do parlamentar relator ─────────────────────────────────────────
    codigo_parlamentar          BIGINT,
    nome_parlamentar            VARCHAR(300),
    nome_completo               VARCHAR(300),
    sexo_parlamentar            VARCHAR(2),
    forma_tratamento            VARCHAR(100),
    sigla_partido               VARCHAR(20),
    uf_parlamentar              VARCHAR(5),

    -- ── Campos do colegiado ───────────────────────────────────────────────────
    codigo_colegiado            BIGINT,
    sigla_casa                  VARCHAR(10),
    sigla_colegiado             VARCHAR(30),
    nome_colegiado              VARCHAR(300),
    codigo_tipo_colegiado       BIGINT,

    CONSTRAINT pk_silver_senado_relatoria PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_relatoria UNIQUE (senado_materia_id, id_relatoria),
    CONSTRAINT fk_silver_senado_relatoria_materia FOREIGN KEY (senado_materia_id)
        REFERENCES silver.senado_materia(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_silver_senado_relatoria_job FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.senado_relatoria IS 'Silver — relatoria de matérias do Senado, fiel ao endpoint /processo/relatoria?codigoMateria';
