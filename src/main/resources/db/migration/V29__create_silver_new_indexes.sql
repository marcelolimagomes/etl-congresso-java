-- Phase 8: Índices para as novas tabelas Silver (V18–V28)
-- Convenção: idx_{tabela}_{campo} → aceleração de consultas por job e por chave natural

-- ── senado_ref_tipo_situacao ──────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_senado_ref_tipo_situacao_job ON silver.senado_ref_tipo_situacao(etl_job_id);

-- ── senado_ref_tipo_decisao ───────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_senado_ref_tipo_decisao_job ON silver.senado_ref_tipo_decisao(etl_job_id);

-- ── senado_ref_tipo_autor ─────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_senado_ref_tipo_autor_job ON silver.senado_ref_tipo_autor(etl_job_id);

-- ── senado_ref_sigla ──────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_senado_ref_sigla_job ON silver.senado_ref_sigla(etl_job_id);

-- ── senado_ref_classe ─────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_senado_ref_classe_job ON silver.senado_ref_classe(etl_job_id);

-- ── senado_ref_assunto ────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_senado_ref_assunto_job ON silver.senado_ref_assunto(etl_job_id);
