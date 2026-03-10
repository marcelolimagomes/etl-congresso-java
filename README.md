# ETL Congresso Java

Pipeline ETL para matérias e proposições legislativas (Câmara e Senado) com Java 21, Spring Boot e PostgreSQL.

## Operação de Ingestão

- Guia completo: [docs/operacao-ingestao.md](docs/operacao-ingestao.md)
- Script: [scripts/ingest.sh](scripts/ingest.sh)
- Ambientes:
  - [config/env/ingest-host.env](config/env/ingest-host.env)
  - [config/env/ingest-container.env](config/env/ingest-container.env)

## Geração de Páginas Estáticas

O pipeline inclui um serviço Java que gera as páginas HTML estáticas das proposições diretamente a partir do banco Gold.
As páginas são salvas em `open-data/public/proposicoes/{casa}-{idOriginal}/index.html` e consumidas pelo front-end Nuxt.

### Variáveis de ambiente

| Variável                 | Padrão             | Descrição                             |
| ------------------------ | ------------------ | ------------------------------------- |
| `ETL_PAGEGEN_OUTPUT_DIR` | `open-data/public` | Diretório raiz de saída               |
| `ETL_PAGEGEN_BATCH_SIZE` | `500`              | Proposições por lote de processamento |

### Via Makefile (requer app rodando em localhost:8080)

```bash
# Gerar todas as páginas
make pages-generate

# Gerar apenas as páginas de um ano específico
make pages-generate-ano ANO=2024

# Verificar status
make pages-status
```

### Via script `ingest.sh`

```bash
# Gerar todas as páginas
./scripts/ingest.sh --env host --mode pages-generate

# Gerar por ano
./scripts/ingest.sh --env host --mode pages-generate-ano --ano 2024

# Verificar status
./scripts/ingest.sh --env host --mode pages-status
```

### Via API REST (admin)

```bash
# Gerar tudo
curl -X POST -u admin:changeme http://localhost:8080/admin/etl/pages/generate

# Gerar por ano
curl -X POST -u admin:changeme "http://localhost:8080/admin/etl/pages/generate?ano=2024"

# Verificar status
curl -u admin:changeme http://localhost:8080/admin/etl/pages/status
```

A geração é **assíncrona** — a API retorna `202 Accepted` imediatamente e o processamento continua em background.
Um `sitemap-proposicoes.xml` é gerado ao final de cada execução.

## Atalhos úteis

- Exibir ajuda: `make help`
- Ver guia de ingestão: `make ingest-doc`
- Disparar ingestão (exemplo):

```bash
make ingest ENV=host MODE=camara-full ANO_INI=2024 ANO_FIM=2024
```
