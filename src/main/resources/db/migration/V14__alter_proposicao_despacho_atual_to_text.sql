-- V14: Ajusta coluna Gold para suportar despachos longos vindos da Silver
-- Motivo: alguns registros da Câmara trazem ultimo_status_despacho > 1000 chars.

ALTER TABLE proposicao
    ALTER COLUMN despacho_atual TYPE TEXT;
