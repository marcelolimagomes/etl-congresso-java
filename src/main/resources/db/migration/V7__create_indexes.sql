-- V7__create_indexes.sql
-- Índices estratégicos para performance de consultas ETL e analíticas

-- proposicao
CREATE INDEX idx_proposicao_tipo         ON proposicao(tipo);
CREATE INDEX idx_proposicao_ano          ON proposicao(ano);
CREATE INDEX idx_proposicao_casa         ON proposicao(casa);
CREATE INDEX idx_proposicao_status_final ON proposicao(status_final);
CREATE INDEX idx_proposicao_data_update  ON proposicao(data_atualizacao);
CREATE INDEX idx_proposicao_virou_lei    ON proposicao(virou_lei) WHERE virou_lei = TRUE;
CREATE INDEX idx_proposicao_id_origem    ON proposicao(id_origem, casa);

-- tramitacao
CREATE INDEX idx_tramitacao_proposicao   ON tramitacao(proposicao_id);
CREATE INDEX idx_tramitacao_data         ON tramitacao(data_hora);

-- etl_job_control
CREATE INDEX idx_job_control_origem      ON etl_job_control(origem, tipo_execucao);
CREATE INDEX idx_job_control_status      ON etl_job_control(status);
CREATE INDEX idx_job_control_iniciado    ON etl_job_control(iniciado_em DESC);

-- etl_file_control
CREATE INDEX idx_file_control_status     ON etl_file_control(status, forcar_reprocessamento);
CREATE INDEX idx_file_control_ano        ON etl_file_control(ano_referencia);
