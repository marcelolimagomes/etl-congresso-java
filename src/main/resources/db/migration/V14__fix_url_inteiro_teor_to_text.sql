-- V14__fix_url_inteiro_teor_to_text.sql
-- Consolida dois ajustes de coluna na tabela Gold proposicao:
--   1) Amplia url_inteiro_teor de VARCHAR(1000) para TEXT (Gold + Silver);
--      proposituras da Câmara podem ter URLs > 1000 chars.
--   2) Amplia despacho_atual para TEXT para suportar despachos longos da Câmara.

ALTER TABLE proposicao
    ALTER COLUMN url_inteiro_teor TYPE TEXT;

ALTER TABLE proposicao
    ALTER COLUMN despacho_atual TYPE TEXT;

ALTER TABLE silver.camara_proposicao
    ALTER COLUMN url_inteiro_teor TYPE TEXT;

COMMENT ON COLUMN proposicao.url_inteiro_teor
    IS 'URL do inteiro teor do documento (sem limite de tamanho)';

COMMENT ON COLUMN silver.camara_proposicao.url_inteiro_teor
    IS 'URL do inteiro teor do documento (sem limite de tamanho)';
