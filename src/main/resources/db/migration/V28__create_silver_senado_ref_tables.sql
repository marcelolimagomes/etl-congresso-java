-- Phase 8: Tabelas de Referência/Domínio do Senado Federal
-- Fonte: endpoints GET /dadosabertos/processo/tipos-* e /processo/siglas|classes|assuntos
-- Estratégia: Full replace (truncate + insert) — dados estáticos atualizados periodicamente

-- ── 1. silver.senado_ref_tipo_situacao ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS silver.senado_ref_tipo_situacao (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id  UUID        REFERENCES etl_job_control(id),
    ingerido_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    codigo      VARCHAR(50) NOT NULL UNIQUE,
    descricao   VARCHAR(500)
);

-- ── 2. silver.senado_ref_tipo_decisao ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS silver.senado_ref_tipo_decisao (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id  UUID        REFERENCES etl_job_control(id),
    ingerido_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    codigo      VARCHAR(50) NOT NULL UNIQUE,
    descricao   VARCHAR(500)
);

-- ── 3. silver.senado_ref_tipo_autor ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS silver.senado_ref_tipo_autor (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id  UUID        REFERENCES etl_job_control(id),
    ingerido_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    codigo      VARCHAR(50) NOT NULL UNIQUE,
    descricao   VARCHAR(500)
);

-- ── 4. silver.senado_ref_sigla ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS silver.senado_ref_sigla (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id  UUID        REFERENCES etl_job_control(id),
    ingerido_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    sigla       VARCHAR(30) NOT NULL UNIQUE,
    descricao   VARCHAR(500),
    classe      VARCHAR(200)
);

-- ── 5. silver.senado_ref_classe ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS silver.senado_ref_classe (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id  UUID        REFERENCES etl_job_control(id),
    ingerido_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    codigo      VARCHAR(50) NOT NULL UNIQUE,
    descricao   VARCHAR(500),
    classe_pai  VARCHAR(50)
);

-- ── 6. silver.senado_ref_assunto ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS silver.senado_ref_assunto (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id          UUID        REFERENCES etl_job_control(id),
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    codigo              VARCHAR(50) NOT NULL UNIQUE,
    assunto_geral       VARCHAR(500),
    assunto_especifico  VARCHAR(500)
);
