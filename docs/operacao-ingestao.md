# Guia de Operação — Ingestão ETL

Este guia descreve como disparar a ingestão usando o script `scripts/ingest.sh`, separando os ambientes:

- `host`: Java rodando localmente + PostgreSQL no container
- `container`: aplicação Java já rodando em container

## 1) Pré-requisitos

- Docker e Docker Compose instalados
- Java 21 instalado
- Maven disponível em `mvn` ou em `/tmp/apache-maven-3.9.9/bin/mvn`

## 2) Arquivos de ambiente

- `config/env/ingest-host.env`
- `config/env/ingest-container.env`

Esses arquivos isolam as variáveis por ambiente (`APP_BASE_URL`, credenciais admin, datasource, profile Spring e estratégia de inicialização da app).

## 3) Modos suportados

- `status`
- `jobs`
- `camara-full`
- `camara-incremental`
- `camara-reprocess`
- `senado-full`
- `senado-incremental`
- `senado-reprocess`

## 4) Exemplos (script direto)

Status geral:

```bash
./scripts/ingest.sh --env host --mode status
```

Listar jobs:

```bash
./scripts/ingest.sh --env host --mode jobs
```

Carga total da Câmara (ano fechado):

```bash
./scripts/ingest.sh --env host --mode camara-full --ano-inicio 2024 --ano-fim 2024
```

Incremental do Senado por intervalo:

```bash
./scripts/ingest.sh --env host --mode senado-incremental --data-inicio 2026-03-01 --data-fim 2026-03-05
```

Reprocessamento Câmara:

```bash
./scripts/ingest.sh --env host --mode camara-reprocess --ano 2024
```

Reprocessamento Senado:

```bash
./scripts/ingest.sh --env host --mode senado-reprocess --data 2026-03-05
```

## 5) Exemplos via Makefile

Status:

```bash
make ingest ENV=host MODE=status
```

Jobs:

```bash
make ingest ENV=host MODE=jobs
```

Câmara full-load:

```bash
make ingest ENV=host MODE=camara-full ANO_INI=2024 ANO_FIM=2024
```

Senado incremental:

```bash
make ingest ENV=host MODE=senado-incremental DATA_INI=2026-03-01 DATA_FIM=2026-03-05
```

## 6) Observações operacionais

- Em `host`, o script sobe o PostgreSQL no container automaticamente (`docker compose up -d postgres`).
- Em `host`, se a aplicação não estiver ativa, o script pode iniciar o Java local conforme `START_JAVA_APP=true` no `ingest-host.env`.
- Nos modos assíncronos (`*-full`, `*-incremental`, `*-reprocess`), a app local deve permanecer ativa até o job concluir.
- Em `container`, o script não inicia Java local (`START_JAVA_APP=false`), apenas chama os endpoints admin.
- Logs de inicialização local ficam em `data/logs/ingest-host.log`.
- Para enriquecer `urlInteiroTeor` no Senado, habilite `ETL_SENADO_ENRICH_URL_TEXTO=true` (pode aumentar o tempo de carga).

## 7) Troubleshooting rápido

- Erro de autenticação admin: valide `ADMIN_USERNAME` e `ADMIN_PASSWORD` no arquivo de ambiente.
- App não sobe no host: confira `JAVA_HOME`, Maven e porta `8080` livre.
- Falha de conexão com banco: valide `SPRING_DATASOURCE_URL` e se o `postgres` está saudável no compose.
