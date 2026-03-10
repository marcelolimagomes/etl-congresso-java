-- V34: Tabela Silver — Despesas CEAP da Câmara dos Deputados
-- Silver Layer — espelha o CSV Ano-{ano}.csv (Cota Parlamentar — CEAP)

-- ─────────────────────────────────────────────────────────────────────────────
-- C7: Ano-{ano}.csv (http://www.camara.leg.br/cotas/Ano-{ano}.csv.zip)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE silver.camara_despesa (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Campos do CSV Ano-{ano}.csv (CEAP / Cota Parlamentar)
    camara_deputado_id      VARCHAR(20),
    ano                     VARCHAR(10),
    mes                     VARCHAR(5),
    tipo_despesa            VARCHAR(300),
    cod_documento           VARCHAR(20),
    tipo_documento          VARCHAR(100),
    cod_tipo_documento      VARCHAR(10),
    data_documento          VARCHAR(30),
    num_documento           VARCHAR(50),
    parcela                 VARCHAR(10),
    valor_documento         VARCHAR(30),
    valor_glosa             VARCHAR(30),
    valor_liquido           VARCHAR(30),
    nome_fornecedor         VARCHAR(300),
    cnpj_cpf_fornecedor     VARCHAR(20),
    num_ressarcimento       VARCHAR(50),
    url_documento           VARCHAR(500),
    cod_lote                VARCHAR(20),

    CONSTRAINT pk_silver_camara_despesa         PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_despesa_nat_key UNIQUE      (camara_deputado_id, cod_documento, num_documento, parcela),
    CONSTRAINT fk_silver_camara_despesa_job     FOREIGN KEY (etl_job_id)
        REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_despesa IS 'Silver — despesas CEAP da Câmara dos Deputados (CSV Ano-{ano}.csv.zip)';
COMMENT ON COLUMN silver.camara_despesa.camara_deputado_id IS 'ideCadastro do deputado na Câmara (campo ideCadastro no CSV)';
COMMENT ON COLUMN silver.camara_despesa.cod_documento      IS 'Código do documento (campo ideDocumento no CSV)';
COMMENT ON COLUMN silver.camara_despesa.num_documento      IS 'Número do documento (campo txtNumero no CSV)';
COMMENT ON COLUMN silver.camara_despesa.parcela            IS 'Parcela do documento (campo numParcela no CSV)';

CREATE INDEX idx_silver_camara_despesa_ano  ON silver.camara_despesa (ano);
CREATE INDEX idx_silver_camara_despesa_dep  ON silver.camara_despesa (camara_deputado_id);
