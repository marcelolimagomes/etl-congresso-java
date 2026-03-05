-- V3__create_etl_job_control.sql
-- Auditoria e rastreabilidade de execuções do pipeline ETL

CREATE TABLE etl_job_control (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    origem           VARCHAR(20)  NOT NULL,
    tipo_execucao    VARCHAR(20)  NOT NULL,
    iniciado_em      TIMESTAMP    NOT NULL DEFAULT NOW(),
    finalizado_em    TIMESTAMP,
    total_processado INTEGER      NOT NULL DEFAULT 0,
    total_inserido   INTEGER      NOT NULL DEFAULT 0,
    total_atualizado INTEGER      NOT NULL DEFAULT 0,
    total_ignorados  INTEGER      NOT NULL DEFAULT 0,
    total_erros      INTEGER      NOT NULL DEFAULT 0,
    status           VARCHAR(50)  NOT NULL DEFAULT 'RUNNING',
    mensagem_erro    TEXT,
    parametros       JSONB,

    CONSTRAINT pk_etl_job_control PRIMARY KEY (id)
);

COMMENT ON TABLE  etl_job_control              IS 'Auditoria de execuções do ETL';
COMMENT ON COLUMN etl_job_control.origem       IS 'CAMARA ou SENADO';
COMMENT ON COLUMN etl_job_control.tipo_execucao IS 'FULL, INCREMENTAL ou FORCED';
COMMENT ON COLUMN etl_job_control.status       IS 'RUNNING, SUCCESS, FAILED, PARTIAL';
COMMENT ON COLUMN etl_job_control.parametros   IS 'JSON com parâmetros de execução (anos, datas, etc.)';
