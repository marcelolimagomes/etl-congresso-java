-- V40: Corrige colunas VARCHAR limitadas em sub-tabelas Silver da Câmara
-- cujos CSVs contêm valores que excedem o limite original.

-- camara_deputado_ocupacao
ALTER TABLE silver.camara_deputado_ocupacao ALTER COLUMN entidade TYPE TEXT;
ALTER TABLE silver.camara_deputado_ocupacao ALTER COLUMN titulo TYPE TEXT;

-- camara_deputado_orgao
ALTER TABLE silver.camara_deputado_orgao ALTER COLUMN nome_orgao TYPE TEXT;
ALTER TABLE silver.camara_deputado_orgao ALTER COLUMN nome_publicacao TYPE TEXT;
ALTER TABLE silver.camara_deputado_orgao ALTER COLUMN titulo TYPE TEXT;
