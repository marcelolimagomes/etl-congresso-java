-- V2__create_tramitacao.sql
-- Histórico de tramitação de cada proposição

CREATE TABLE tramitacao (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    proposicao_id           UUID         NOT NULL,
    sequencia               INTEGER,
    data_hora               TIMESTAMP,
    sigla_orgao             VARCHAR(50),
    descricao_orgao         VARCHAR(500),
    descricao_tramitacao    TEXT,
    descricao_situacao      VARCHAR(500),
    despacho                TEXT,
    ambito                  VARCHAR(100),
    criado_em               TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tramitacao          PRIMARY KEY (id),
    CONSTRAINT fk_tramitacao_prop     FOREIGN KEY (proposicao_id)
                                      REFERENCES proposicao(id) ON DELETE CASCADE
);

COMMENT ON TABLE tramitacao IS 'Histórico de tramitação de proposições';
