-- V39: Amplia descricao_status em camara_deputado_historico de VARCHAR(500) para TEXT
-- Motivação: a API da Câmara retorna descrições de status com mais de 500 caracteres
--            (ex: despachos de convocação), causando "value too long for type character varying(500)".

ALTER TABLE silver.camara_deputado_historico
    ALTER COLUMN descricao_status TYPE TEXT;
