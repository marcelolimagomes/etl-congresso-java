-- V38: Corrige tamanhos de colunas VARCHAR(5) que recebem texto descritivo completo
-- da API do Senado Federal (ex: "Titular", "Suplente").

-- senado_senador_mandato: participacao armazena "Titular"/"Suplente" (até 8 chars)
ALTER TABLE silver.senado_senador_mandato
  ALTER COLUMN participacao TYPE VARCHAR(20);

-- Tabelas de segurança preventiva: entrou_exercicio pode retornar texto maior
-- que 5 chars dependendo da versão da API.
ALTER TABLE silver.senado_senador_mandato
  ALTER COLUMN entrou_exercicio TYPE VARCHAR(20);

-- senado_senador_comissao: ativo armazena "Sim"/"Não" (3 chars) — OK em VARCHAR(5),
-- mas ampliamos por precaução uniformizando com as outras tabelas de indicadores.
ALTER TABLE silver.senado_senador_comissao
  ALTER COLUMN ativo TYPE VARCHAR(20);
