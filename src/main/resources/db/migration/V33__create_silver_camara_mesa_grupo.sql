-- V33: Sub-tabelas Silver — mesa diretora e grupos de trabalho da Câmara (C8, C9)
-- Silver Layer — espelha os CSVs legislaturasMesas.csv e gruposMembros.csv

-- ─────────────────────────────────────────────────────────────────────────────
-- C8: legislaturasMesas.csv
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_mesa_diretora (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos do CSV legislaturasMesas.csv
    id_deputado     VARCHAR(20),
    id_legislatura  VARCHAR(10),
    titulo          VARCHAR(200),
    data_inicio     VARCHAR(30),
    data_fim        VARCHAR(30),

    CONSTRAINT pk_silver_camara_mesa_diretora           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_mesa_diretora_nat_key   UNIQUE      (id_deputado, id_legislatura, titulo, data_inicio),
    CONSTRAINT fk_silver_camara_mesa_diretora_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_mesa_diretora IS 'Silver — composição da Mesa Diretora da Câmara por legislatura (CSV legislaturasMesas.csv)';

-- ─────────────────────────────────────────────────────────────────────────────
-- C9: gruposMembros.csv
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_grupo_membro (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos do CSV gruposMembros.csv
    id_deputado         VARCHAR(20),
    id_grupo            VARCHAR(20),
    nome_parlamentar    VARCHAR(300),
    uri                 VARCHAR(500),
    titulo              VARCHAR(200),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),

    CONSTRAINT pk_silver_camara_grupo_membro           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_grupo_membro_nat_key   UNIQUE      (id_deputado, id_grupo, data_inicio),
    CONSTRAINT fk_silver_camara_grupo_membro_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_grupo_membro IS 'Silver — membros de grupos de trabalho da Câmara (CSV gruposMembros.csv)';
