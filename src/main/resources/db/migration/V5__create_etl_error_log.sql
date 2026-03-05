-- V5__create_etl_error_log.sql
-- Log persistente de erros durante execuções ETL

CREATE TABLE etl_error_log (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    job_id       UUID,
    origem       VARCHAR(20)  NOT NULL,
    tipo_erro    VARCHAR(100),
    endpoint     VARCHAR(500),
    payload      TEXT,
    mensagem     TEXT,
    stack_trace  TEXT,
    tentativas   INTEGER      NOT NULL DEFAULT 1,
    criado_em    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_etl_error_log PRIMARY KEY (id),
    CONSTRAINT fk_error_log_job FOREIGN KEY (job_id)
                                REFERENCES etl_job_control(id)
);

COMMENT ON TABLE etl_error_log IS 'Log de erros durante execuções ETL — permite diagnóstico e reprocessamento seletivo';
