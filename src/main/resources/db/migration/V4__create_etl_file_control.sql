-- V4__create_etl_file_control.sql
-- Controle de ingestão de arquivos CSV (idempotência por checksum)

CREATE TABLE etl_file_control (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    origem                  VARCHAR(20)  NOT NULL DEFAULT 'CAMARA',
    nome_arquivo            VARCHAR(255) NOT NULL,
    url_download            VARCHAR(500),
    checksum_sha256         VARCHAR(64),
    tamanho_bytes           BIGINT,
    ano_referencia          INTEGER,
    data_referencia         DATE,
    processado_em           TIMESTAMP,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    forcar_reprocessamento  BOOLEAN      NOT NULL DEFAULT FALSE,
    job_id                  UUID,
    criado_em               TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_etl_file_control  PRIMARY KEY (id),
    CONSTRAINT uq_file_control      UNIQUE (origem, nome_arquivo),
    CONSTRAINT fk_file_control_job  FOREIGN KEY (job_id)
                                    REFERENCES etl_job_control(id)
);

COMMENT ON TABLE  etl_file_control                   IS 'Controle de arquivos CSV — evita reprocessamento desnecessário';
COMMENT ON COLUMN etl_file_control.checksum_sha256   IS 'SHA-256 do conteúdo — se igual ao anterior, arquivo é ignorado';
COMMENT ON COLUMN etl_file_control.forcar_reprocessamento IS 'TRUE = processar mesmo que checksum seja o mesmo';
