-- V16: Tabela silver.senado_autoria
-- Silver Layer — Senado autoria: espelha /dadosabertos/materia/autoria/{codigo}.json

CREATE TABLE silver.senado_autoria (

    -- ── Controle ──────────────────────────────────────────────────────────────
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    senado_materia_id   UUID        NOT NULL,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- ── Campos de autoria ─────────────────────────────────────────────────────
    nome_autor              VARCHAR(300)    NOT NULL,
    sexo_autor              VARCHAR(2),
    codigo_tipo_autor       VARCHAR(50)     NOT NULL,
    descricao_tipo_autor    VARCHAR(300),

    -- ── Campos do parlamentar (null quando autor é entidade) ──────────────────
    codigo_parlamentar      VARCHAR(20),
    nome_parlamentar        VARCHAR(300),
    sigla_partido           VARCHAR(20),
    uf_parlamentar          VARCHAR(5),

    CONSTRAINT pk_silver_senado_autoria PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_autoria UNIQUE (senado_materia_id, nome_autor, codigo_tipo_autor),
    CONSTRAINT fk_silver_senado_autoria_materia FOREIGN KEY (senado_materia_id)
        REFERENCES silver.senado_materia(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_silver_senado_autoria_job FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.senado_autoria IS 'Silver — autoria de matérias do Senado, fiel ao endpoint /autoria/{codigo}';
