# Plano de Construção — ETL de Matérias e Proposições Legislativas

**Projeto:** `etl-congresso-java`  
**Data:** 2026-03-04  
**Stack:** Java 21 + Spring Boot 3.x + PostgreSQL 15 + Docker  
**Baseado em:** [`doc/spec/spec-etp-materias-proposicoes.md`](../../doc/spec/spec-etp-materias-proposicoes.md)

---

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Estrutura do Codebase](#2-estrutura-do-codebase)
3. [Dependências Maven (pom.xml)](#3-dependências-maven-pomxml)
4. [Infraestrutura com Docker Compose](#4-infraestrutura-com-docker-compose)
5. [Modelo de Dados — Migrações Flyway](#5-modelo-de-dados--migrações-flyway)
6. [Configuração da Aplicação (application.yml)](#6-configuração-da-aplicação-applicationyml)
7. [Detalhamento das Camadas](#7-detalhamento-das-camadas)
8. [Plano de Execução por Fases](#8-plano-de-execução-por-fases)
9. [Endpoints Admin REST](#9-endpoints-admin-rest)
10. [Observabilidade e Monitoramento](#10-observabilidade-e-monitoramento)
11. [Variáveis de Ambiente](#11-variáveis-de-ambiente)
12. [Checklist de Implementação](#12-checklist-de-implementação)

---

## 1. Visão Geral

### 1.1 Objetivo

Implementar um pipeline ETL capaz de:

- Extrair proposições legislativas (PL, PLP, MPV, PEC, PDL, PR) da **Câmara dos Deputados** via arquivos CSV estáticos e da **API REST do Senado Federal**
- Normalizar, deduplicar e persistir os dados em **PostgreSQL**
- Executar tanto em **carga inicial massiva** quanto em **atualizações incrementais diárias**
- Garantir **idempotência**, **rastreabilidade** e **respeito aos rate limits**

### 1.2 Fontes de Dados

| Casa           | Mecanismo           | URL Base                                             | Limite            |
|----------------|---------------------|------------------------------------------------------|-------------------|
| Câmara         | CSV estático (bulk) | `http://dadosabertos.camara.leg.br/arquivos/`        | Sem rate limit    |
| Câmara         | API REST (incremental) | `https://dadosabertos.camara.leg.br/api/v2/`      | ~100 req/página   |
| Senado         | API REST JSON       | `https://legis.senado.leg.br/dadosabertos/`          | 10 req/s          |

### 1.3 Endpoints Primários Utilizados

**Câmara — CSV Bulk:**
```
http://dadosabertos.camara.leg.br/arquivos/proposicoes/json/proposicoes-{ano}.json.zip
http://dadosabertos.camara.leg.br/arquivos/proposicoes/csv/proposicoes-{ano}.csv
```

**Câmara — API REST (incremental):**
```
GET /api/v2/proposicoes?dataInicio={data}&dataFim={data}&siglaTipo=PL,PLP,MPV,PEC,PDL,PR
GET /api/v2/proposicoes/{id}/tramitacoes
GET /api/v2/proposicoes/{id}/votacoes
```

**Senado — API REST:**
```
GET /dadosabertos/materia/atualizadas?data={data}
GET /dadosabertos/materia/{codigo}
GET /dadosabertos/materia/movimentacoes/{codigo}
GET /dadosabertos/materia/votacoes/{codigo}
GET /dadosabertos/materia/tramitando
GET /dadosabertos/materia/pesquisa/lista
```

---

## 2. Estrutura do Codebase

```
etl-congresso-java/
├── pom.xml
├── docker-compose.yml
├── docker-compose.override.yml          ← dev local (sem build)
├── .env.example
├── Makefile
│
├── src/
│   └── main/
│       ├── java/
│       │   └── br/leg/congresso/etl/
│       │       │
│       │       ├── EtlCongressoApplication.java
│       │       │
│       │       ├── config/
│       │       │   ├── WebClientConfig.java           ← WebClient com timeouts
│       │       │   ├── ExecutorConfig.java            ← ThreadPool ETL
│       │       │   ├── Resilience4jConfig.java        ← RateLimiter + Retry
│       │       │   ├── JpaConfig.java                 ← Batch settings, Hikari
│       │       │   └── SecurityConfig.java            ← Spring Security (admin)
│       │       │
│       │       ├── domain/
│       │       │   ├── Proposicao.java                ← Entidade JPA principal
│       │       │   ├── Tramitacao.java                ← Entidade JPA
│       │       │   ├── EtlJobControl.java             ← Controle de jobs
│       │       │   ├── EtlFileControl.java            ← Controle de arquivos CSV
│       │       │   ├── EtlErrorLog.java               ← Log de erros
│       │       │   ├── EtlLock.java                   ← Lock distribuído
│       │       │   └── enums/
│       │       │       ├── CasaLegislativa.java       ← CAMARA / SENADO
│       │       │       ├── TipoProposicao.java        ← PL, PLP, MPV, PEC, PDL, PR
│       │       │       ├── StatusEtl.java             ← PENDING, RUNNING, SUCCESS, FAILED
│       │       │       └── TipoExecucao.java          ← FULL, INCREMENTAL, FORCED
│       │       │
│       │       ├── repository/
│       │       │   ├── ProposicaoRepository.java
│       │       │   ├── TramitacaoRepository.java
│       │       │   ├── EtlJobControlRepository.java
│       │       │   ├── EtlFileControlRepository.java
│       │       │   ├── EtlErrorLogRepository.java
│       │       │   └── EtlLockRepository.java
│       │       │
│       │       ├── extractor/
│       │       │   ├── ExtractorPort.java             ← Interface comum
│       │       │   │
│       │       │   ├── camara/
│       │       │   │   ├── CamaraCSVExtractor.java    ← Download + parse CSV
│       │       │   │   ├── CamaraApiExtractor.java    ← REST incremental
│       │       │   │   ├── CamaraFileDownloader.java  ← Download WebClient
│       │       │   │   ├── dto/
│       │       │   │   │   ├── CamaraProposicaoCSVRow.java
│       │       │   │   │   ├── CamaraProposicaoDTO.java
│       │       │   │   │   └── CamaraTramitacaoDTO.java
│       │       │   │   └── mapper/
│       │       │   │       └── CamaraProposicaoMapper.java
│       │       │   │
│       │       │   └── senado/
│       │       │       ├── SenadoApiExtractor.java    ← REST paginado
│       │       │       ├── SenadoIncrementalExtractor.java
│       │       │       ├── AdaptiveRateLimiter.java   ← Rate limit dinâmico
│       │       │       ├── dto/
│       │       │       │   ├── SenadoMateriaDTO.java
│       │       │       │   ├── SenadoMovimentacaoDTO.java
│       │       │       │   └── SenadoVotacaoDTO.java
│       │       │       └── mapper/
│       │       │           └── SenadoMateriaMapper.java
│       │       │
│       │       ├── transformer/
│       │       │   ├── ProposicaoTransformer.java     ← Normalização + hash
│       │       │   ├── TipoProposicaoNormalizer.java  ← Mapeamento siglas
│       │       │   ├── ContentHashGenerator.java      ← SHA-256 fingerprint
│       │       │   └── DeduplicationService.java      ← UPSERT logic
│       │       │
│       │       ├── loader/
│       │       │   ├── ProposicaoLoader.java          ← Batch INSERT/UPSERT
│       │       │   ├── TramitacaoLoader.java
│       │       │   └── BatchUpsertHelper.java         ← Native SQL COPY/ON CONFLICT
│       │       │
│       │       ├── orchestrator/
│       │       │   ├── EtlOrchestrator.java           ← Ponto central de coordenação
│       │       │   ├── FullLoadOrchestrator.java      ← Carga completa
│       │       │   └── IncrementalLoadOrchestrator.java
│       │       │
│       │       ├── scheduler/
│       │       │   └── EtlScheduler.java              ← @Scheduled cron
│       │       │
│       │       ├── admin/
│       │       │   ├── AdminEtlController.java        ← REST admin endpoints
│       │       │   └── dto/
│       │       │       ├── EtlStatusResponse.java
│       │       │       └── EtlTriggerRequest.java
│       │       │
│       │       └── metrics/
│       │           └── EtlMetrics.java                ← Micrometer counters/timers
│       │
│       └── resources/
│           ├── application.yml
│           ├── application-local.yml
│           ├── application-prod.yml
│           └── db/migration/
│               ├── V1__create_proposicao.sql
│               ├── V2__create_tramitacao.sql
│               ├── V3__create_etl_job_control.sql
│               ├── V4__create_etl_file_control.sql
│               ├── V5__create_etl_error_log.sql
│               ├── V6__create_etl_lock.sql
│               └── V7__create_indexes.sql
│
└── src/test/
    └── java/br/leg/congresso/etl/
        ├── extractor/
        │   ├── CamaraCSVExtractorTest.java
        │   └── SenadoApiExtractorTest.java
        ├── transformer/
        │   └── ProposicaoTransformerTest.java
        └── orchestrator/
            └── EtlOrchestratorTest.java
```

---

## 3. Dependências Maven (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.3</version>
        <relativePath/>
    </parent>

    <groupId>br.leg.congresso</groupId>
    <artifactId>etl-congresso</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>ETL Congresso Nacional</name>
    <description>Pipeline ETL - Matérias e Proposições Legislativas</description>

    <properties>
        <java.version>21</java.version>
        <resilience4j.version>2.2.0</resilience4j.version>
        <opencsv.version>5.9</opencsv.version>
        <mapstruct.version>1.6.3</mapstruct.version>
    </properties>

    <dependencies>

        <!-- ===== Spring Boot Core ===== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
            <!-- WebClient para downloads e chamadas REST reativas -->
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- ===== Banco de Dados ===== -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- ===== Resiliência ===== -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>${resilience4j.version}</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-reactor</artifactId>
            <version>${resilience4j.version}</version>
        </dependency>

        <!-- ===== Parsing CSV ===== -->
        <dependency>
            <groupId>com.opencsv</groupId>
            <artifactId>opencsv</artifactId>
            <version>${opencsv.version}</version>
        </dependency>

        <!-- ===== Mapeamento de Objetos ===== -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- ===== Métricas ===== -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- ===== Utilitários ===== -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- ===== Testes ===== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>mockwebserver</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <!-- MapStruct + Lombok — ordem de processamento obrigatória -->
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

---

## 4. Infraestrutura com Docker Compose

### 4.1 `docker-compose.yml` — Ambiente Completo

```yaml
version: "3.9"

# ─────────────────────────────────────────────
#  ETL Congresso Nacional — Infraestrutura
# ─────────────────────────────────────────────

services:

  # ──────────────────────────────────
  # Banco de dados principal
  # ──────────────────────────────────
  postgres:
    image: postgres:15-alpine
    container_name: etl-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-etl_congresso}
      POSTGRES_USER: ${POSTGRES_USER:-etl_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-etl_pass}
      PGDATA: /var/lib/postgresql/data/pgdata
    ports:
      - "${POSTGRES_PORT:-5433}:5433"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./infra/postgres/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-etl_user} -d ${POSTGRES_DB:-etl_congresso}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - etl-network

  # ──────────────────────────────────
  # Aplicação ETL (Spring Boot)
  # ──────────────────────────────────
  etl-app:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        JAVA_VERSION: 21
    container_name: etl-congresso-app
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: ${APP_PROFILE:-prod}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5433/${POSTGRES_DB:-etl_congresso}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-etl_user}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-etl_pass}
      ETL_CAMARA_BASE_URL: ${ETL_CAMARA_BASE_URL:-https://dadosabertos.camara.leg.br}
      ETL_SENADO_BASE_URL: ${ETL_SENADO_BASE_URL:-https://legis.senado.leg.br}
      ETL_CAMARA_CSV_BASE_URL: ${ETL_CAMARA_CSV_BASE_URL:-http://dadosabertos.camara.leg.br/arquivos}
      ETL_SCHEDULER_ENABLED: ${ETL_SCHEDULER_ENABLED:-true}
      ETL_SCHEDULER_CRON: ${ETL_SCHEDULER_CRON:-0 0 3 * * ?}
      ADMIN_USERNAME: ${ADMIN_USERNAME:-admin}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:-changeme}
      JAVA_OPTS: >-
        -Xms512m
        -Xmx2g
        -XX:+UseG1GC
        -XX:MaxGCPauseMillis=200
    ports:
      - "${APP_PORT:-8080}:8080"
    volumes:
      - etl_tmp:/tmp/etl
      - etl_logs:/app/logs
    networks:
      - etl-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # ──────────────────────────────────
  # Métricas — Prometheus
  # ──────────────────────────────────
  prometheus:
    image: prom/prometheus:v2.51.0
    container_name: etl-prometheus
    restart: unless-stopped
    depends_on:
      - etl-app
    ports:
      - "${PROMETHEUS_PORT:-9090}:9090"
    volumes:
      - ./infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    command:
      - "--config.file=/etc/prometheus/prometheus.yml"
      - "--storage.tsdb.path=/prometheus"
      - "--storage.tsdb.retention.time=30d"
      - "--web.enable-lifecycle"
    networks:
      - etl-network

  # ──────────────────────────────────
  # Dashboard — Grafana
  # ──────────────────────────────────
  grafana:
    image: grafana/grafana:10.4.2
    container_name: etl-grafana
    restart: unless-stopped
    depends_on:
      - prometheus
    ports:
      - "${GRAFANA_PORT:-3000}:3000"
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_USER:-admin}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
      GF_USERS_ALLOW_SIGN_UP: "false"
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - grafana_data:/var/lib/grafana
      - ./infra/grafana/provisioning:/etc/grafana/provisioning:ro
      - ./infra/grafana/dashboards:/var/lib/grafana/dashboards:ro
    networks:
      - etl-network

  # ──────────────────────────────────
  # Adminer — UI para PostgreSQL
  # ──────────────────────────────────
  adminer:
    image: adminer:4.8.1
    container_name: etl-adminer
    restart: unless-stopped
    depends_on:
      - postgres
    ports:
      - "${ADMINER_PORT:-8081}:8080"
    environment:
      ADMINER_DEFAULT_SERVER: postgres
      ADMINER_DESIGN: "pepa-linha"
    networks:
      - etl-network
    profiles:
      - tools   # Subir apenas com: docker compose --profile tools up

# ──────────────────────────────────
# Volumes
# ──────────────────────────────────
volumes:
  postgres_data:
    name: etl_postgres_data
  etl_tmp:
    name: etl_tmp_files
  etl_logs:
    name: etl_application_logs
  prometheus_data:
    name: etl_prometheus_data
  grafana_data:
    name: etl_grafana_data

# ──────────────────────────────────
# Redes
# ──────────────────────────────────
networks:
  etl-network:
    name: etl-congresso-network
    driver: bridge
```

### 4.2 `docker-compose.override.yml` — Desenvolvimento Local

```yaml
version: "3.9"

# Sobrescreve configurações para ambiente de desenvolvimento local.
# NÃO faz build da aplicação — use maven diretamente.

services:

  postgres:
    ports:
      - "5433:5433"   # Expõe BD para IDE local

  etl-app:
    # Em dev, a aplicação roda via IDE/Maven. Remove o serviço.
    profiles:
      - disabled

  adminer:
    profiles: []   # Sempre sobe adminer em dev (sem --profile)
```

### 4.3 `Dockerfile`

```dockerfile
# ── Estágio 1: Build ──────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn -B -q package -DskipTests

# ── Estágio 2: Runtime ────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuário não-root
RUN addgroup -S etl && adduser -S etl -G etl
USER etl

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 4.4 Arquivo de Configuração do Prometheus

`infra/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: "etl-congresso"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["etl-app:8080"]
    scrape_interval: 10s
```

---

## 5. Modelo de Dados — Migrações Flyway

### V1 — Tabela `proposicao`

```sql
-- V1__create_proposicao.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE proposicao (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    casa              VARCHAR(20)  NOT NULL,            -- CAMARA | SENADO
    tipo              VARCHAR(50)  NOT NULL,            -- PL, PLP, MPV, PEC, PDL, PR
    sigla             VARCHAR(20),
    numero            INTEGER,
    ano               INTEGER,
    ementa            TEXT,
    situacao          VARCHAR(500),
    data_apresentacao DATE,
    data_atualizacao  TIMESTAMP,
    status_final      VARCHAR(100),
    virou_lei         BOOLEAN      NOT NULL DEFAULT FALSE,
    id_origem         VARCHAR(50),                      -- ID original na fonte (Câmara ou Senado)
    uri_origem        VARCHAR(500),                     -- URL de referência na API
    content_hash      VARCHAR(64),                      -- SHA-256 dos campos relevantes
    criado_em         TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMP,

    CONSTRAINT uq_proposicao_natural UNIQUE (casa, sigla, numero, ano)
);

COMMENT ON TABLE  proposicao                IS 'Proposições e matérias legislativas normalizadas';
COMMENT ON COLUMN proposicao.casa           IS 'Casa de origem: CAMARA ou SENADO';
COMMENT ON COLUMN proposicao.tipo           IS 'Tipo normalizado: LEI_ORDINARIA, LEI_COMPLEMENTAR, etc.';
COMMENT ON COLUMN proposicao.content_hash   IS 'Hash SHA-256 para detecção de mudanças (idempotência)';
COMMENT ON COLUMN proposicao.id_origem      IS 'Identificador na API de origem (id Câmara ou código Senado)';
```

### V2 — Tabela `tramitacao`

```sql
-- V2__create_tramitacao.sql
CREATE TABLE tramitacao (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    proposicao_id  UUID         NOT NULL REFERENCES proposicao(id) ON DELETE CASCADE,
    sequencia      INTEGER,
    data_evento    DATE,
    orgao          VARCHAR(255),
    descricao      TEXT,
    situacao       VARCHAR(500),
    regime         VARCHAR(100),
    criado_em      TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE tramitacao IS 'Histórico de tramitação de proposições';
```

### V3 — Tabela `etl_job_control`

```sql
-- V3__create_etl_job_control.sql
CREATE TABLE etl_job_control (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    origem           VARCHAR(20)  NOT NULL,    -- CAMARA | SENADO
    tipo_execucao    VARCHAR(20)  NOT NULL,    -- FULL | INCREMENTAL | FORCED
    iniciado_em      TIMESTAMP    NOT NULL DEFAULT NOW(),
    finalizado_em    TIMESTAMP,
    total_processado INTEGER      NOT NULL DEFAULT 0,
    total_inserido   INTEGER      NOT NULL DEFAULT 0,
    total_atualizado INTEGER      NOT NULL DEFAULT 0,
    total_ignorados  INTEGER      NOT NULL DEFAULT 0,
    total_erros      INTEGER      NOT NULL DEFAULT 0,
    status           VARCHAR(50)  NOT NULL DEFAULT 'RUNNING',  -- RUNNING | SUCCESS | FAILED | PARTIAL
    mensagem_erro    TEXT,
    parametros       JSONB                                      -- parâmetros de execução (datas, anos, etc.)
);

COMMENT ON TABLE etl_job_control IS 'Auditoria e rastreabilidade de execuções do ETL';
```

### V4 — Tabela `etl_file_control`

```sql
-- V4__create_etl_file_control.sql
CREATE TABLE etl_file_control (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    origem                  VARCHAR(20)  NOT NULL DEFAULT 'CAMARA',
    nome_arquivo            VARCHAR(255) NOT NULL,
    url_download            VARCHAR(500),
    checksum_sha256         VARCHAR(64),
    tamanho_bytes           BIGINT,
    ano_referencia          INTEGER,
    data_referencia         DATE,
    processado_em           TIMESTAMP,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    forcar_reprocessamento  BOOLEAN      NOT NULL DEFAULT FALSE,
    job_id                  UUID         REFERENCES etl_job_control(id),
    criado_em               TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_file_control UNIQUE (origem, nome_arquivo)
);

COMMENT ON TABLE etl_file_control IS 'Controle de ingestão de arquivos CSV — evita reprocessamento desnecessário';
COMMENT ON COLUMN etl_file_control.checksum_sha256 IS 'SHA-256 do conteúdo para detectar mudanças no arquivo';
```

### V5 — Tabela `etl_error_log`

```sql
-- V5__create_etl_error_log.sql
CREATE TABLE etl_error_log (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id       UUID         REFERENCES etl_job_control(id),
    origem       VARCHAR(20)  NOT NULL,
    tipo_erro    VARCHAR(100),
    endpoint     VARCHAR(500),
    payload      TEXT,
    mensagem     TEXT,
    stack_trace  TEXT,
    tentativas   INTEGER      NOT NULL DEFAULT 1,
    criado_em    TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE etl_error_log IS 'Registro persistente de erros durante execuções ETL';
```

### V6 — Tabela `etl_lock`

```sql
-- V6__create_etl_lock.sql
CREATE TABLE etl_lock (
    recurso     VARCHAR(100) PRIMARY KEY,     -- ex: 'camara_2024', 'senado_full'
    locked_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    locked_by   VARCHAR(100),                 -- hostname/instância para diagnóstico
    expires_at  TIMESTAMP    NOT NULL
);

COMMENT ON TABLE etl_lock IS 'Lock distribuído para evitar execução paralela do mesmo recurso';
```

### V7 — Índices

```sql
-- V7__create_indexes.sql
-- Índices funcionais para consultas ETL e analíticas

CREATE INDEX idx_proposicao_tipo         ON proposicao(tipo);
CREATE INDEX idx_proposicao_ano          ON proposicao(ano);
CREATE INDEX idx_proposicao_casa         ON proposicao(casa);
CREATE INDEX idx_proposicao_status_final ON proposicao(status_final);
CREATE INDEX idx_proposicao_data_update  ON proposicao(data_atualizacao);
CREATE INDEX idx_proposicao_virou_lei    ON proposicao(virou_lei) WHERE virou_lei = TRUE;
CREATE INDEX idx_proposicao_id_origem    ON proposicao(id_origem, casa);

CREATE INDEX idx_tramitacao_proposicao   ON tramitacao(proposicao_id);
CREATE INDEX idx_tramitacao_data         ON tramitacao(data_evento);

CREATE INDEX idx_job_control_origem      ON etl_job_control(origem, tipo_execucao);
CREATE INDEX idx_job_control_status      ON etl_job_control(status);
CREATE INDEX idx_job_control_iniciado    ON etl_job_control(iniciado_em DESC);

CREATE INDEX idx_file_control_status     ON etl_file_control(status, forcar_reprocessamento);
CREATE INDEX idx_file_control_ano        ON etl_file_control(ano_referencia);
```

---

## 6. Configuração da Aplicação (application.yml)

```yaml
# src/main/resources/application.yml

spring:
  application:
    name: etl-congresso

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/etl_congresso}
    username: ${SPRING_DATASOURCE_USERNAME:etl_user}
    password: ${SPRING_DATASOURCE_PASSWORD:etl_pass}
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      auto-commit: false

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 500
          order_inserts: true
          order_updates: true
        order_inserts: true
        order_updates: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true

  task:
    scheduling:
      enabled: ${ETL_SCHEDULER_ENABLED:true}

# ─────────────────────────────────────────────
# ETL Configurações Específicas
# ─────────────────────────────────────────────
etl:
  camara:
    base-url: ${ETL_CAMARA_BASE_URL:https://dadosabertos.camara.leg.br}
    api-path: /api/v2
    csv-base-url: ${ETL_CAMARA_CSV_BASE_URL:http://dadosabertos.camara.leg.br/arquivos}
    csv-tmp-dir: ${ETL_CSV_TMP_DIR:/tmp/etl/camara}
    tipos-proposicao: PL,PLP,MPV,PEC,PDL,PR
    ano-inicio-historico: 2001
    timeout-seconds: 120
    chunk-size: 10000       # linhas por chunk no CSV

  senado:
    base-url: ${ETL_SENADO_BASE_URL:https://legis.senado.leg.br}
    api-path: /dadosabertos
    timeout-seconds: 60
    ano-inicio-historico: 1988
    page-size: 100

  incremental:
    janela-dias: 2          # Buscar registros atualizados nos últimos N dias

  executor:
    core-pool-size: 8
    max-pool-size: 16
    queue-capacity: 200

  scheduler:
    cron: ${ETL_SCHEDULER_CRON:"0 0 3 * * ?"}   # 03:00 diariamente
    timezone: America/Sao_Paulo

# ─────────────────────────────────────────────
# Resilience4j
# ─────────────────────────────────────────────
resilience4j:
  ratelimiter:
    instances:
      senadoApi:
        limit-for-period: 8
        limit-refresh-period: 1s
        timeout-duration: 500ms
      camaraApi:
        limit-for-period: 20
        limit-refresh-period: 1s
        timeout-duration: 500ms

  retry:
    instances:
      senadoApi:
        max-attempts: 3
        wait-duration: 2s
        retry-exceptions:
          - java.io.IOException
          - org.springframework.web.reactive.function.client.WebClientRequestException
      camaraApi:
        max-attempts: 3
        wait-duration: 1s

  circuitbreaker:
    instances:
      senadoApi:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        permitted-number-of-calls-in-half-open-state: 5
        sliding-window-size: 20

# ─────────────────────────────────────────────
# Segurança
# ─────────────────────────────────────────────
admin:
  username: ${ADMIN_USERNAME:admin}
  password: ${ADMIN_PASSWORD:changeme}

# ─────────────────────────────────────────────
# Actuator / Métricas
# ─────────────────────────────────────────────
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: etl-congresso

# ─────────────────────────────────────────────
# Logging
# ─────────────────────────────────────────────
logging:
  level:
    br.leg.congresso.etl: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /app/logs/etl-congresso.log
```

---

## 7. Detalhamento das Camadas

### 7.1 `domain/` — Entidades JPA

| Classe              | Tabela             | Descrição                                      |
|---------------------|--------------------|------------------------------------------------|
| `Proposicao`        | `proposicao`       | Entidade central. Chave natural: casa+sigla+numero+ano |
| `Tramitacao`        | `tramitacao`       | Linha de tramitação de uma proposição          |
| `EtlJobControl`     | `etl_job_control`  | Registro de execução do job                   |
| `EtlFileControl`    | `etl_file_control` | Controle de arquivos CSV ingeridos             |
| `EtlErrorLog`       | `etl_error_log`    | Log persistente de erros                       |
| `EtlLock`           | `etl_lock`         | Lock distribuído por recurso/ano               |

### 7.2 `extractor/camara/` — Extração Câmara

| Classe                    | Responsabilidade                                                          |
|---------------------------|---------------------------------------------------------------------------|
| `CamaraFileDownloader`    | Download de CSV usando `WebClient`; salva em `/tmp/etl/camara/`          |
| `CamaraCSVExtractor`      | Stream parsing do CSV com OpenCSV em chunks; emite `CamaraProposicaoCSVRow` |
| `CamaraApiExtractor`      | Chamadas REST incrementais a `/api/v2/proposicoes` com filtro de data    |

**Fluxo de extração CSV Câmara:**
```
download(url) → salvar /tmp → calcular SHA-256
    → verificar etl_file_control
        → SE mesmo hash: ignorar
        → SE hash diferente ou novo: processar
            → parse stream CSV → emitir chunks
```

### 7.3 `extractor/senado/` — Extração Senado

| Classe                      | Responsabilidade                                                        |
|-----------------------------|-------------------------------------------------------------------------|
| `SenadoApiExtractor`        | Carga inicial paginando por ano via `/materia/pesquisa/lista`           |
| `SenadoIncrementalExtractor`| Busca matérias em `/materia/atualizadas?data={data}`                   |
| `AdaptiveRateLimiter`       | Ajuste dinâmico da taxa: reduz em 429, aumenta em 200                   |

**Controle de Rate Limit Senado:**

| Código HTTP | Ação                             |
|-------------|----------------------------------|
| 200         | `currentLimit = min(limit+1, 10)`|
| 429         | `currentLimit = max(limit-2, 2)` |
| 503 / 5xx   | `currentLimit = max(limit-3, 2)` — ativa retry |

### 7.4 `transformer/` — Normalização

| Classe                    | Responsabilidade                                                     |
|---------------------------|----------------------------------------------------------------------|
| `TipoProposicaoNormalizer`| Mapeia siglas (`PL`, `PEC`, etc.) para enum `TipoProposicao`        |
| `ContentHashGenerator`    | SHA-256 dos campos: casa+tipo+numero+ano+ementa+situacao+data        |
| `ProposicaoTransformer`   | Converte DTO → entidade JPA; aplica normalização e hash             |
| `DeduplicationService`    | Compara hash com DB; decide INSERT / UPDATE / SKIP                  |

**Regra de deduplicação:**

| Situação                        | Ação   |
|---------------------------------|--------|
| Chave natural não existe        | INSERT |
| Existe com mesmo `content_hash` | SKIP   |
| Existe com hash diferente       | UPDATE |

### 7.5 `loader/` — Persistência

- `ProposicaoLoader`: usa `saveAll()` com `@Transactional`; batch de 500 registros
- `BatchUpsertHelper`: executa SQL nativo com `INSERT ... ON CONFLICT (casa, sigla, numero, ano) DO UPDATE SET ...`
- Para cargas massivas (> 50.000 registros): utiliza `COPY` via `CopyManager` do driver JDBC

### 7.6 `orchestrator/` — Coordenação

```
EtlOrchestrator
    ├── fullLoad(origem, anoInicio, anoFim)
    │       → cria EtlJobControl (FULL)
    │       → adquire EtlLock por ano
    │       → chama extractor → transformer → loader
    │       → libera lock
    │       → atualiza EtlJobControl
    │
    └── incrementalLoad(origem, janelaDias)
            → cria EtlJobControl (INCREMENTAL)
            → extrator incremental (últimos N dias)
            → transformer → loader (upsert)
            → atualiza EtlJobControl
```

### 7.7 `scheduler/`

```java
@Scheduled(cron = "${etl.scheduler.cron}", zone = "${etl.scheduler.timezone}")
public void runIncrementalLoad() {
    etlOrchestrator.incrementalLoad(CasaLegislativa.CAMARA, 2);
    etlOrchestrator.incrementalLoad(CasaLegislativa.SENADO, 2);
}
```

---

## 8. Plano de Execução por Fases

### Fase 1 — Fundação do Projeto

**Objetivo:** Projeto compilável, banco dockerizado, Flyway rodando.

- [ ] Criar `pom.xml` com todas as dependências
- [ ] Criar estrutura de pacotes Java
- [ ] Criar `docker-compose.yml` com PostgreSQL + Adminer
- [ ] Criar `Dockerfile` multi-stage
- [ ] Criar `.env.example`
- [ ] Criar migrações Flyway V1 → V7
- [ ] Criar entidades JPA (`Proposicao`, `Tramitacao`, `EtlJobControl`, etc.)
- [ ] Criar `application.yml` base
- [ ] Validar: `docker compose up postgres` + `mvn flyway:migrate`

**Entregável:** Projeto sobe sem erros, tabelas criadas no banco.

---

### Fase 2 — Loader CSV Câmara

**Objetivo:** Fazer download e parse dos CSVs de proposições da Câmara.

- [ ] Implementar `WebClientConfig` com timeout e retry
- [ ] Implementar `CamaraFileDownloader` — download para `/tmp/etl/`
- [ ] Implementar `ContentHashGenerator` — SHA-256 do arquivo
- [ ] Implementar `EtlFileControlRepository` e lógica de controle
- [ ] Implementar `CamaraCSVExtractor` — stream parsing com OpenCSV
  - Mapear campos do CSV para `CamaraProposicaoCSVRow`
  - Filtrar apenas PL, PLP, MPV, PEC, PDL, PR
- [ ] Implementar `CamaraPro posicaoMapper` (MapStruct) → `Proposicao`
- [ ] Implementar `ProposicaoLoader` com batch upsert
- [ ] Implementar `EtlJobControl` — registrar início/fim + contadores
- [ ] **Teste de integração:** baixar CSV de 1 ano, persistir, verificar BD

**Entregável:** Endpoint `/admin/etl/camara/full-load?anoInicio=2024&anoFim=2024` funcional.

---

### Fase 3 — Loader REST Senado

**Objetivo:** Extração de matérias do Senado via API REST.

- [ ] Implementar `AdaptiveRateLimiter` com ajuste por código HTTP
- [ ] Configurar `Resilience4jConfig` — RateLimiter + Retry + CircuitBreaker para Senado
- [ ] Implementar `SenadoApiExtractor` — carga por ano + paginação
  - Endpoint: `/dadosabertos/materia/pesquisa/lista`
  - Paginação controlada por parâmetros da API
- [ ] Implementar `SenadoMateriaMapper` → `Proposicao`
- [ ] Implementar lógica de lock em `EtlLock` para evitar paralelismo indevido
- [ ] **Teste de integração:** carregar 1 ano do Senado, verificar rate limit

**Entregável:** Endpoint `/admin/etl/senado/full-load?anoInicio=2024&anoFim=2024` funcional.

---

### Fase 4 — Transformações e Normalização

**Objetivo:** Dados normalizados, sem duplicatas, com rastreabilidade.

- [ ] Implementar `TipoProposicaoNormalizer` — mapeamento completo de siglas
- [ ] Implementar `DeduplicationService` — hash comparison + upsert
- [ ] Implementar `ContentHashGenerator` para registros individuais
- [ ] Adicionar campo `content_hash` na entidade e migração
- [ ] **Teste unitário:** transformador com todos os tipos de proposição
- [ ] **Teste unitário:** deduplicação — insert/update/skip

**Entregável:** Dados normalizados; reprocessamento do mesmo CSV não duplica dados.

---

### Fase 5 — Carga Incremental

**Objetivo:** Atualização diária automática.

- [ ] Implementar `SenadoIncrementalExtractor` — `/materia/atualizadas?data={data}`
- [ ] Implementar `CamaraApiExtractor` — `/api/v2/proposicoes?dataInicio=...`
- [ ] Implementar `IncrementalLoadOrchestrator`
- [ ] Implementar `EtlScheduler` com `@Scheduled`
- [ ] Implementar endpoint de reprocessamento forçado:
  - `POST /admin/etl/camara/reprocess?ano=2024`
  - `POST /admin/etl/senado/reprocess?data=2024-01-01`
- [ ] **Teste:** simular janela de 2 dias, verificar que apenas novos são inseridos

**Entregável:** Scheduler rodando às 03:00, carga incremental funcional.

---

### Fase 6 — APIs Admin e Observabilidade

**Objetivo:** Visibilidade operacional e controle manual.

- [ ] Implementar `AdminEtlController` com endpoints (ver seção 9)
- [ ] Implementar `EtlMetrics` com Micrometer (ver seção 10)
- [ ] Configurar Prometheus + Grafana no `docker-compose.yml`
- [ ] Criar dashboard Grafana básico:
  - Total de proposições por casa
  - Registros processados por hora
  - Taxa de erros
  - Tempo de execução do job
- [ ] Configurar Spring Security para endpoints `/admin/**`

**Entregável:** Dashboard Grafana com métricas em tempo real.

---

### Fase 7 — Otimizações de Performance

**Objetivo:** Suportar cargas completas (1988–2026) com eficiência.

- [ ] Implementar `BatchUpsertHelper` com `INSERT ... ON CONFLICT ... DO UPDATE`
- [ ] Avaliar uso de `COPY` para cargas > 50k registros
- [ ] Implementar paralelização por ano:
  ```java
  years.parallelStream()
       .forEach(year -> processYearWithLock(year, executor));
  ```
- [ ] Configurar `ThreadPoolTaskExecutor` com 8–16 workers
- [ ] Tuning do HikariCP: `maximumPoolSize` = 20
- [ ] **Teste de carga:** processar todos os anos da Câmara (> 500k registros)

**Entregável:** Carga inicial completa em < 2 horas.

---

## 9. Endpoints Admin REST

Base path: `/admin/etl`  
Autenticação: Basic Auth (usuário admin)

| Método | Endpoint                                  | Descrição                                          |
|--------|-------------------------------------------|----------------------------------------------------|
| `POST` | `/admin/etl/camara/full-load`             | Inicia carga completa Câmara (param: anoInicio, anoFim) |
| `POST` | `/admin/etl/senado/full-load`             | Inicia carga completa Senado (param: anoInicio, anoFim) |
| `POST` | `/admin/etl/camara/incremental`           | Força execução incremental Câmara                  |
| `POST` | `/admin/etl/senado/incremental`           | Força execução incremental Senado                  |
| `POST` | `/admin/etl/camara/reprocess`             | Reprocessa arquivo CSV (param: ano)                |
| `POST` | `/admin/etl/senado/reprocess`             | Reprocessa período Senado (param: data)            |
| `GET`  | `/admin/etl/jobs`                         | Lista execuções recentes (últimas 50)              |
| `GET`  | `/admin/etl/jobs/{id}`                    | Detalhe de uma execução                            |
| `GET`  | `/admin/etl/jobs/{id}/errors`             | Erros de uma execução                              |
| `GET`  | `/admin/etl/status`                       | Status atual (running jobs, locks ativos)           |
| `GET`  | `/actuator/health`                        | Saúde da aplicação (Spring Actuator)               |
| `GET`  | `/actuator/prometheus`                    | Métricas para Prometheus                           |

---

## 10. Observabilidade e Monitoramento

### 10.1 Métricas Micrometer (Prometheus)

| Métrica                              | Tipo    | Labels                      |
|--------------------------------------|---------|-----------------------------|
| `etl_job_total`                      | Counter | `origem`, `tipo`, `status`  |
| `etl_registros_processados_total`    | Counter | `origem`, `operacao`        |
| `etl_registros_inseridos_total`      | Counter | `origem`                    |
| `etl_registros_atualizados_total`    | Counter | `origem`                    |
| `etl_registros_ignorados_total`      | Counter | `origem`                    |
| `etl_erros_total`                    | Counter | `origem`, `tipo_erro`       |
| `etl_job_duracao_segundos`           | Timer   | `origem`, `tipo`            |
| `etl_taxa_req_por_segundo`           | Gauge   | `origem`                    |
| `etl_threads_ativas`                 | Gauge   | —                           |
| `etl_reprocessamentos_forcados_total`| Counter | `origem`                    |

### 10.2 Logs Estruturados

```
[ETL-JOB-START]  origem=CAMARA tipo=FULL jobId=uuid ano=2024
[ETL-DOWNLOAD]   arquivo=proposicoes-2024.csv tamanho=12345678 checksum=abc123
[ETL-PROGRESS]   processado=10000 inserido=9800 atualizado=150 ignorado=50
[ETL-JOB-END]    jobId=uuid duracao=125s total=10000 status=SUCCESS
[ETL-RATE-LIMIT] origem=SENADO httpCode=429 currentLimit=6 -> newLimit=4
[ETL-CIRCUIT]    origem=SENADO estado=OPEN motivo=falhas consecutivas
```

### 10.3 Alertas Recomendados (Grafana)

| Condição                                   | Severidade |
|--------------------------------------------|------------|
| Job ETL sem execução por > 26h             | WARNING    |
| Taxa de erros > 5% em 1 execução           | WARNING    |
| Circuit Breaker Senado em estado OPEN      | CRITICAL   |
| PostgreSQL com < 2 conexões disponíveis    | CRITICAL   |
| Execução de full-load > 4h                 | WARNING    |

---

## 11. Variáveis de Ambiente

Criar arquivo `.env` na raiz do projeto (baseado em `.env.example`):

```bash
# ─────────────────────────────────────────────
# .env.example — Copie para .env e ajuste os valores
# ─────────────────────────────────────────────

# Banco de Dados
POSTGRES_DB=etl_congresso
POSTGRES_USER=etl_user
POSTGRES_PASSWORD=etl_pass_segura_aqui
POSTGRES_PORT=5433

# Aplicação
APP_PORT=8080
APP_PROFILE=prod

# ETL — URLs das APIs (usar padrões se não houver proxy)
ETL_CAMARA_BASE_URL=https://dadosabertos.camara.leg.br
ETL_CAMARA_CSV_BASE_URL=http://dadosabertos.camara.leg.br/arquivos
ETL_SENADO_BASE_URL=https://legis.senado.leg.br
ETL_CSV_TMP_DIR=/tmp/etl/camara

# ETL — Scheduler
ETL_SCHEDULER_ENABLED=true
ETL_SCHEDULER_CRON=0 0 3 * * ?

# Segurança
ADMIN_USERNAME=admin
ADMIN_PASSWORD=senha_forte_aqui

# Monitoramento
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
GRAFANA_USER=admin
GRAFANA_PASSWORD=grafana_pass

# Adminer (opcional, profile=tools)
ADMINER_PORT=8081
```

---

## 12. Checklist de Implementação

### Pré-requisitos do Ambiente

- [ ] Java 21 instalado (`java -version`)
- [ ] Maven 3.9+ instalado (`mvn -version`)
- [ ] Docker + Docker Compose instalados
- [ ] Acesso às URLs das APIs (sem VPN bloqueando)

### Comandos de Inicialização

```bash
# 1. Subir infraestrutura
docker compose up -d postgres prometheus grafana

# 2. Rodar migrações (na primeira vez)
mvn flyway:migrate

# 3. Compilar e startar aplicação (dev)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. Subir tudo via Docker (prod)
docker compose up -d --build

# 5. Subir com ferramentas (Adminer)
docker compose --profile tools up -d

# 6. Verificar saúde
curl http://localhost:8080/actuator/health

# 7. Disparar carga inicial Câmara (2024)
curl -X POST -u admin:changeme \
  "http://localhost:8080/admin/etl/camara/full-load?anoInicio=2024&anoFim=2024"

# 8. Disparar carga inicial Senado (2024)
curl -X POST -u admin:changeme \
  "http://localhost:8080/admin/etl/senado/full-load?anoInicio=2024&anoFim=2024"
```

### Ordem de URLs de Verificação

| Serviço     | URL                                      |
|-------------|------------------------------------------|
| API Health  | http://localhost:8080/actuator/health    |
| Métricas    | http://localhost:8080/actuator/prometheus|
| Grafana     | http://localhost:3000                    |
| Prometheus  | http://localhost:9090                    |
| Adminer     | http://localhost:8081                    |

---

## Referências

- [Spec ETL](../../doc/spec/spec-etp-materias-proposicoes.md)
- [Análise API Câmara](../../doc/api/analise-tecnica-api-camara.md)
- [Análise API Senado](../../doc/api/analise-tecnica-api-senado.md)
- [OpenAPI Câmara](../../doc/api/open-api-camara-dos-deputados.json)
- [OpenAPI Senado](../../doc/api/senado-openapi.json)
- [Fontes CSV Câmara](../../doc/api/cd-fonte-arquivos.txt)
