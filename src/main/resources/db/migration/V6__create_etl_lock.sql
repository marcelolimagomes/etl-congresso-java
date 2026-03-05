-- V6__create_etl_lock.sql
-- Lock distribuído para evitar execução paralela do mesmo recurso/ano

CREATE TABLE etl_lock (
    recurso     VARCHAR(100) NOT NULL,
    locked_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    locked_by   VARCHAR(100),
    expires_at  TIMESTAMP    NOT NULL,

    CONSTRAINT pk_etl_lock PRIMARY KEY (recurso)
);

COMMENT ON TABLE  etl_lock            IS 'Lock distribuído por recurso — impede execuções paralelas do mesmo ano/fonte';
COMMENT ON COLUMN etl_lock.recurso    IS 'Identificador do recurso, ex: camara_2024, senado_full';
COMMENT ON COLUMN etl_lock.locked_by  IS 'Hostname/instância que detém o lock (diagnóstico)';
COMMENT ON COLUMN etl_lock.expires_at IS 'Data de expiração automática do lock (evita deadlock permanente)';
