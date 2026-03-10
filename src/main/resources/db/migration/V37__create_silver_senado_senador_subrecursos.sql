-- V37__create_silver_senado_senador_subrecursos.sql
-- Sub-recursos por senador: profissao, mandato, licenca, historico_academico,
-- filiacao, discurso, comissao, cargo, aparte

CREATE TABLE IF NOT EXISTS silver.senado_senador_profissao (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador      VARCHAR(20),
    codigo_profissao    VARCHAR(20),
    descricao_profissao VARCHAR(300),
    data_registro       VARCHAR(30),
    CONSTRAINT uq_silver_senado_sen_prof UNIQUE (codigo_senador, codigo_profissao)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_mandato (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador      VARCHAR(20),
    codigo_mandato      VARCHAR(20),
    descricao           VARCHAR(300),
    uf_mandato          VARCHAR(5),
    participacao        VARCHAR(5),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    data_designacao     VARCHAR(30),
    data_termino        VARCHAR(30),
    entrou_exercicio    VARCHAR(5),
    data_exercicio      VARCHAR(30),
    CONSTRAINT uq_silver_senado_sen_mandato UNIQUE (codigo_senador, codigo_mandato)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_licenca (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador      VARCHAR(20),
    codigo_licenca      VARCHAR(20),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    motivo              VARCHAR(100),
    descricao_motivo    VARCHAR(300),
    CONSTRAINT uq_silver_senado_sen_licenca UNIQUE (codigo_senador, codigo_licenca)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_historico_academico (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador          VARCHAR(20),
    codigo_curso            VARCHAR(20),
    nome_curso              VARCHAR(300),
    instituicao             VARCHAR(300),
    descricao_instituicao   VARCHAR(300),
    nivel_formacao          VARCHAR(100),
    data_inicio_formacao    VARCHAR(30),
    data_termino_formacao   VARCHAR(30),
    concluido               VARCHAR(5),
    CONSTRAINT uq_silver_senado_sen_hist_acad UNIQUE (codigo_senador, codigo_curso)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_filiacao (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador          VARCHAR(20),
    codigo_filiacao         VARCHAR(20),
    codigo_partido          VARCHAR(20),
    sigla_partido           VARCHAR(20),
    nome_partido            VARCHAR(200),
    data_inicio_filiacao    VARCHAR(30),
    data_termino_filiacao   VARCHAR(30),
    CONSTRAINT uq_silver_senado_sen_filiacao UNIQUE (codigo_senador, codigo_filiacao)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_discurso (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador          VARCHAR(20),
    codigo_discurso         VARCHAR(20),
    codigo_sessao           VARCHAR(20),
    data_pronunciamento     VARCHAR(30),
    casa                    VARCHAR(10),
    tipo_sessao             VARCHAR(100),
    numero_sessao           VARCHAR(20),
    tipo_pronunciamento     VARCHAR(100),
    texto_discurso          TEXT,
    duracao_aparte          VARCHAR(20),
    url_video               VARCHAR(500),
    url_audio               VARCHAR(500),
    CONSTRAINT uq_silver_senado_sen_discurso UNIQUE (codigo_senador, codigo_discurso)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_comissao (
    id                          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id                  UUID,
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMP,
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado           BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador              VARCHAR(20),
    codigo_comissao             VARCHAR(20),
    sigla_comissao              VARCHAR(30),
    nome_comissao               VARCHAR(300),
    cargo                       VARCHAR(100),
    data_inicio_participacao    VARCHAR(30),
    data_termino_participacao   VARCHAR(30),
    ativo                       VARCHAR(5),
    CONSTRAINT uq_silver_senado_sen_comissao UNIQUE (codigo_senador, codigo_comissao, data_inicio_participacao)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_cargo (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador      VARCHAR(20),
    codigo_cargo        VARCHAR(20),
    descricao_cargo     VARCHAR(300),
    tipo_cargo          VARCHAR(100),
    comissao_ou_orgao   VARCHAR(300),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    CONSTRAINT uq_silver_senado_sen_cargo UNIQUE (codigo_senador, codigo_cargo, data_inicio)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_aparte (
    id                          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id                  UUID,
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMP,
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado           BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador              VARCHAR(20),
    codigo_aparte               VARCHAR(20),
    codigo_discurso_principal   VARCHAR(20),
    codigo_sessao               VARCHAR(20),
    data_pronunciamento         VARCHAR(30),
    casa                        VARCHAR(10),
    texto_aparte                TEXT,
    url_video                   VARCHAR(500),
    CONSTRAINT uq_silver_senado_sen_aparte UNIQUE (codigo_senador, codigo_aparte)
);
