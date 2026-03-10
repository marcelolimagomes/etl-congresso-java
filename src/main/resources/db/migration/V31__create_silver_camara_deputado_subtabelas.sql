-- V31: Sub-tabelas Silver — deputados Câmara (profissão, ocupação, órgão, frente)
-- Silver Layer — espelha os CSVs auxiliares de deputados (C2-C5)

-- ─────────────────────────────────────────────────────────────────────────────
-- C2: deputadosProfissoes.csv
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_deputado_profissao (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos do CSV deputadosProfissoes.csv
    id_deputado         VARCHAR(20),
    titulo              VARCHAR(300),
    cod_tipo_profissao  VARCHAR(20),
    data_hora           VARCHAR(30),

    CONSTRAINT pk_silver_camara_deputado_profissao           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_deputado_profissao_nat_key   UNIQUE      (id_deputado, titulo, cod_tipo_profissao),
    CONSTRAINT fk_silver_camara_deputado_profissao_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_profissao IS 'Silver — profissões declaradas de deputados da Câmara (CSV deputadosProfissoes.csv)';
COMMENT ON COLUMN silver.camara_deputado_profissao.id_deputado IS 'id do deputado na Câmara (campo idDeputado no CSV)';

-- ─────────────────────────────────────────────────────────────────────────────
-- C3: deputadosOcupacoes.csv
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_deputado_ocupacao (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos do CSV deputadosOcupacoes.csv
    id_deputado     VARCHAR(20),
    titulo          VARCHAR(300),
    ano_inicio      VARCHAR(10),
    ano_fim         VARCHAR(10),
    entidade        VARCHAR(300),
    entidade_uf     VARCHAR(5),
    entidade_pais   VARCHAR(100),

    CONSTRAINT pk_silver_camara_deputado_ocupacao           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_deputado_ocupacao_nat_key   UNIQUE      (id_deputado, titulo, ano_inicio),
    CONSTRAINT fk_silver_camara_deputado_ocupacao_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_ocupacao IS 'Silver — ocupações profissionais de deputados da Câmara (CSV deputadosOcupacoes.csv)';

-- ─────────────────────────────────────────────────────────────────────────────
-- C4: orgaosDeputados-L{leg}.csv
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_deputado_orgao (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos do CSV orgaosDeputados-L{leg}.csv
    id_deputado         VARCHAR(20),
    id_orgao            VARCHAR(20),
    sigla_orgao         VARCHAR(30),
    nome_orgao          VARCHAR(300),
    nome_publicacao     VARCHAR(300),
    titulo              VARCHAR(200),
    cod_titulo          VARCHAR(20),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    uri_orgao           VARCHAR(500),

    CONSTRAINT pk_silver_camara_deputado_orgao           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_deputado_orgao_nat_key   UNIQUE      (id_deputado, id_orgao, data_inicio),
    CONSTRAINT fk_silver_camara_deputado_orgao_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_orgao IS 'Silver — participação de deputados em órgãos da Câmara (CSV orgaosDeputados-L{leg}.csv)';

-- ─────────────────────────────────────────────────────────────────────────────
-- C5: frentesDeputados.csv
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_deputado_frente (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos do CSV frentesDeputados.csv
    id_deputado     VARCHAR(20),
    id_frente       VARCHAR(20),
    id_legislatura  VARCHAR(10),
    titulo          VARCHAR(500),
    uri             VARCHAR(500),

    CONSTRAINT pk_silver_camara_deputado_frente           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_deputado_frente_nat_key   UNIQUE      (id_deputado, id_frente),
    CONSTRAINT fk_silver_camara_deputado_frente_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_frente IS 'Silver — participação de deputados em frentes parlamentares (CSV frentesDeputados.csv)';
