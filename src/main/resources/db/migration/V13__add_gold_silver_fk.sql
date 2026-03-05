-- V13: Rastreabilidade Silver→Gold
-- Adiciona colunas FK nas tabelas Gold para linkagem bidirecional com Silver

-- ── proposicao ───────────────────────────────────────────────────────────────
ALTER TABLE public.proposicao
    ADD COLUMN silver_camara_id UUID,
    ADD COLUMN silver_senado_id UUID;

-- FKs com DEFERRABLE para permitir inserção em mesma transação
ALTER TABLE public.proposicao
    ADD CONSTRAINT fk_proposicao_silver_camara
        FOREIGN KEY (silver_camara_id)
        REFERENCES silver.camara_proposicao(id)
        DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT fk_proposicao_silver_senado
        FOREIGN KEY (silver_senado_id)
        REFERENCES silver.senado_materia(id)
        DEFERRABLE INITIALLY DEFERRED;

COMMENT ON COLUMN public.proposicao.silver_camara_id IS
    'FK para silver.camara_proposicao — rastreabilidade bidirecional Silver→Gold';
COMMENT ON COLUMN public.proposicao.silver_senado_id IS
    'FK para silver.senado_materia — rastreabilidade bidirecional Silver→Gold';

-- ── tramitacao ────────────────────────────────────────────────────────────────
ALTER TABLE public.tramitacao
    ADD COLUMN silver_camara_tramitacao_id UUID,
    ADD COLUMN silver_senado_movimentacao_id UUID;

ALTER TABLE public.tramitacao
    ADD CONSTRAINT fk_tramitacao_silver_camara
        FOREIGN KEY (silver_camara_tramitacao_id)
        REFERENCES silver.camara_tramitacao(id)
        DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT fk_tramitacao_silver_senado
        FOREIGN KEY (silver_senado_movimentacao_id)
        REFERENCES silver.senado_movimentacao(id)
        DEFERRABLE INITIALLY DEFERRED;

COMMENT ON COLUMN public.tramitacao.silver_camara_tramitacao_id IS
    'FK para silver.camara_tramitacao — rastreabilidade Silver→Gold';
COMMENT ON COLUMN public.tramitacao.silver_senado_movimentacao_id IS
    'FK para silver.senado_movimentacao — rastreabilidade Silver→Gold';
