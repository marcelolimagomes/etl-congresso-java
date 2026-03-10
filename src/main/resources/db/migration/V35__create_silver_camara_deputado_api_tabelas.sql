-- V35: Tabelas Silver — endpoints API-only de deputados da Câmara (A2-A5)
-- Silver Layer — sem CSV equivalente; fonte: API REST da Câmara dos Deputados

-- ─────────────────────────────────────────────────────────────────────────────
-- A2: GET /deputados/{id}/discursos
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_deputado_discurso (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos da API /deputados/{id}/discursos
    camara_deputado_id              VARCHAR(20),
    data_hora_inicio                VARCHAR(30),
    data_hora_fim                   VARCHAR(30),
    tipo_discurso                   VARCHAR(100),
    sumario                         TEXT,
    transcricao                     TEXT,
    keywords                        TEXT,
    url_texto                       VARCHAR(500),
    url_audio                       VARCHAR(500),
    url_video                       VARCHAR(500),
    uri_evento                      VARCHAR(500),
    fase_evento_titulo              VARCHAR(300),
    fase_evento_data_hora_inicio    VARCHAR(30),
    fase_evento_data_hora_fim       VARCHAR(30),

    CONSTRAINT pk_silver_camara_deputado_discurso           PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_dep_discurso_nat_key        UNIQUE      (camara_deputado_id, data_hora_inicio, tipo_discurso),
    CONSTRAINT fk_silver_camara_deputado_discurso_job       FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_discurso IS 'Silver — discursos de deputados da Câmara (API GET /deputados/{id}/discursos)';

-- ─────────────────────────────────────────────────────────────────────────────
-- A3: GET /deputados/{id}/eventos
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_deputado_evento (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos da API /deputados/{id}/eventos
    camara_deputado_id      VARCHAR(20),
    id_evento               VARCHAR(20),
    data_hora_inicio        VARCHAR(30),
    data_hora_fim           VARCHAR(30),
    descricao               TEXT,
    descricao_tipo          VARCHAR(100),
    situacao                VARCHAR(50),
    local_externo           VARCHAR(300),
    uri                     VARCHAR(500),
    url_registro            VARCHAR(500),
    local_camara_nome       VARCHAR(200),
    local_camara_predio     VARCHAR(100),
    local_camara_sala       VARCHAR(50),
    local_camara_andar      VARCHAR(20),
    orgaos                  TEXT,

    CONSTRAINT pk_silver_camara_deputado_evento             PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_dep_evento_nat_key          UNIQUE      (camara_deputado_id, id_evento),
    CONSTRAINT fk_silver_camara_deputado_evento_job         FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_evento IS 'Silver — eventos de deputados da Câmara (API GET /deputados/{id}/eventos)';

-- ─────────────────────────────────────────────────────────────────────────────
-- A4: GET /deputados/{id}/historico
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_deputado_historico (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos da API /deputados/{id}/historico
    camara_deputado_id  VARCHAR(20),
    camara_id_registro  VARCHAR(20),
    id_legislatura      VARCHAR(10),
    nome                VARCHAR(300),
    nome_eleitoral      VARCHAR(300),
    email               VARCHAR(200),
    sigla_partido       VARCHAR(20),
    sigla_uf            VARCHAR(5),
    situacao            VARCHAR(50),
    condicao_eleitoral  VARCHAR(50),
    descricao_status    VARCHAR(500),
    data_hora           VARCHAR(30),
    uri_partido         VARCHAR(500),
    url_foto            VARCHAR(500),

    CONSTRAINT pk_silver_camara_deputado_historico          PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_dep_historico_nat_key       UNIQUE      (camara_deputado_id, data_hora, id_legislatura),
    CONSTRAINT fk_silver_camara_deputado_historico_job      FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_historico IS 'Silver — histórico de status de deputados da Câmara (API GET /deputados/{id}/historico)';

-- ─────────────────────────────────────────────────────────────────────────────
-- A5: GET /deputados/{id}/mandatosExternos
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_deputado_mandato_externo (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos da API /deputados/{id}/mandatosExternos
    camara_deputado_id      VARCHAR(20),
    ano_inicio              VARCHAR(10),
    ano_fim                 VARCHAR(10),
    cargo                   VARCHAR(200),
    sigla_uf                VARCHAR(5),
    municipio               VARCHAR(200),
    sigla_partido_eleicao   VARCHAR(20),
    uri_partido_eleicao     VARCHAR(500),

    CONSTRAINT pk_silver_camara_dep_mandato_externo         PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_dep_mandato_ext_nat_key     UNIQUE      (camara_deputado_id, cargo, sigla_uf, ano_inicio),
    CONSTRAINT fk_silver_camara_dep_mandato_externo_job     FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_deputado_mandato_externo IS 'Silver — mandatos externos de deputados da Câmara (API GET /deputados/{id}/mandatosExternos)';
