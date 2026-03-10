-- V36: Tabelas Silver — Senadores do Senado Federal (S1, S4, S5, S6)
-- Silver Layer — fonte: API REST do Senado Federal
-- S1: GET /senador/lista/atual + complemento GET /senador/{codigo}
-- S4: GET /senador/afastados
-- S5: GET /senador/partidos
-- S6: GET /senador/lista/tiposUsoPalavra

-- ─────────────────────────────────────────────────────────────────────────────
-- S1: GET /senador/lista/atual (+ complemento GET /senador/{codigo})
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.senado_senador (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Colunas da listagem (GET /senador/lista/atual)
    codigo_senador              VARCHAR(20),
    nome_parlamentar            VARCHAR(300),
    nome_civil                  VARCHAR(300),
    sexo                        VARCHAR(5),
    uf_parlamentar              VARCHAR(5),
    participacao                VARCHAR(5),
    partido_parlamentar         VARCHAR(100),
    sigla_partido_parlamentar   VARCHAR(20),
    data_designacao             VARCHAR(30),
    codigo_legislatura          VARCHAR(10),

    -- Colunas de complemento API (GET /senador/{codigo})
    det_nome_completo           VARCHAR(300),
    det_data_nascimento         VARCHAR(30),
    det_local_nascimento        VARCHAR(200),
    det_estado_civil            VARCHAR(50),
    det_escolaridade            VARCHAR(100),
    det_contato_email           VARCHAR(200),
    det_url_foto                VARCHAR(500),
    det_url_pagina_parlamentar  VARCHAR(500),
    det_pagina                  VARCHAR(500),
    det_facebook                VARCHAR(500),
    det_twitter                 VARCHAR(500),
    det_profissoes              TEXT,

    CONSTRAINT pk_silver_senado_senador                 PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_senador_nat_key         UNIQUE      (codigo_senador),
    CONSTRAINT fk_silver_senado_senador_job             FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.senado_senador IS 'Silver — senadores do Senado Federal (API GET /senador/lista/atual + GET /senador/{codigo})';

-- ─────────────────────────────────────────────────────────────────────────────
-- S4: GET /senador/afastados
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.senado_senador_afastado (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos da API /senador/afastados
    codigo_senador              VARCHAR(20),
    nome_parlamentar            VARCHAR(300),
    uf_mandato                  VARCHAR(5),
    motivo_afastamento          VARCHAR(300),
    data_afastamento            VARCHAR(30),
    data_termino_afastamento    VARCHAR(30),

    CONSTRAINT pk_silver_senado_senador_afastado            PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_senador_afastado_nat_key    UNIQUE      (codigo_senador, data_afastamento),
    CONSTRAINT fk_silver_senado_senador_afastado_job        FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.senado_senador_afastado IS 'Silver — senadores afastados do Senado Federal (API GET /senador/afastados)';

-- ─────────────────────────────────────────────────────────────────────────────
-- S5: GET /senador/partidos
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.senado_partido (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos da API /senador/partidos
    codigo_partido      VARCHAR(20),
    sigla_partido       VARCHAR(20),
    nome_partido        VARCHAR(200),
    data_ativacao       VARCHAR(30),
    data_desativacao    VARCHAR(30),

    CONSTRAINT pk_silver_senado_partido             PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_partido_nat_key     UNIQUE      (codigo_partido),
    CONSTRAINT fk_silver_senado_partido_job         FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.senado_partido IS 'Silver — partidos do Senado Federal (API GET /senador/partidos)';

-- ─────────────────────────────────────────────────────────────────────────────
-- S6: GET /senador/lista/tiposUsoPalavra
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.senado_tipo_uso_palavra (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos da API /senador/lista/tiposUsoPalavra
    codigo_tipo     VARCHAR(20),
    descricao_tipo  VARCHAR(200),
    abreviatura     VARCHAR(20),

    CONSTRAINT pk_silver_senado_tipo_uso_palavra                PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_tipo_uso_palavra_nat_key        UNIQUE      (codigo_tipo),
    CONSTRAINT fk_silver_senado_tipo_uso_palavra_job            FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.senado_tipo_uso_palavra IS 'Silver — tipos de uso da palavra no Senado Federal (API GET /senador/lista/tiposUsoPalavra)';
