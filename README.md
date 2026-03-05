# ETL Congresso Java

Pipeline ETL para matérias e proposições legislativas (Câmara e Senado) com Java 21, Spring Boot e PostgreSQL.

## Operação de Ingestão

- Guia completo: [docs/operacao-ingestao.md](docs/operacao-ingestao.md)
- Script: [scripts/ingest.sh](scripts/ingest.sh)
- Ambientes:
  - [config/env/ingest-host.env](config/env/ingest-host.env)
  - [config/env/ingest-container.env](config/env/ingest-container.env)

## Atalhos úteis

- Exibir ajuda: `make help`
- Ver guia de ingestão: `make ingest-doc`
- Disparar ingestão (exemplo):

```bash
make ingest ENV=host MODE=camara-full ANO_INI=2024 ANO_FIM=2024
```
