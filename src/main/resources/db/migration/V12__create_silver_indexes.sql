-- V12: Índices no schema silver para performance

-- ── silver.camara_proposicao ─────────────────────────────────────────────────
CREATE INDEX idx_silver_camara_prop_ano
    ON silver.camara_proposicao (ano);

CREATE INDEX idx_silver_camara_prop_sigla_tipo
    ON silver.camara_proposicao (sigla_tipo);

CREATE INDEX idx_silver_camara_prop_gold
    ON silver.camara_proposicao (gold_sincronizado)
    WHERE gold_sincronizado = FALSE;

CREATE INDEX idx_silver_camara_prop_hash
    ON silver.camara_proposicao (content_hash);

-- ── silver.camara_tramitacao ─────────────────────────────────────────────────
CREATE INDEX idx_silver_camara_tram_proposicao
    ON silver.camara_tramitacao (camara_proposicao_id);

-- ── silver.senado_materia ────────────────────────────────────────────────────
CREATE INDEX idx_silver_senado_mat_ano
    ON silver.senado_materia (ano);

CREATE INDEX idx_silver_senado_mat_sigla
    ON silver.senado_materia (sigla);

CREATE INDEX idx_silver_senado_mat_gold
    ON silver.senado_materia (gold_sincronizado)
    WHERE gold_sincronizado = FALSE;

CREATE INDEX idx_silver_senado_mat_hash
    ON silver.senado_materia (content_hash);

-- ── silver.senado_movimentacao ───────────────────────────────────────────────
CREATE INDEX idx_silver_senado_mov_materia
    ON silver.senado_movimentacao (senado_materia_id);
