-- V15: Corrige tamanho da coluna det_casa_iniciadora em silver.senado_materia
-- O fallback em applyDetalhe() usa nomeCasaIniciadora (ex: "Senado Federal" = 15 chars),
-- mas a coluna estava definida como VARCHAR(10) — risco de truncamento silencioso.
-- Ref: plano-ingestao-senado-outras-informacoes.md, Seção 6.2

ALTER TABLE silver.senado_materia
    ALTER COLUMN det_casa_iniciadora TYPE VARCHAR(100);

COMMENT ON COLUMN silver.senado_materia.det_casa_iniciadora
    IS 'Sigla ou nome da casa iniciadora (ex: "SF", "CD", "Senado Federal"); ampliado de VARCHAR(10) para VARCHAR(100) — V15';
