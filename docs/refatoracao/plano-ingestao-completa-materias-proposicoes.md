# Plano de Refatoração — Ingestão Completa de Matérias e Proposições (Silver)

> **Data:** 2026-03-08  
> **Status:** Proposta — pendente aprovação  
> **Pré-requisitos:** Fases 1 e 2 do plano de ingestão de OutrasInformacoes concluídas

---

## Sumário

1. [Objetivo](#1-objetivo)
2. [Contexto e Estado Atual](#2-contexto-e-estado-atual)
3. [Inventário de Endpoints — Câmara dos Deputados](#3-inventário-de-endpoints--câmara-dos-deputados)
4. [Inventário de Endpoints — Senado Federal](#4-inventário-de-endpoints--senado-federal)
5. [Análise de Fontes de Dados CSV (Câmara)](#5-análise-de-fontes-de-dados-csv-câmara)
6. [Modelo de Dados Silver — Novas Tabelas](#6-modelo-de-dados-silver--novas-tabelas)
7. [Integração com Processos Existentes](#7-integração-com-processos-existentes)
8. [Estratégia de Migração Senado: materia → processo](#8-estratégia-de-migração-senado-materia--processo)
9. [Faseamento da Implementação](#9-faseamento-da-implementação)
10. [Migrações Flyway](#10-migrações-flyway)
11. [Componentes Java — Novos Artefatos](#11-componentes-java--novos-artefatos)
12. [Estratégia de Enriquecimento](#12-estratégia-de-enriquecimento)
13. [Configuração e Rate Limiting](#13-configuração-e-rate-limiting)
14. [Riscos e Mitigações](#14-riscos-e-mitigações)
15. [Critérios de Aceitação](#15-critérios-de-aceitação)

---

## 1. Objetivo

Expandir a camada Silver para cobrir **todos os endpoints que se relacionam com matérias (Senado) e proposições (Câmara)**, criando uma tabela dedicada para cada endpoint/fonte de dados, com colunas fiéis à estrutura original da fonte (CSV ou JSON da API).

### 1.1. Princípios Norteadores

| Princípio                               | Descrição                                                                                             |
| --------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| **Uma tabela por endpoint/fonte**       | Cada endpoint ou arquivo CSV gera sua própria tabela Silver, espelhando a estrutura de dados original |
| **Fidelidade à fonte**                  | Colunas da tabela Silver reproduzem exatamente os campos do endpoint/CSV, sem transformação           |
| **CSV como fonte prioritária (Câmara)** | Sempre que existir arquivo CSV publicado pela Câmara, ele é a fonte primária de ingestão em lote      |
| **Complementaridade**                   | Novos processos se integram aos existentes via `SilverEnrichmentService` e `EtlJobControl`            |
| **Idempotência**                        | Todas as operações são seguras para re-execução                                                       |
| **Rate Limit respeitado**               | Câmara: 20 req/s / Senado: 8 req/s (com Resilience4j)                                                 |

---

## 2. Contexto e Estado Atual

### 2.1. Tabelas Silver Existentes

| Tabela                       | Fonte                                                 | Tipo                           | Status         |
| ---------------------------- | ----------------------------------------------------- | ------------------------------ | -------------- |
| `silver.camara_proposicao`   | CSV `proposicoes-{ano}.csv` + API `/proposicoes/{id}` | Carga em lote + enriquecimento | ✅ Operacional |
| `silver.camara_tramitacao`   | API `/proposicoes/{id}/tramitacoes`                   | Enriquecimento                 | ✅ Operacional |
| `silver.senado_materia`      | API `/materia/pesquisa/lista` + `/materia/{codigo}`   | Carga em lote + enriquecimento | ✅ Operacional |
| `silver.senado_movimentacao` | API `/materia/{codigo}/movimentacoes`                 | Enriquecimento                 | ✅ Operacional |
| `silver.senado_autoria`      | API `/materia/autoria/{codigo}`                       | Enriquecimento                 | ✅ Operacional |
| `silver.senado_relatoria`    | API `/processo/relatoria?codigoMateria={codigo}`      | Enriquecimento                 | ✅ Operacional |

### 2.2. Gaps Identificados

**Câmara dos Deputados — 5 fontes faltantes:**

- Classificação temática (CSV `proposicoesTemas-{ano}.csv` + API `/proposicoes/{id}/temas`)
- Autores de proposições (CSV `proposicoesAutores-{ano}.csv` + API `/proposicoes/{id}/autores`)
- Proposições relacionadas (API `/proposicoes/{id}/relacionadas`)
- Votações de proposições (CSV `votacoes-{ano}.csv` + API `/proposicoes/{id}/votacoes`)
- Orientações de bancada em votações (CSV `votacoesOrientacoes-{ano}.csv`)
- Votos individuais (CSV `votacoesVotos-{ano}.csv`)

**Senado Federal — 6 fontes faltantes (com atenção à depreciação):**

- Emendas de matéria (API legada `/materia/emendas/{codigo}` → novo `/processo/emenda`)
- Votações de matéria (API legada `/materia/votacoes/{codigo}` → novo `/votacao`)
- Textos/Documentos de matéria (API legada `/materia/textos/{codigo}` → novo `/processo/documento`)
- Situação atual (API legada `/materia/situacaoatual/{codigo}` → novo `/processo/{idProcesso}`)
- Ordem do dia (API `/materia/ordia/{codigo}`)
- Prazos (API `/processo/prazo`)

---

## 3. Inventário de Endpoints — Câmara dos Deputados

### 3.1. Endpoints de Proposições (escopo deste plano)

| #   | Endpoint                             | Fonte CSV disponível           | Status Atual                                                      | Ação            |
| --- | ------------------------------------ | ------------------------------ | ----------------------------------------------------------------- | --------------- |
| 1   | `GET /proposicoes`                   | `proposicoes-{ano}.csv`        | ✅ Ingerido via CSV → `silver.camara_proposicao`                  | Manter          |
| 2   | `GET /proposicoes/{id}`              | —                              | ✅ Enriquecido em `silver.camara_proposicao` (colunas `status_*`) | Manter          |
| 3   | `GET /proposicoes/{id}/autores`      | `proposicoesAutores-{ano}.csv` | ❌ Não ingerido                                                   | **NOVA tabela** |
| 4   | `GET /proposicoes/{id}/relacionadas` | —                              | ❌ Não ingerido                                                   | **NOVA tabela** |
| 5   | `GET /proposicoes/{id}/temas`        | `proposicoesTemas-{ano}.csv`   | ❌ Não ingerido                                                   | **NOVA tabela** |
| 6   | `GET /proposicoes/{id}/tramitacoes`  | —                              | ✅ Ingerido → `silver.camara_tramitacao`                          | Manter          |
| 7   | `GET /proposicoes/{id}/votacoes`     | `votacoes-{ano}.csv`           | ❌ Não ingerido                                                   | **NOVA tabela** |

### 3.2. Endpoints de Votações (relacionados a proposições)

| #   | Endpoint                         | Fonte CSV disponível            | Status Atual    | Ação                                 |
| --- | -------------------------------- | ------------------------------- | --------------- | ------------------------------------ |
| 8   | `GET /votacoes`                  | `votacoes-{ano}.csv`            | ❌ Não ingerido | **NOVA tabela** (via CSV)            |
| 9   | `GET /votacoes/{id}`             | —                               | ❌ Não ingerido | Enriquecimento da tabela de votações |
| 10  | `GET /votacoes/{id}/orientacoes` | `votacoesOrientacoes-{ano}.csv` | ❌ Não ingerido | **NOVA tabela** (via CSV)            |
| 11  | `GET /votacoes/{id}/votos`       | `votacoesVotos-{ano}.csv`       | ❌ Não ingerido | **NOVA tabela** (via CSV)            |

---

## 4. Inventário de Endpoints — Senado Federal

### 4.1. ⚠️ Alerta Crítico: Depreciação da API `/materia`

A maioria dos endpoints `/dadosabertos/materia/*` está marcada como **DEPRECATED** no OpenAPI do Senado, com data de desativação **01/02/2026 já ultrapassada**. Os endpoints substitutos estão na nova API `/dadosabertos/processo/*`.

| Endpoint Legado                   | Endpoint Substituto                     | Status                              |
| --------------------------------- | --------------------------------------- | ----------------------------------- |
| `/materia/pesquisa/lista`         | `/processo`                             | DEPRECATED — **usar na migração**   |
| `/materia/{codigo}`               | `/processo/{idProcesso}`                | DEPRECATED — **priorizar migração** |
| `/materia/autoria/{codigo}`       | Info dentro de `/processo/{idProcesso}` | DEPRECATED                          |
| `/materia/movimentacoes/{codigo}` | Info dentro de `/processo/{idProcesso}` | DEPRECATED                          |
| `/materia/relatorias/{codigo}`    | `/processo/relatoria`                   | DEPRECATED — **já migrado**         |
| `/materia/emendas/{codigo}`       | `/processo/emenda`                      | DEPRECATED                          |
| `/materia/votacoes/{codigo}`      | `/votacao`                              | DEPRECATED                          |
| `/materia/textos/{codigo}`        | `/processo/documento`                   | DEPRECATED                          |
| `/materia/situacaoatual/{codigo}` | `/processo/{idProcesso}`                | DEPRECATED                          |

### 4.2. Endpoints no Escopo (novos)

| #   | Endpoint (novo)                 | Equivalente legado                | Status Atual                        | Ação                                      |
| --- | ------------------------------- | --------------------------------- | ----------------------------------- | ----------------------------------------- |
| 1   | `GET /processo/emenda`          | `/materia/emendas/{codigo}`       | ❌ Não ingerido                     | **NOVA tabela**                           |
| 2   | `GET /votacao`                  | `/materia/votacoes/{codigo}`      | ❌ Não ingerido                     | **NOVA tabela**                           |
| 3   | `GET /processo/documento`       | `/materia/textos/{codigo}`        | ❌ Não ingerido                     | **NOVA tabela**                           |
| 4   | `GET /processo/{id}` (situação) | `/materia/situacaoatual/{codigo}` | ❌ Parcial (enriquecimento det\_\*) | **Avaliar extração de situação**          |
| 5   | `GET /processo/prazo`           | `/materia/tiposPrazo`             | ❌ Não ingerido                     | **NOVA tabela**                           |
| 6   | `GET /materia/ordia/{codigo}`   | —                                 | ❌ Não ingerido                     | **NOVA tabela** (se endpoint ainda ativo) |

### 4.3. Endpoints de Referência (tabelas de domínio — Senado)

| #   | Endpoint                       | Descrição             | Ação                       |
| --- | ------------------------------ | --------------------- | -------------------------- |
| 7   | `GET /processo/tipos-situacao` | Tipos de situação     | **NOVA tabela** de domínio |
| 8   | `GET /processo/tipos-decisao`  | Tipos de decisão      | **NOVA tabela** de domínio |
| 9   | `GET /processo/tipos-autor`    | Tipos de autor        | **NOVA tabela** de domínio |
| 10  | `GET /processo/siglas`         | Siglas de processos   | **NOVA tabela** de domínio |
| 11  | `GET /processo/classes`        | Classes de processos  | **NOVA tabela** de domínio |
| 12  | `GET /processo/assuntos`       | Assuntos legislativos | **NOVA tabela** de domínio |

---

## 5. Análise de Fontes de Dados CSV (Câmara)

A Câmara dos Deputados disponibiliza arquivos CSV de atualização diária, que devem ser a **fonte primária de ingestão em lote** para os dados da Câmara. A premissa é: se existe CSV, usar CSV.

### 5.1. CSVs de Proposições Relacionados (Fonte Primária)

| Arquivo CSV                    | Segmentação         | URL de Download                                                                                  | Tabela Silver Alvo                        |
| ------------------------------ | ------------------- | ------------------------------------------------------------------------------------------------ | ----------------------------------------- |
| `proposicoes-{ano}.csv`        | Por ano (1934–2026) | `http://dadosabertos.camara.leg.br/arquivos/proposicoes/csv/proposicoes-{ano}.csv`               | `silver.camara_proposicao` ✅ existente   |
| `proposicoesTemas-{ano}.csv`   | Por ano (1934–2026) | `http://dadosabertos.camara.leg.br/arquivos/proposicoesTemas/csv/proposicoesTemas-{ano}.csv`     | `silver.camara_proposicao_tema` **NOVO**  |
| `proposicoesAutores-{ano}.csv` | Por ano (2007–2026) | `http://dadosabertos.camara.leg.br/arquivos/proposicoesAutores/csv/proposicoesAutores-{ano}.csv` | `silver.camara_proposicao_autor` **NOVO** |

### 5.2. CSVs de Votações Relacionados (Fonte Primária)

| Arquivo CSV                     | Segmentação         | URL de Download                                                                                    | Tabela Silver Alvo                          |
| ------------------------------- | ------------------- | -------------------------------------------------------------------------------------------------- | ------------------------------------------- |
| `votacoes-{ano}.csv`            | Por ano (1935–2025) | `http://dadosabertos.camara.leg.br/arquivos/votacoes/csv/votacoes-{ano}.csv`                       | `silver.camara_votacao` **NOVO**            |
| `votacoesOrientacoes-{ano}.csv` | Por ano (2003–2025) | `http://dadosabertos.camara.leg.br/arquivos/votacoesOrientacoes/csv/votacoesOrientacoes-{ano}.csv` | `silver.camara_votacao_orientacao` **NOVO** |
| `votacoesVotos-{ano}.csv`       | Por ano (2003–2025) | `http://dadosabertos.camara.leg.br/arquivos/votacoesVotos/csv/votacoesVotos-{ano}.csv`             | `silver.camara_votacao_voto` **NOVO**       |

### 5.3. Estratégia CSV vs API

```
                                                   ┌─────────────────────┐
                                                   │   Existe CSV?       │
                                                   └──────┬──────────────┘
                                                          │
                                          Sim ◄───────────┤────────────► Não
                                           │                              │
                                ┌──────────▼──────────┐      ┌───────────▼────────────┐
                                │  Ingestão em lote    │      │  Enriquecimento via    │
                                │  via CSV (primário)  │      │  API REST (secundário) │
                                └──────────┬──────────┘      └───────────┬────────────┘
                                           │                              │
                                ┌──────────▼──────────┐      ┌───────────▼────────────┐
                                │  Complemento de      │      │  Registros salvos na   │
                                │  detalhe via API     │      │  tabela Silver          │
                                │  (quando necessário)  │     │  diretamente            │
                                └─────────────────────┘      └────────────────────────┘
```

---

## 6. Modelo de Dados Silver — Novas Tabelas

### 6.1. Câmara — Tabelas com Origem CSV (Lote)

#### `silver.camara_proposicao_tema`

**Fonte primária:** CSV `proposicoesTemas-{ano}.csv`  
**Fonte secundária (enriquecimento):** API `GET /proposicoes/{id}/temas`

```sql
CREATE TABLE silver.camara_proposicao_tema (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id             UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em            TIMESTAMP NOT NULL DEFAULT NOW(),
    content_hash           VARCHAR(64),
    origem_carga           VARCHAR(20) NOT NULL DEFAULT 'CSV',

    -- Campos do CSV proposicoesTemas-{ano}.csv (espelho fiel)
    uri_proposicao         VARCHAR(500),
    sigla_tipo             VARCHAR(20),
    numero                 INTEGER,
    ano                    INTEGER,
    cod_tema               INTEGER,
    tema                   VARCHAR(200),

    -- Relacionamento com silver.camara_proposicao
    camara_proposicao_id   UUID REFERENCES silver.camara_proposicao(id),

    -- Deduplicação
    CONSTRAINT uq_camara_proposicao_tema UNIQUE (uri_proposicao, cod_tema)
);
```

#### `silver.camara_proposicao_autor`

**Fonte primária:** CSV `proposicoesAutores-{ano}.csv`  
**Fonte secundária (enriquecimento):** API `GET /proposicoes/{id}/autores`

```sql
CREATE TABLE silver.camara_proposicao_autor (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id             UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em            TIMESTAMP NOT NULL DEFAULT NOW(),
    content_hash           VARCHAR(64),
    origem_carga           VARCHAR(20) NOT NULL DEFAULT 'CSV',

    -- Campos do CSV proposicoesAutores-{ano}.csv (espelho fiel)
    uri_proposicao         VARCHAR(500),
    sigla_tipo             VARCHAR(20),
    numero                 INTEGER,
    ano                    INTEGER,
    cod_tipo_autor         INTEGER,
    tipo_autor             VARCHAR(100),
    nome_autor             VARCHAR(500),
    uri_autor              VARCHAR(500),
    ordem_assinatura       INTEGER,
    proponente             INTEGER,

    -- Relacionamento com silver.camara_proposicao
    camara_proposicao_id   UUID REFERENCES silver.camara_proposicao(id),

    -- Deduplicação
    CONSTRAINT uq_camara_proposicao_autor UNIQUE (uri_proposicao, nome_autor, ordem_assinatura)
);
```

#### `silver.camara_votacao`

**Fonte primária:** CSV `votacoes-{ano}.csv`  
**Fonte secundária (enriquecimento):** API `GET /votacoes/{id}`

```sql
CREATE TABLE silver.camara_votacao (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em               TIMESTAMP,
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'CSV',

    -- Campos do CSV votacoes-{ano}.csv (espelho fiel)
    votacao_id                  VARCHAR(100) NOT NULL,
    uri                         VARCHAR(500),
    data                        VARCHAR(30),
    data_hora_registro          VARCHAR(30),
    sigla_orgao                 VARCHAR(30),
    uri_orgao                   VARCHAR(500),
    uri_evento                  VARCHAR(500),
    proposicao_objeto           TEXT,
    uri_proposicao_objeto       VARCHAR(500),
    descricao                   TEXT,
    aprovacao                   INTEGER,

    -- Campos adicionais da API /votacoes/{id} (enriquecimento)
    -- (a preencher durante enriquecimento)

    -- Relacionamento com silver.camara_proposicao (se aplicável)
    camara_proposicao_id        UUID REFERENCES silver.camara_proposicao(id),

    CONSTRAINT uq_camara_votacao_id UNIQUE (votacao_id)
);
```

#### `silver.camara_votacao_orientacao`

**Fonte primária:** CSV `votacoesOrientacoes-{ano}.csv`

```sql
CREATE TABLE silver.camara_votacao_orientacao (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'CSV',

    -- Campos do CSV votacoesOrientacoes-{ano}.csv (espelho fiel)
    votacao_id                  VARCHAR(100) NOT NULL,
    uri_votacao                 VARCHAR(500),
    sigla_partido_bloco         VARCHAR(100),
    cod_partido_bloco           INTEGER,
    uri_partido_bloco           VARCHAR(500),
    cod_tipo_lideranca          VARCHAR(30),
    orientacao_voto             VARCHAR(50),

    -- Relacionamento com silver.camara_votacao
    camara_votacao_id           UUID REFERENCES silver.camara_votacao(id),

    CONSTRAINT uq_camara_votacao_orientacao UNIQUE (votacao_id, sigla_partido_bloco)
);
```

#### `silver.camara_votacao_voto`

**Fonte primária:** CSV `votacoesVotos-{ano}.csv`

```sql
CREATE TABLE silver.camara_votacao_voto (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'CSV',

    -- Campos do CSV votacoesVotos-{ano}.csv (espelho fiel)
    votacao_id                  VARCHAR(100) NOT NULL,
    uri_votacao                 VARCHAR(500),
    deputado_id                 INTEGER,
    deputado_uri                VARCHAR(500),
    deputado_nome               VARCHAR(300),
    deputado_sigla_partido      VARCHAR(30),
    deputado_uri_partido        VARCHAR(500),
    deputado_sigla_uf           VARCHAR(5),
    deputado_id_legislatura     INTEGER,
    deputado_url_foto           VARCHAR(500),
    tipo_voto                   VARCHAR(50),
    data_registro_voto          VARCHAR(30),

    -- Relacionamento com silver.camara_votacao
    camara_votacao_id           UUID REFERENCES silver.camara_votacao(id),

    CONSTRAINT uq_camara_votacao_voto UNIQUE (votacao_id, deputado_id)
);
```

### 6.2. Câmara — Tabela com Origem API (Enriquecimento)

#### `silver.camara_proposicao_relacionada`

**Fonte:** API `GET /proposicoes/{id}/relacionadas` (não existe CSV)

```sql
CREATE TABLE silver.camara_proposicao_relacionada (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos do endpoint /proposicoes/{id}/relacionadas (espelho fiel)
    proposicao_id               VARCHAR(100) NOT NULL,
    relacionada_id              INTEGER NOT NULL,
    relacionada_uri             VARCHAR(500),
    relacionada_sigla_tipo      VARCHAR(20),
    relacionada_numero          VARCHAR(20),
    relacionada_ano             VARCHAR(10),
    relacionada_ementa          TEXT,
    relacionada_cod_tipo        VARCHAR(20),

    -- Relacionamento com silver.camara_proposicao
    camara_proposicao_id        UUID REFERENCES silver.camara_proposicao(id),

    CONSTRAINT uq_camara_proposicao_relacionada UNIQUE (proposicao_id, relacionada_id)
);
```

### 6.3. Senado — Novas Tabelas (API `/processo`)

> **Nota:** As tabelas do Senado acompanham a estrutura da nova API `/processo/*` para não depender de endpoints deprecados.

#### `silver.senado_emenda`

**Fonte:** API `GET /processo/emenda?codigoMateria={codigo}`

```sql
CREATE TABLE silver.senado_emenda (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos da resposta (espelho fiel da estrutura do endpoint)
    codigo_emenda               VARCHAR(50),
    tipo_emenda                 VARCHAR(100),
    descricao_tipo_emenda       VARCHAR(200),
    numero_emenda               VARCHAR(50),
    data_apresentacao           VARCHAR(30),
    colegiado_apresentacao      VARCHAR(200),
    turno                       VARCHAR(50),
    autor_nome                  VARCHAR(500),
    autor_codigo_parlamentar    VARCHAR(50),
    autor_tipo                  VARCHAR(100),
    ementa                      TEXT,
    inteiro_teor_url            VARCHAR(1000),

    -- Relacionamento com silver.senado_materia
    senado_materia_id           UUID REFERENCES silver.senado_materia(id),
    codigo_materia              VARCHAR(50),

    CONSTRAINT uq_senado_emenda UNIQUE (senado_materia_id, codigo_emenda)
);
```

#### `silver.senado_votacao`

**Fonte:** API `GET /votacao?codigoMateria={codigo}` (nova API)

```sql
CREATE TABLE silver.senado_votacao (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos do endpoint /votacao (espelho fiel)
    codigo_sessao               VARCHAR(50),
    sigla_casa                  VARCHAR(10),
    codigo_sessao_votacao       VARCHAR(50),
    sequencial_sessao           VARCHAR(50),
    data_sessao                 VARCHAR(30),
    descricao_votacao           TEXT,
    resultado                   VARCHAR(200),
    descricao_resultado         TEXT,
    total_votos_sim             INTEGER,
    total_votos_nao             INTEGER,
    total_votos_abstencao       INTEGER,
    indicador_votacao_secreta   VARCHAR(10),
    votos_parlamentares         JSONB,

    -- Relacionamento com silver.senado_materia
    senado_materia_id           UUID REFERENCES silver.senado_materia(id),
    codigo_materia              VARCHAR(50),

    CONSTRAINT uq_senado_votacao UNIQUE (senado_materia_id, codigo_sessao_votacao, sequencial_sessao)
);
```

#### `silver.senado_documento`

**Fonte:** API `GET /processo/documento?codigoMateria={codigo}`

```sql
CREATE TABLE silver.senado_documento (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos do endpoint /processo/documento (espelho fiel)
    codigo_documento            VARCHAR(50),
    tipo_documento              VARCHAR(200),
    descricao_tipo_documento    VARCHAR(500),
    data_documento              VARCHAR(30),
    descricao_documento         TEXT,
    url_documento               VARCHAR(1000),
    tipo_conteudo               VARCHAR(100),
    autor_nome                  VARCHAR(500),
    autor_codigo_parlamentar    VARCHAR(50),

    -- Relacionamento com silver.senado_materia
    senado_materia_id           UUID REFERENCES silver.senado_materia(id),
    codigo_materia              VARCHAR(50),

    CONSTRAINT uq_senado_documento UNIQUE (senado_materia_id, codigo_documento)
);
```

#### `silver.senado_prazo`

**Fonte:** API `GET /processo/prazo?codigoMateria={codigo}`

```sql
CREATE TABLE silver.senado_prazo (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id                  UUID NOT NULL REFERENCES etl_job_control(id),
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT NOW(),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',

    -- Campos do endpoint /processo/prazo (espelho fiel)
    tipo_prazo                  VARCHAR(200),
    data_inicio                 VARCHAR(30),
    data_fim                    VARCHAR(30),
    descricao                   TEXT,
    colegiado                   VARCHAR(200),
    situacao                    VARCHAR(100),

    -- Relacionamento com silver.senado_materia
    senado_materia_id           UUID REFERENCES silver.senado_materia(id),
    codigo_materia              VARCHAR(50),

    CONSTRAINT uq_senado_prazo UNIQUE (senado_materia_id, tipo_prazo, data_inicio)
);
```

### 6.4. Senado — Tabelas de Referência/Domínio

Tabelas estáticas carregadas uma vez e atualizadas periodicamente:

#### `silver.senado_ref_tipo_situacao`

```sql
CREATE TABLE silver.senado_ref_tipo_situacao (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    codigo          VARCHAR(50) NOT NULL UNIQUE,
    descricao       VARCHAR(500)
);
```

#### `silver.senado_ref_tipo_decisao`

```sql
CREATE TABLE silver.senado_ref_tipo_decisao (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    codigo          VARCHAR(50) NOT NULL UNIQUE,
    descricao       VARCHAR(500)
);
```

#### `silver.senado_ref_tipo_autor`

```sql
CREATE TABLE silver.senado_ref_tipo_autor (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    codigo          VARCHAR(50) NOT NULL UNIQUE,
    descricao       VARCHAR(500)
);
```

#### `silver.senado_ref_sigla`

```sql
CREATE TABLE silver.senado_ref_sigla (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    sigla           VARCHAR(30) NOT NULL UNIQUE,
    descricao       VARCHAR(500),
    classe          VARCHAR(200)
);
```

#### `silver.senado_ref_classe`

```sql
CREATE TABLE silver.senado_ref_classe (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    codigo          VARCHAR(50) NOT NULL UNIQUE,
    descricao       VARCHAR(500),
    classe_pai      VARCHAR(50)
);
```

#### `silver.senado_ref_assunto`

```sql
CREATE TABLE silver.senado_ref_assunto (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    codigo          VARCHAR(50) NOT NULL UNIQUE,
    assunto_geral   VARCHAR(500),
    assunto_especifico VARCHAR(500)
);
```

---

## 7. Integração com Processos Existentes

### 7.1. Fluxo ETL Atual (preservado)

```
Bronze CSV/API ──► SilverCamaraLoader / SilverSenadoLoader ──► Silver Tables
                                                                     │
SilverEnrichmentService.enriquecerTudo() ◄──────────────────────────┘
     ├── enriquecerDetalhesSenado()
     ├── enriquecerTramitacoesCamara()
     ├── enriquecerMovimentacoesSenado()
     ├── enriquecerAutoriasSenado()
     └── enriquecerRelatoriasSenado()
```

### 7.2. Fluxo ETL Expandido

```
Bronze CSV/API ──► Loaders existentes ──► Silver Tables existentes
       │                                        │
       │   ┌────────────────────────────────────┘
       │   │
       ▼   ▼
 NOVOS Loaders CSV (Câmara)          NOVOS Enrichments (Senado)
 ┌──────────────────────────┐        ┌──────────────────────────────┐
 │ SilverCamaraTemasLoader  │        │ enriquecerEmendasSenado()    │
 │ SilverCamaraAutoresLoader│        │ enriquecerVotacoesSenado()   │
 │ SilverCamaraVotacaoLoader│        │ enriquecerDocumentosSenado() │
 │ SilverCamaraOrientLoader │        │ enriquecerPrazosSenado()     │
 │ SilverCamaraVotosLoader  │        │ carregarReferenciasSenado()  │
 └──────────────────────────┘        └──────────────────────────────┘
       │                                        │
       ▼                                        ▼
 Silver Tables (novas)                Silver Tables (novas)
       │                                        │
       └────────────────┬───────────────────────┘
                        ▼
              SilverEnrichmentService.enriquecerTudo() (expandido)
                        │
                        ▼
              enriquecerRelacionadasCamara() ← API-only (sem CSV)
```

### 7.3. Ponto de Integração: `SilverEnrichmentService`

O serviço existente `SilverEnrichmentService` será expandido com novos métodos:

```java
// Métodos existentes (mantidos)
enriquecerDetalhesSenado()
enriquecerTramitacoesCamara()
enriquecerMovimentacoesSenado()
enriquecerAutoriasSenado()
enriquecerRelatoriasSenado()

// Novos métodos (adicionados)
enriquecerRelacionadasCamara()     // API /proposicoes/{id}/relacionadas
enriquecerEmendasSenado()          // API /processo/emenda
enriquecerVotacoesSenado()         // API /votacao
enriquecerDocumentosSenado()       // API /processo/documento
enriquecerPrazosSenado()           // API /processo/prazo
```

### 7.4. Ponto de Integração: `EtlJobControl`

Todos os novos processos registram jobs em `etl_job_control`, seguindo o padrão existente:

```java
EtlJobControl job = EtlJobControl.builder()
    .origem(CasaLegislativa.CAMARA)  // ou SENADO
    .tipoExecucao(TipoExecucao.FULL)
    .iniciadoEm(LocalDateTime.now())
    .build();
```

### 7.5. Novos Pipelines CSV (Câmara)

O pipeline de carga CSV segue o padrão já estabelecido para `proposicoes-{ano}.csv`:

1. Download do arquivo CSV via `camaraCsvWebClient`
2. Parse com `CsvMapper` (OpenCSV ou Jackson CSV)
3. Mapeamento para entidade Silver
4. Carga via Loader com deduplicação por UNIQUE constraint
5. Registro em `EtlJobControl` e `EtlFileControl`

```
                    ┌──────────────────────────────────┐
                    │  Para cada novo CSV (ano a ano):  │
                    │                                   │
 Download ──►  CSV ──► CsvExtractor ──► List<Entity>   │
                                             │          │
                                    SilverLoader.carregar()
                                             │          │
                                      Silver Table      │
                    └──────────────────────────────────┘
```

---

## 8. Estratégia de Migração Senado: materia → processo

### 8.1. Contexto

Os endpoints legados (`/materia/*`) já ultrapassaram a data oficial de desativação. O sistema atual depende deles para:

- Pesquisa/listagem: `GET /materia/pesquisa/lista`
- Detalhe: `GET /materia/{codigo}`
- Movimentações: `GET /materia/{codigo}/movimentacoes`
- Autoria: `GET /materia/autoria/{codigo}`

### 8.2. Plano de Migração (fora do escopo direto, mas preparatório)

| Etapa | De                                | Para                                       | Impacto                                                                  |
| ----- | --------------------------------- | ------------------------------------------ | ------------------------------------------------------------------------ |
| 1     | `/materia/pesquisa/lista`         | `GET /processo`                            | `SenadoApiExtractor.extractRawByYearRange()`                             |
| 2     | `/materia/{codigo}`               | `GET /processo/{idProcesso}`               | `SenadoApiExtractor.fetchRawDetalhe()` — requer `identificacao_processo` |
| 3     | `/materia/{codigo}/movimentacoes` | Dados inclusos em `/processo/{idProcesso}` | `SenadoApiExtractor.fetchRawMovimentacoes()`                             |
| 4     | `/materia/autoria/{codigo}`       | Dados inclusos em `/processo/{idProcesso}` | `SenadoApiExtractor.fetchRawAutoria()`                                   |

**Observação:** O campo `identificacao_processo` já é persistido em `silver.senado_materia`, servindo como chave para a nova API.

### 8.3. Recomendação

Para os **novos endpoints** deste plano (emendas, documentos, votações, prazos), usar exclusivamente a **nova API `/processo/*`** e `/votacao`, evitando qualquer dependência dos endpoints deprecados.

Para os **endpoints existentes**, planejar migração progressiva como trabalho paralelo.

---

## 9. Faseamento da Implementação

### Fase 3 — Ingestão CSV Câmara: Temas e Autores

**Prioridade:** Alta  
**Justificativa:** CSV disponível, baixa complexidade, alto valor informacional

| Item | Artefato                                        | Tipo |
| ---- | ----------------------------------------------- | ---- |
| 3.1  | Migration V18: `silver.camara_proposicao_tema`  | SQL  |
| 3.2  | Migration V19: `silver.camara_proposicao_autor` | SQL  |
| 3.3  | `SilverCamaraProposicaoTema` (entidade)         | Java |
| 3.4  | `SilverCamaraProposicaoAutor` (entidade)        | Java |
| 3.5  | `SilverCamaraProposicaoTemaRepository`          | Java |
| 3.6  | `SilverCamaraProposicaoAutorRepository`         | Java |
| 3.7  | `CamaraTemasCSVExtractor`                       | Java |
| 3.8  | `CamaraAutoresCSVExtractor`                     | Java |
| 3.9  | `SilverCamaraTemasLoader`                       | Java |
| 3.10 | `SilverCamaraAutoresLoader`                     | Java |
| 3.11 | Testes unitários para loaders e extractors      | Java |
| 3.12 | Integração com orquestrador de ingestão         | Java |

### Fase 4 — Ingestão CSV Câmara: Votações

**Prioridade:** Alta  
**Justificativa:** CSV disponível, 3 datasets relacionados (votações, orientações, votos)

| Item | Artefato                                                                                      | Tipo |
| ---- | --------------------------------------------------------------------------------------------- | ---- |
| 4.1  | Migration V20: `silver.camara_votacao`                                                        | SQL  |
| 4.2  | Migration V21: `silver.camara_votacao_orientacao`                                             | SQL  |
| 4.3  | Migration V22: `silver.camara_votacao_voto`                                                   | SQL  |
| 4.4  | `SilverCamaraVotacao`, `SilverCamaraVotacaoOrientacao`, `SilverCamaraVotacaoVoto` (entidades) | Java |
| 4.5  | Repositories para as 3 entidades                                                              | Java |
| 4.6  | `CamaraVotacoesCSVExtractor`, `CamaraOrientacoesCSVExtractor`, `CamaraVotosCSVExtractor`      | Java |
| 4.7  | `SilverCamaraVotacaoLoader`, `SilverCamaraOrientacaoLoader`, `SilverCamaraVotosLoader`        | Java |
| 4.8  | Vinculação `camara_proposicao_id` via URI da proposição                                       | Java |
| 4.9  | Testes unitários                                                                              | Java |

### Fase 5 — Enriquecimento API Câmara: Relacionadas

**Prioridade:** Média  
**Justificativa:** Sem CSV, depende de API individual por proposição

| Item | Artefato                                                 | Tipo |
| ---- | -------------------------------------------------------- | ---- |
| 5.1  | Migration V23: `silver.camara_proposicao_relacionada`    | SQL  |
| 5.2  | `SilverCamaraProposicaoRelacionada` (entidade)           | Java |
| 5.3  | `SilverCamaraProposicaoRelacionadaRepository`            | Java |
| 5.4  | `CamaraApiExtractor.fetchRelacionadas(String camaraId)`  | Java |
| 5.5  | `CamaraRelacionadasDTO`                                  | Java |
| 5.6  | `SilverCamaraRelacionadasLoader`                         | Java |
| 5.7  | `SilverEnrichmentService.enriquecerRelacionadasCamara()` | Java |
| 5.8  | Testes unitários                                         | Java |

### Fase 6 — Ingestão API Senado: Emendas, Documentos, Prazos

**Prioridade:** Alta  
**Justificativa:** Nova API `/processo/*`, sem dependência de endpoints deprecados

| Item | Artefato                                                                                      | Tipo |
| ---- | --------------------------------------------------------------------------------------------- | ---- |
| 6.1  | Migration V24: `silver.senado_emenda`                                                         | SQL  |
| 6.2  | Migration V25: `silver.senado_documento`                                                      | SQL  |
| 6.3  | Migration V26: `silver.senado_prazo`                                                          | SQL  |
| 6.4  | Entidades: `SilverSenadoEmenda`, `SilverSenadoDocumento`, `SilverSenadoPrazo`                 | Java |
| 6.5  | Repositories para as 3 entidades                                                              | Java |
| 6.6  | DTOs: `SenadoEmendaDTO`, `SenadoDocumentoDTO`, `SenadoPrazoDTO`                               | Java |
| 6.7  | `SenadoApiExtractor`: novos métodos `fetchEmendas()`, `fetchDocumentos()`, `fetchPrazos()`    | Java |
| 6.8  | Loaders: `SilverSenadoEmendaLoader`, `SilverSenadoDocumentoLoader`, `SilverSenadoPrazoLoader` | Java |
| 6.9  | `SilverEnrichmentService`: novos métodos de enriquecimento                                    | Java |
| 6.10 | Testes unitários                                                                              | Java |

### Fase 7 — Ingestão API Senado: Votações

**Prioridade:** Média  
**Justificativa:** Nova API `/votacao`, complementa informações de tramitação

| Item | Artefato                                                 | Tipo |
| ---- | -------------------------------------------------------- | ---- |
| 7.1  | Migration V27: `silver.senado_votacao`                   | SQL  |
| 7.2  | `SilverSenadoVotacao` (entidade)                         | Java |
| 7.3  | `SilverSenadoVotacaoRepository`                          | Java |
| 7.4  | `SenadoVotacaoDTO`                                       | Java |
| 7.5  | `SenadoApiExtractor.fetchVotacoes(String codigoMateria)` | Java |
| 7.6  | `SilverSenadoVotacaoLoader`                              | Java |
| 7.7  | `SilverEnrichmentService.enriquecerVotacoesSenado()`     | Java |
| 7.8  | Testes unitários                                         | Java |

### Fase 8 — Tabelas de Referência (Senado)

**Prioridade:** Baixa  
**Justificativa:** Dados estáticos, carregados uma vez

| Item | Artefato                                                                          | Tipo |
| ---- | --------------------------------------------------------------------------------- | ---- |
| 8.1  | Migration V28: tabelas `silver.senado_ref_*` (6 tabelas)                          | SQL  |
| 8.2  | Entidades de referência (6 classes)                                               | Java |
| 8.3  | Repositories de referência                                                        | Java |
| 8.4  | `SenadoApiExtractor`: métodos `fetchTiposSituacao()`, `fetchTiposDecisao()`, etc. | Java |
| 8.5  | `SilverSenadoReferenciaLoader` (genérico ou 1 por tipo)                           | Java |
| 8.6  | Testes unitários                                                                  | Java |

---

## 10. Migrações Flyway

### 10.1. Sequência Proposta

| Versão | Arquivo                                                | Fase | Descrição                      |
| ------ | ------------------------------------------------------ | ---- | ------------------------------ |
| V18    | `V18__create_silver_camara_proposicao_tema.sql`        | 3    | Temas de proposições (CSV)     |
| V19    | `V19__create_silver_camara_proposicao_autor.sql`       | 3    | Autores de proposições (CSV)   |
| V20    | `V20__create_silver_camara_votacao.sql`                | 4    | Votações (CSV)                 |
| V21    | `V21__create_silver_camara_votacao_orientacao.sql`     | 4    | Orientações de bancada (CSV)   |
| V22    | `V22__create_silver_camara_votacao_voto.sql`           | 4    | Votos individuais (CSV)        |
| V23    | `V23__create_silver_camara_proposicao_relacionada.sql` | 5    | Proposições relacionadas (API) |
| V24    | `V24__create_silver_senado_emenda.sql`                 | 6    | Emendas do Senado (API)        |
| V25    | `V25__create_silver_senado_documento.sql`              | 6    | Documentos do Senado (API)     |
| V26    | `V26__create_silver_senado_prazo.sql`                  | 6    | Prazos do Senado (API)         |
| V27    | `V27__create_silver_senado_votacao.sql`                | 7    | Votações do Senado (API)       |
| V28    | `V28__create_silver_senado_ref_tables.sql`             | 8    | 6 tabelas de referência        |
| V29    | `V29__create_silver_new_indexes.sql`                   | 8    | Índices para novas tabelas     |

### 10.2. Padrão de Índices

Cada nova tabela deve ter, no mínimo:

- Índice na FK `etl_job_id`
- Índice na FK de relacionamento (`camara_proposicao_id` ou `senado_materia_id`)
- Índice no campo de negócio usado como chave natural (e.g., `votacao_id`, `codigo_emenda`)

---

## 11. Componentes Java — Novos Artefatos

### 11.1. Extractores CSV (Câmara)

Seguir o padrão do extractor CSV existente para `proposicoes-{ano}.csv`:

| Classe                          | CSV                             | Método principal                                        |
| ------------------------------- | ------------------------------- | ------------------------------------------------------- |
| `CamaraTemasCSVExtractor`       | `proposicoesTemas-{ano}.csv`    | `extract(int ano): List<SilverCamaraProposicaoTema>`    |
| `CamaraAutoresCSVExtractor`     | `proposicoesAutores-{ano}.csv`  | `extract(int ano): List<SilverCamaraProposicaoAutor>`   |
| `CamaraVotacoesCSVExtractor`    | `votacoes-{ano}.csv`            | `extract(int ano): List<SilverCamaraVotacao>`           |
| `CamaraOrientacoesCSVExtractor` | `votacoesOrientacoes-{ano}.csv` | `extract(int ano): List<SilverCamaraVotacaoOrientacao>` |
| `CamaraVotosCSVExtractor`       | `votacoesVotos-{ano}.csv`       | `extract(int ano): List<SilverCamaraVotacaoVoto>`       |

**URL de download:** `http://dadosabertos.camara.leg.br/arquivos/{dataset}/csv/{dataset}-{ano}.csv`

### 11.2. Extractores API (novos métodos)

**`CamaraApiExtractor` — novos métodos:**

```java
List<CamaraRelacionadaDTO> fetchRelacionadas(String camaraId)
// GET /api/v2/proposicoes/{id}/relacionadas
```

**`SenadoApiExtractor` — novos métodos:**

```java
List<SenadoEmendaDTO> fetchEmendas(String codigoMateria)
// GET /processo/emenda?codigoMateria={codigo}

List<SenadoDocumentoDTO> fetchDocumentos(String codigoMateria)
// GET /processo/documento?codigoMateria={codigo}

List<SenadoPrazoDTO> fetchPrazos(String codigoMateria)
// GET /processo/prazo?codigoMateria={codigo}

List<SenadoVotacaoDTO> fetchVotacoes(String codigoMateria)
// GET /votacao?codigoMateria={codigo}

// Referências (carga única)
List<SenadoRefDTO> fetchTiposSituacao()
List<SenadoRefDTO> fetchTiposDecisao()
List<SenadoRefDTO> fetchTiposAutor()
List<SenadoRefDTO> fetchSiglas()
List<SenadoRefDTO> fetchClasses()
List<SenadoRefDTO> fetchAssuntos()
```

### 11.3. Loaders (todos seguem o padrão existente)

| Classe                           | Tabela                                 | Estratégia                                                     |
| -------------------------------- | -------------------------------------- | -------------------------------------------------------------- |
| `SilverCamaraTemasLoader`        | `silver.camara_proposicao_tema`        | Upsert por `(uri_proposicao, cod_tema)`                        |
| `SilverCamaraAutoresLoader`      | `silver.camara_proposicao_autor`       | Upsert por `(uri_proposicao, nome_autor, ordem_assinatura)`    |
| `SilverCamaraVotacaoLoader`      | `silver.camara_votacao`                | Upsert por `votacao_id`                                        |
| `SilverCamaraOrientacaoLoader`   | `silver.camara_votacao_orientacao`     | Upsert por `(votacao_id, sigla_partido_bloco)`                 |
| `SilverCamaraVotosLoader`        | `silver.camara_votacao_voto`           | Upsert por `(votacao_id, deputado_id)`                         |
| `SilverCamaraRelacionadasLoader` | `silver.camara_proposicao_relacionada` | Insert-only por `(proposicao_id, relacionada_id)`              |
| `SilverSenadoEmendaLoader`       | `silver.senado_emenda`                 | Insert-only por `(senado_materia_id, codigo_emenda)`           |
| `SilverSenadoDocumentoLoader`    | `silver.senado_documento`              | Insert-only por `(senado_materia_id, codigo_documento)`        |
| `SilverSenadoPrazoLoader`        | `silver.senado_prazo`                  | Insert-only por `(senado_materia_id, tipo_prazo, data_inicio)` |
| `SilverSenadoVotacaoLoader`      | `silver.senado_votacao`                | Insert-only por chave composta                                 |
| `SilverSenadoReferenciaLoader`   | `silver.senado_ref_*`                  | Full replace (truncate + insert)                               |

### 11.4. DTOs Novos

| Classe                 | Endpoint                         | Localização            |
| ---------------------- | -------------------------------- | ---------------------- |
| `CamaraRelacionadaDTO` | `/proposicoes/{id}/relacionadas` | `extractor.camara.dto` |
| `SenadoEmendaDTO`      | `/processo/emenda`               | `extractor.senado.dto` |
| `SenadoDocumentoDTO`   | `/processo/documento`            | `extractor.senado.dto` |
| `SenadoPrazoDTO`       | `/processo/prazo`                | `extractor.senado.dto` |
| `SenadoVotacaoDTO`     | `/votacao`                       | `extractor.senado.dto` |
| `SenadoRefDTO`         | `/processo/tipos-*`              | `extractor.senado.dto` |

---

## 12. Estratégia de Enriquecimento

### 12.1. Orquestração Completa (após todas as fases)

```java
public EtlJobControl enriquecerTudo() {
    // === FASE EXISTENTE ===
    enriquecerDetalhesSenado();        // /materia/{codigo} → det_* cols
    enriquecerTramitacoesCamara();     // /proposicoes/{id}/tramitacoes
    enriquecerMovimentacoesSenado();   // /materia/{codigo}/movimentacoes
    enriquecerAutoriasSenado();        // /materia/autoria/{codigo}
    enriquecerRelatoriasSenado();      // /processo/relatoria

    // === FASE NOVA: Câmara (API-only) ===
    enriquecerRelacionadasCamara();    // /proposicoes/{id}/relacionadas

    // === FASE NOVA: Senado (API /processo) ===
    enriquecerEmendasSenado();         // /processo/emenda
    enriquecerDocumentosSenado();      // /processo/documento
    enriquecerPrazosSenado();          // /processo/prazo
    enriquecerVotacoesSenado();        // /votacao

    return jobControl;
}
```

### 12.2. Carga CSV Independente (Câmara)

A carga de CSVs novos (temas, autores, votações, orientações, votos) ocorre **no pipeline principal**, junto com a carga de `proposicoes-{ano}.csv`, e **não** no `SilverEnrichmentService`:

```java
// No orquestrador principal (existente):
silverCamaraLoader.carregar(proposicoes, job);          // existente
silverCamaraTemasLoader.carregar(temas, job);           // NOVO
silverCamaraAutoresLoader.carregar(autores, job);       // NOVO
silverCamaraVotacaoLoader.carregar(votacoes, job);      // NOVO
silverCamaraOrientacaoLoader.carregar(orientacoes, job); // NOVO
silverCamaraVotosLoader.carregar(votos, job);           // NOVO
```

### 12.3. Controle de Pendências

Para enriquecimentos via API, usar o padrão existente de detecção de pendências:

| Tabela-mãe                 | Tabela-filha                           | Query de pendência                                                                                                                        |
| -------------------------- | -------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `silver.camara_proposicao` | `silver.camara_proposicao_relacionada` | `WHERE NOT EXISTS (SELECT 1 FROM silver.camara_proposicao_relacionada r WHERE r.camara_proposicao_id = p.id)`                             |
| `silver.senado_materia`    | `silver.senado_emenda`                 | `WHERE NOT EXISTS (SELECT 1 FROM silver.senado_emenda e WHERE e.senado_materia_id = m.id) AND m.det_sigla_casa_identificacao IS NOT NULL` |
| `silver.senado_materia`    | `silver.senado_documento`              | Idem padrão                                                                                                                               |
| `silver.senado_materia`    | `silver.senado_prazo`                  | Idem padrão                                                                                                                               |
| `silver.senado_materia`    | `silver.senado_votacao`                | Idem padrão                                                                                                                               |

---

## 13. Configuração e Rate Limiting

### 13.1. Rate Limiters (manter existentes)

```yaml
resilience4j.ratelimiter.instances:
  senadoApi:
    limit-for-period: 8 # 8 req/s (limite oficial: 10)
    limit-refresh-period: 1s
    timeout-duration: 500ms
  camaraApi:
    limit-for-period: 20 # 20 req/s
    limit-refresh-period: 1s
    timeout-duration: 500ms
```

### 13.2. WebClient para CSV Downloads

Reutilizar o bean `camaraCsvWebClient` (timeout 300s, buffer 200MB) já configurado para downloads de arquivos grandes.

### 13.3. Novas URLs CSV (application.yml)

```yaml
etl:
  camara:
    csv:
      base-url: http://dadosabertos.camara.leg.br/arquivos
      datasets:
        proposicoes: proposicoes/csv/proposicoes-{ano}.csv # existente
        temas: proposicoesTemas/csv/proposicoesTemas-{ano}.csv # NOVO
        autores: proposicoesAutores/csv/proposicoesAutores-{ano}.csv # NOVO
        votacoes: votacoes/csv/votacoes-{ano}.csv # NOVO
        orientacoes: votacoesOrientacoes/csv/votacoesOrientacoes-{ano}.csv # NOVO
        votos: votacoesVotos/csv/votacoesVotos-{ano}.csv # NOVO
```

### 13.4. Paralelismo para Enriquecimento

```yaml
etl:
  senado:
    enrichment:
      parallelism: 6 # Existente — manter
  camara:
    enrichment:
      parallelism: 4 # NOVO — para /proposicoes/{id}/relacionadas
```

---

## 14. Riscos e Mitigações

| #   | Risco                                                                         | Impacto                                             | Probabilidade | Mitigação                                                                             |
| --- | ----------------------------------------------------------------------------- | --------------------------------------------------- | ------------- | ------------------------------------------------------------------------------------- |
| 1   | **Desativação iminente de endpoints `/materia/*`**                            | Alto — quebraria pesquisa/detalhe/movimentações     | Alta          | Planejar migração para `/processo` como work-item separado e paralelo                 |
| 2   | **Volume de dados de votações e votos**                                       | Médio — CSVs podem ter milhões de registros         | Média         | Processar em chunks de 500 registros; usar `@Transactional` com saveAll               |
| 3   | **Campos CSV divergentes da documentação**                                    | Baixo — estrutura CSV pode mudar entre anos         | Baixa         | Mapear CSV com `@CsvBindByName` flexível; log de campos ignorados                     |
| 4   | **Novo endpoint `/processo` com estrutura diferente do legado**               | Médio — DTOs precisam ser redesenhados              | Alta          | Criar DTOs novos para `/processo/*`; não reusar DTOs de `/materia/*`                  |
| 5   | **Rate limit mais restritivo com mais endpoints**                             | Médio — mais chamadas simultâneas                   | Média         | Manter semáforo global (`AdaptiveRateLimiter`); enriquecimentos sequenciais no Senado |
| 6   | **FK `camara_proposicao_id` inconsistente em CSVs de temas/autores/votações** | Baixo — proposição pode não existir no Silver ainda | Média         | FK nullable; vincular em batch posterior; constraint `ON DELETE SET NULL`             |
| 7   | **Indisponibilidade temporária de endpoints novos (503/429)**                 | Baixo                                               | Média         | Retry exponencial + CircuitBreaker (Resilience4j já configurado)                      |

---

## 15. Critérios de Aceitação

### 15.1. Por Fase

| Fase | Critério                                                                                         |
| ---- | ------------------------------------------------------------------------------------------------ |
| 3    | CSVs de temas e autores são ingeridos com 100% dos campos preservados; re-execução é idempotente |
| 4    | CSVs de votações, orientações e votos são ingeridos; FK com `camara_votacao` funcional           |
| 5    | Proposições relacionadas são enriquecidas via API para todas as proposições pendentes            |
| 6    | Emendas, documentos e prazos do Senado são extraídos via `/processo/*`                           |
| 7    | Votações do Senado são extraídas via `/votacao`                                                  |
| 8    | Tabelas de referência populadas e consultáveis                                                   |

### 15.2. Globais

- [ ] Todas as novas tabelas seguem o schema `silver.*`
- [ ] Colunas espelham fielmente a estrutura da fonte (CSV ou API)
- [ ] UNIQUE constraints garantem idempotência
- [ ] `EtlJobControl` registra contadores para cada novo processo
- [ ] Rate limiting respeitado (zero erros 429 em execução normal)
- [ ] Testes unitários com cobertura mínima de 80% nos loaders
- [ ] Migrações Flyway aplicadas sem conflito com versões anteriores (V1–V17)
- [ ] README ou operação-ingestão atualizada com novos processos

---

## Apêndice A — Mapa de Tabelas Silver (Estado Final)

```
silver.
├── camara_proposicao              ✅ V8  (existente — CSV proposicoes-{ano}.csv + API detalhe)
├── camara_tramitacao              ✅ V9  (existente — API /proposicoes/{id}/tramitacoes)
├── camara_proposicao_tema         🆕 V18 (CSV proposicoesTemas-{ano}.csv)
├── camara_proposicao_autor        🆕 V19 (CSV proposicoesAutores-{ano}.csv)
├── camara_votacao                 🆕 V20 (CSV votacoes-{ano}.csv)
├── camara_votacao_orientacao      🆕 V21 (CSV votacoesOrientacoes-{ano}.csv)
├── camara_votacao_voto            🆕 V22 (CSV votacoesVotos-{ano}.csv)
├── camara_proposicao_relacionada  🆕 V23 (API /proposicoes/{id}/relacionadas)
├── senado_materia                 ✅ V10 (existente — API pesquisa + detalhe)
├── senado_movimentacao            ✅ V11 (existente — API movimentações)
├── senado_autoria                 ✅ V16 (existente — API autoria)
├── senado_relatoria               ✅ V17 (existente — API relatoria)
├── senado_emenda                  🆕 V24 (API /processo/emenda)
├── senado_documento               🆕 V25 (API /processo/documento)
├── senado_prazo                   🆕 V26 (API /processo/prazo)
├── senado_votacao                 🆕 V27 (API /votacao)
├── senado_ref_tipo_situacao       🆕 V28 (API /processo/tipos-situacao)
├── senado_ref_tipo_decisao        🆕 V28 (API /processo/tipos-decisao)
├── senado_ref_tipo_autor          🆕 V28 (API /processo/tipos-autor)
├── senado_ref_sigla               🆕 V28 (API /processo/siglas)
├── senado_ref_classe              🆕 V28 (API /processo/classes)
└── senado_ref_assunto             🆕 V28 (API /processo/assuntos)
```

**Total:** 6 tabelas existentes + 16 novas = **22 tabelas Silver**

---

## Apêndice B — Diagrama de Relacionamentos Silver

```
silver.camara_proposicao (1)
    ├──(N) silver.camara_tramitacao
    ├──(N) silver.camara_proposicao_tema
    ├──(N) silver.camara_proposicao_autor
    ├──(N) silver.camara_proposicao_relacionada
    └──(N) silver.camara_votacao (via uri_proposicao_objeto)
              ├──(N) silver.camara_votacao_orientacao
              └──(N) silver.camara_votacao_voto

silver.senado_materia (1)
    ├──(N) silver.senado_movimentacao
    ├──(N) silver.senado_autoria
    ├──(N) silver.senado_relatoria
    ├──(N) silver.senado_emenda
    ├──(N) silver.senado_documento
    ├──(N) silver.senado_prazo
    └──(N) silver.senado_votacao

silver.senado_ref_* (tabelas de referência — independentes)
```
