-- V19: Tabela silver.camara_proposicao_autor
-- Silver Layer — Câmara autores: espelha CSV proposicoesAutores-{ano}.csv
-- Colunas verificadas em 2024: idProposicao;uriProposicao;idDeputadoAutor;uriAutor;
--   codTipoAutor;tipoAutor;nomeAutor;siglaPartidoAutor;uriPartidoAutor;siglaUFAutor;
--   ordemAssinatura;proponente

CREATE TABLE silver.camara_proposicao_autor (

    -- ── Controle ──────────────────────────────────────────────────────────────
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    camara_proposicao_id    UUID,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP   NOT NULL DEFAULT NOW(),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'CSV',

    -- ── Campos do CSV proposicoesAutores-{ano}.csv (espelho fiel) ─────────────
    id_proposicao           VARCHAR(50),
    uri_proposicao          VARCHAR(500),
    id_deputado_autor       VARCHAR(50),
    uri_autor               VARCHAR(500),
    cod_tipo_autor          VARCHAR(50),
    tipo_autor              VARCHAR(100),
    nome_autor              VARCHAR(500),
    sigla_partido_autor     VARCHAR(20),
    uri_partido_autor       VARCHAR(500),
    sigla_uf_autor          VARCHAR(5),
    ordem_assinatura        INTEGER,
    proponente              INTEGER,

    CONSTRAINT pk_silver_camara_proposicao_autor PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_proposicao_autor
        UNIQUE (uri_proposicao, nome_autor, ordem_assinatura),
    CONSTRAINT fk_silver_camara_proposicao_autor_proposicao FOREIGN KEY (camara_proposicao_id)
        REFERENCES silver.camara_proposicao(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_silver_camara_proposicao_autor_job FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

CREATE INDEX idx_silver_camara_proposicao_autor_proposicao_id
    ON silver.camara_proposicao_autor(camara_proposicao_id);

CREATE INDEX idx_silver_camara_proposicao_autor_uri_proposicao
    ON silver.camara_proposicao_autor(uri_proposicao);

CREATE INDEX idx_silver_camara_proposicao_autor_etl_job
    ON silver.camara_proposicao_autor(etl_job_id);

COMMENT ON TABLE silver.camara_proposicao_autor IS
    'Silver — autores de proposições da Câmara, fiel ao CSV proposicoesAutores-{ano}.csv';
