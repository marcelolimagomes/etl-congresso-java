# Relatorio de Validacao ETL 2024

Data: 2026-03-06
Escopo: Ingestao Camara e Senado (Silver), promocao para Gold (Camara e Senado)

## Resumo Executivo

- Camara Silver 2024: 5419 registros
- Senado Silver 2024: 1433 registros
- Comparacao fonte x Silver: OK para Camara e Senado
- Problema identificado na promocao Camara->Gold: `despacho_atual` limitado a `varchar(1000)`
- Correcao aplicada: alteracao de schema para `TEXT`
- Reprocessamento executado: 500 registros promovidos com sucesso
- Estado final Camara 2024: Silver 5419, Gold 5419, faltando 0

## Evidencias de Jobs

- `CAMARA FULL` (id `6c112fb4-c315-4793-88d7-a20ee4f20427`): `SUCCESS`, inseridos 5419
- `CAMARA PROMOCAO` (id `c0842e7f-dc4d-4f00-9702-828b7576d651`): `PARTIAL`, erros 500
- `CAMARA FULL` (id `5b76ccd1-96e6-444b-80d9-9a267ba372a1`): `SUCCESS`
- `CAMARA PROMOCAO` (id `f6acb1ce-2422-4ebc-8154-82ccf5f88519`): `SUCCESS`, processado 500, inserido 500, erros 0
- `SENADO FULL` (id `37dcb0f7-aae1-4bbe-9812-c4e440946a7d`): `SUCCESS`, inseridos 1433
- `SENADO PROMOCAO` (id `f0266a92-007d-47af-ae4a-0f9c61c8b2ba`): `SUCCESS`, inseridos 1431, atualizados 2

## Correcao Tecnica Aplicada

- Alteracao em runtime no banco:

```sql
ALTER TABLE proposicao
    ALTER COLUMN despacho_atual TYPE TEXT;
```

- Backfill dos registros faltantes na promocao:

```sql
UPDATE silver.camara_proposicao s
SET gold_sincronizado = false
WHERE s.ano = 2024
  AND NOT EXISTS (
    SELECT 1 FROM proposicao p WHERE p.silver_camara_id = s.id
  );
```

- Correcao persistida no codigo:
  - `src/main/java/br/leg/congresso/etl/domain/Proposicao.java`
  - `src/main/resources/db/migration/V14__alter_proposicao_despacho_atual_to_text.sql`

## Validacao Fonte x Silver (2024)

### Camara

- Fonte: CSV oficial `proposicoes-2024.csv`
- Linhas lidas: 61510
- Linhas aceitas pelo filtro de tipos do ETL: 5419
- `silver.camara_proposicao` (ano=2024): 5419
- Resultado: match

Amostra comparada:

- API/CSV: `id=253500`, `siglaTipo=PL`, `numero=2597`, `ano=2024`
- DB Silver: `camara_id=253500`, `sigla_tipo=PL`, `numero=2597`, `ano=2024`

### Senado

- Fonte: `GET /dadosabertos/materia/pesquisa/lista.json` por sigla e `ano=2024`
- Total filtrado pelos tipos do ETL: 1433
- `silver.senado_materia` (ano=2024): 1433
- Resultado: match

Amostra comparada:

- API: `Codigo=161856`, `Sigla=PL`, `Numero=00001`, `Ano=2024`
- DB Silver: `codigo=161856`, `sigla=PL`, `numero=00001`, `ano=2024`

## Validacao Final Silver x Gold (Camara 2024)

Consulta de reconciliacao:

```sql
SELECT COUNT(*) AS gold_camara_2024
FROM proposicao
WHERE casa = 'CAMARA' AND ano = 2024;

SELECT COUNT(*) AS silver_camara_2024
FROM silver.camara_proposicao
WHERE ano = 2024;

SELECT COUNT(*) AS faltando_gold
FROM silver.camara_proposicao s
LEFT JOIN proposicao p ON p.silver_camara_id = s.id
WHERE s.ano = 2024 AND p.id IS NULL;
```

Resultado final:

- gold_camara_2024: 5419
- silver_camara_2024: 5419
- faltando_gold: 0
