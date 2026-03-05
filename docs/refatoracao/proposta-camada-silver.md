# Proposta de Refatoração — Introdução da Camada Silver (Padrão Medalhão)

**Projeto:** `etl-congresso-java`  
**Data:** 2026-03-05  
**Autor:** Análise automatizada + revisão técnica  
**Status:** Proposta — pendente aprovação

---

## Sumário

1. [Contexto e Motivação](#1-contexto-e-motivação)
2. [Diagnóstico — Desvio em Relação ao Requisito Original](#2-diagnóstico--desvio-em-relação-ao-requisito-original)
3. [Arquitetura Proposta — Padrão Medalhão](#3-arquitetura-proposta--padrão-medalhão)
4. [Modelo de Dados Silver — Câmara (CSV + API)](#4-modelo-de-dados-silver--câmara-csv--api)
5. [Modelo de Dados Silver — Senado (API JSON)](#5-modelo-de-dados-silver--senado-api-json)
6. [Camada Gold — Alterações de Rastreabilidade](#6-camada-gold--alterações-de-rastreabilidade)
7. [Novo Fluxo ETL](#7-novo-fluxo-etl)
8. [Análise de Impacto no Código](#8-análise-de-impacto-no-código)
9. [Plano de Migrações Flyway](#9-plano-de-migrações-flyway)
10. [Estratégia de Implementação por Fases](#10-estratégia-de-implementação-por-fases)
11. [Riscos e Mitigações](#11-riscos-e-mitigações)
12. [Decisões em Aberto](#12-decisões-em-aberto)

---

## 1. Contexto e Motivação

### 1.1 Requisito Original (spec-etp-materias-proposicoes.md)

> _"Priorizar consumo **em lote via CSV** (quando disponível — especialmente Câmara)."_  
> _"Persistir dados estruturados em PostgreSQL."_

A interpretação correta deste requisito exige que os dados sejam **armazenados no banco preservando a estrutura e os campos da fonte** (raw fidelity), mantendo todos os campos e hierarquias originais:

- Câmara: estrutura do CSV (`proposicoes-{ano}.csv` — 31 colunas com separador `;`) + complemento do endpoint de detalhe (`GET /api/v2/proposicoes/{id}`) + tramitações (`GET /api/v2/proposicoes/{id}/tramitacoes`)  
- Senado: estrutura do JSON da API `/dadosabertos/materia/pesquisa/lista.json` + detalhe `/dadosabertos/materia/{codigo}.json` + movimentações (`/dadosabertos/materia/{codigo}/movimentacoes.json`) + textos (`/dadosabertos/materia/textos/{codigo}.json`)

### 1.2 Padrão Medalhão

O padrão medalhão organiza os dados em três camadas progressivas:

```
Bronze  →  Silver  →  Gold
 (raw)    (fiel à     (normalizado /
           fonte)      analítico)
```

| Camada | Também chamada | Papel | Presente hoje? |
|--------|----------------|-------|----------------|
| Bronze | Landing | Dado bruto em arquivo (CSV/JSON) | Sim — `data/tmp/camara/` |
| **Silver** | **Staging / Cleansed** | **Dado persistido fiel à fonte** | **Ausente** ← gap |
| Gold | Curated | Modelo analítico normalizado | Sim — tabela `proposicao` |

---

## 2. Diagnóstico — Desvio em Relação ao Requisito Original

### 2.1 Estado Atual

O pipeline atual vai diretamente de Bronze → Gold, perdendo a fidelidade ao dado fonte:

```
CSV Câmara ─→ [CamaraCSVExtractor]
                    │
                    ▼
API Câmara ─→ [CamaraProposicaoMapper]  ──→  proposicao (Gold)
                                                   ↑
API Senado ─→ [SenadoMateriaMapper]    ───────────┘
```

### 2.2 Campos Perdidos na Transformação Atual

**Câmara CSV — campos não persistidos:**

| Campo CSV | Situação atual |
|-----------|----------------|
| `codTipo` | Descartado |
| `descricaoTipo` | Descartado |
| `ementaDetalhada` | Mesclado em `ementa` (perde separação) |
| `uriOrgaoNumerador` | Descartado |
| `uriPropAnterior`, `uriPropPrincipal`, `uriPropPosterior` | Descartados |
| `urnFinal` | Descartado |
| `ultimoStatus_sequencia` | Descartado |
| `ultimoStatus_idOrgao`, `ultimoStatus_uriOrgao` | Descartados |
| `ultimoStatus_uriRelator` | Descartado |
| `ultimoStatus_regime` | Descartado |
| `ultimoStatus_idTipoTramitacao`, `ultimoStatus_idSituacao` | Descartados |
| `ultimoStatus_apreciacao` | Descartado |
| `ultimoStatus_url` | Descartado |

**Câmara API (detalhe) — campos não persistidos:**

| Campo API | Situação atual |
|-----------|----------------|
| `statusProposicao.codTipoTramitacao` | Descartado |
| `statusProposicao.codSituacao` | Descartado |
| `statusProposicao.ambito` | Descartado |
| `statusProposicao.apreciacao` | Descartado |
| `statusProposicao.url` | Descartado |
| `statusProposicao.uriUltimoRelator` | Descartado |
| `uriAutores` | Descartado |
| `texto`, `justificativa` | Descartados |
| `uriOrgaoNumerador` | Descartado |

**Senado pesquisa/lista — campos não persistidos:**

| Campo API | Situação atual |
|-----------|----------------|
| `IdentificacaoProcesso` | Descartado |
| `DescricaoIdentificacao` | Descartado |
| `Autor` | Descartado |
| `UrlDetalheMateria` | Descartado |

**Senado detalhe (`/materia/{id}`) — não consumido:**

O endpoint de detalhe do Senado não é consultado durante a carga full. Campos ausentes:
- `DadosBasicosMateria.IndexacaoMateria`  
- `DadosBasicosMateria.CasaIniciadoraNoLegislativo`  
- `DadosBasicosMateria.NaturezaMateria`  
- `IdentificacaoMateria.IndicadorTramitando`  
- `Classificacoes`  
- `OrigemMateria`

**Tramitações Senado (movimentações) — não persistidas:**

As movimentações do Senado chegam pelo endpoint `/dadosabertos/materia/{id}/movimentacoes.json`. Atualmente o mapper existe (`SenadoMateriaMapper.movimentacaoToTramitacao`) mas a chamada não está integrada ao fluxo full-load.

**Senado — URL do inteiro teor:**

O endpoint `/dadosabertos/materia/textos/{codigo}.json` é consumido pela classe `SenadoApiExtractor.fetchUrlTextoMateria()` quando `etl.senado.enrich-url-texto=true`, mas o resultado é gravado diretamente no campo Gold `url_inteiro_teor` sem persistência intermediária. A URL original da fonte é perdida se a configuração estiver desabilitada.

### 2.3 Bug Pré-existente — `CamaraProposicaoCSVRow` com Bindings Incorretos

> **CRÍTICO**: A DTO `CamaraProposicaoCSVRow.java` mapeia nomes de colunas CSV que **não existem** nos arquivos reais.  
> O CSV (verificado em todos os anos de 2001 a 2026) utiliza o prefixo `ultimoStatus_*`, mas o DTO usa `ultimaSituacao*`.

| Binding no DTO (atual) | Coluna real no CSV | Match? |
|------------------------|--------------------|--------|
| `ultimaSituacaoDescricaoSituacao` | `ultimoStatus_descricaoSituacao` | **NÃO** |
| `ultimaSituacaoDespacho` | `ultimoStatus_despacho` | **NÃO** |
| `ultimaSituacaoDataHora` | `ultimoStatus_dataHora` | **NÃO** |
| `ultimoStatusProposicaoDataHora` | `ultimoStatus_dataHora` | **NÃO** |
| `statusProposicao_situacaoDescricao` | _(não existe no CSV)_ | **NÃO** |
| `ultimaAberturaFase` | _(não existe no CSV)_ | **NÃO** |
| `url_inteiro_teor` (legacy) | _(não existe — correto é `urlInteiroTeor`)_ | **NÃO** |

**Consequências imediatas** (bug na aplicação atual):
- `situacao` → sempre `null` para registros CSV (campo Gold populado apenas pela API incremental)
- `despachoAtual` → sempre `null`
- `dataAtualizacao` → sempre `null`

**Colunas do CSV completamente não mapeadas no DTO** (13 campos perdidos):
`descricaoTipo`, `uriOrgaoNumerador`, `uriPropAnterior`, `uriPropPrincipal`, `uriPropPosterior`, `urnFinal`, `ultimoStatus_sequencia`, `ultimoStatus_uriRelator`, `ultimoStatus_idOrgao`, `ultimoStatus_siglaOrgao`, `ultimoStatus_uriOrgao`, `ultimoStatus_regime`, `ultimoStatus_descricaoTramitacao`, `ultimoStatus_idTipoTramitacao`, `ultimoStatus_idSituacao`, `ultimoStatus_apreciacao`, `ultimoStatus_url`

> A refatoração Silver **deve** corrigir este bug ao reescrever o DTO com os 31 nomes de colunas corretos.

### 2.4 Impacto do Desvio

- Dados históricos da fonte são irrecuperáveis uma vez descartados.
- Qualquer novo requisito analítico sobre campos originais exige re-ingestão completa.
- Rastreabilidade entre registro Gold e payload original é fraca (só `id_origem`/`uri_origem`).
- Impossibilidade de auditoria comparativa entre Silver e Gold.
- Bug no DTO da Câmara causa perda silenciosa dos campos `situacao`, `despachoAtual` e `dataAtualizacao` em toda a carga full (CSV).

---

## 3. Arquitetura Proposta — Padrão Medalhão

```
┌─────────────────────────────────────────────────────────────────────┐
│  BRONZE (arquivos)                                                  │
│  data/tmp/camara/{ano}/proposicoes-{ano}.csv                        │
│  (JSON bruto da API em memória — não persiste em arquivo)           │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│  SILVER (banco — fidelidade à fonte)                                │
│                                                                     │
│  schema: silver                                                     │
│  ┌──────────────────────────────┐  ┌───────────────────────────┐   │
│  │ silver_camara_proposicao     │  │ silver_senado_materia      │   │
│  │ silver_camara_tramitacao     │  │ silver_senado_movimentacao │   │
│  └──────────────────────────────┘  └───────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           │  [SilverToGoldTransformer]
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│  GOLD (banco — modelo analítico normalizado)                        │
│                                                                     │
│  schema: public (atual)                                             │
│  ┌────────────────┐  ┌─────────────┐                               │
│  │  proposicao    │  │  tramitacao │                               │
│  └────────────────┘  └─────────────┘                               │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.1 Princípios Adotados

1. **Silver = espelho fiel da fonte**: tipos de dados PostgreSQL escolhidos para máxima compatibilidade; campos com nomes derivados diretamente dos originais (snake_case do campo JSON/CSV).
2. **Gold inalterado semanticamente**: a tabela `proposicao` e a visão analítica não mudam de contrato; apenas a origem dos dados passa a ser Silver → Gold em vez de fonte → Gold.
3. **Deduplicação acontece na Silver**: `content_hash` calculado sobre os campos brutos; a promoção Silver → Gold apenas transforma campos já validados.
4. **Separação por schema PostgreSQL**: tabelas Silver ficam no schema `silver`; tabelas Gold permanecem em `public`. Isso permite permissões granulares e queries cross-schema.
5. **Rastreabilidade bidirecional**: toda linha Gold mantém FK para a linha Silver de origem (`silver_id`), além dos campos `id_origem`/`uri_origem` já existentes.

---

## 4. Modelo de Dados Silver — Câmara (CSV + API)

### 4.1 `silver.camara_proposicao`

Espelha os **31 campos do CSV** mais os **campos extras do endpoint de detalhe** que não existem no CSV.

> **Nota sobre sobreposição**: Os campos `ultimo_status_*` (oriundos do CSV) e `status_*` (oriundos do endpoint de detalhe `/api/v2/proposicoes/{id}`) representam informação semanticamente similar (último status da proposição), mas podem divergir temporalmente. A Silver preserva ambos para manter fidelidade total às duas fontes (CSV bulk vs API detalhe).

```sql
CREATE SCHEMA IF NOT EXISTS silver;

CREATE TABLE silver.camara_proposicao (

    -- ── Controle interno ─────────────────────────────────────────────
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP,
    content_hash    VARCHAR(64),      -- SHA-256 dos campos fonte
    origem_carga    VARCHAR(20)  NOT NULL DEFAULT 'CSV',  -- 'CSV' | 'API'

    -- ── Campos do CSV (colunas originais em snake_case) ───────────────
    camara_id               VARCHAR(20),          -- CSV: "id"
    uri                     VARCHAR(500),
    sigla_tipo              VARCHAR(20),           -- CSV: "siglaTipo"
    numero                  INTEGER,
    ano                     INTEGER,
    cod_tipo                INTEGER,               -- CSV: "codTipo"
    descricao_tipo          VARCHAR(200),          -- CSV: "descricaoTipo"
    ementa                  TEXT,
    ementa_detalhada        TEXT,                  -- CSV: "ementaDetalhada"
    keywords                TEXT,
    data_apresentacao       VARCHAR(50),           -- como vem da fonte, sem parse
    uri_orgao_numerador     VARCHAR(500),
    uri_prop_anterior       VARCHAR(500),
    uri_prop_principal      VARCHAR(500),
    uri_prop_posterior      VARCHAR(500),
    url_inteiro_teor        VARCHAR(1000),
    urn_final               VARCHAR(500),

    -- Último status (prefixo ultimo_status_*)
    ultimo_status_data_hora             VARCHAR(50),
    ultimo_status_sequencia             INTEGER,
    ultimo_status_uri_relator           VARCHAR(500),
    ultimo_status_id_orgao              INTEGER,
    ultimo_status_sigla_orgao          VARCHAR(50),
    ultimo_status_uri_orgao             VARCHAR(500),
    ultimo_status_regime                VARCHAR(200),
    ultimo_status_descricao_tramitacao  TEXT,
    ultimo_status_id_tipo_tramitacao    VARCHAR(20),
    ultimo_status_descricao_situacao    VARCHAR(500),
    ultimo_status_id_situacao           INTEGER,
    ultimo_status_despacho              TEXT,
    ultimo_status_apreciacao            VARCHAR(200),
    ultimo_status_url                   VARCHAR(500),

    -- ── Campos complementares do endpoint de detalhe (API) ───────────
    -- GET /api/v2/proposicoes/{id}  →  campo statusProposicao
    status_data_hora                    VARCHAR(50),
    status_sequencia                    INTEGER,
    status_sigla_orgao                  VARCHAR(50),
    status_uri_orgao                    VARCHAR(500),
    status_uri_ultimo_relator           VARCHAR(500),
    status_regime                       VARCHAR(200),
    status_descricao_tramitacao         TEXT,
    status_cod_tipo_tramitacao          VARCHAR(20),
    status_descricao_situacao           VARCHAR(500),
    status_cod_situacao                 INTEGER,
    status_despacho                     TEXT,
    status_url                          VARCHAR(500),
    status_ambito                       VARCHAR(200),
    status_apreciacao                   VARCHAR(200),

    -- Demais campos do payload de detalhe
    uri_autores                         VARCHAR(500),
    texto                               TEXT,
    justificativa                       TEXT,

    CONSTRAINT pk_silver_camara_proposicao PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_prop       UNIQUE (camara_id),     -- idempotência
    -- ── Flag de controle de promoção Silver→Gold ─────────────────────
    gold_sincronizado    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_silver_camara_job        FOREIGN KEY (etl_job_id)
                                           REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.camara_proposicao IS
    'Silver — proposições da Câmara, fiel ao CSV + complemento da API de detalhe';
COMMENT ON COLUMN silver.camara_proposicao.camara_id IS
    'Chave primária da Câmara (campo "id" do CSV / payload da API)';
COMMENT ON COLUMN silver.camara_proposicao.origem_carga IS
    'Indica se o registro veio de CSV (full-load) ou da API REST (incremental)';
COMMENT ON COLUMN silver.camara_proposicao.content_hash IS
    'SHA-256 dos campos fonte — usado para deduplicação Silver';
COMMENT ON COLUMN silver.camara_proposicao.gold_sincronizado IS
    'Flag de controle: FALSE = pendente promoção para Gold; TRUE = já promovido';
```

### 4.2 `silver.camara_tramitacao`

Espelha o payload de `GET /api/v2/proposicoes/{id}/tramitacoes`.

```sql
CREATE TABLE silver.camara_tramitacao (

    -- ── Controle ──────────────────────────────────────────────────────
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    camara_proposicao_id UUID       NOT NULL,   -- FK para silver.camara_proposicao.id
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP   NOT NULL DEFAULT NOW(),
    content_hash        VARCHAR(64),

    -- ── Campos do payload /tramitacoes ────────────────────────────────
    sequencia                   INTEGER,
    data_hora                   VARCHAR(50),
    sigla_orgao                 VARCHAR(50),
    uri_orgao                   VARCHAR(500),
    uri_ultimo_relator          VARCHAR(500),
    regime                      VARCHAR(200),
    descricao_tramitacao        TEXT,
    cod_tipo_tramitacao         VARCHAR(20),
    descricao_situacao          VARCHAR(500),
    cod_situacao                INTEGER,
    despacho                    TEXT,
    url                         VARCHAR(500),
    ambito                      VARCHAR(200),
    apreciacao                  VARCHAR(200),

    CONSTRAINT pk_silver_camara_tram  PRIMARY KEY (id),
    CONSTRAINT uq_silver_camara_tram  UNIQUE (camara_proposicao_id, sequencia),
    CONSTRAINT fk_silver_camara_tram  FOREIGN KEY (camara_proposicao_id)
                                      REFERENCES silver.camara_proposicao(id)
                                      ON DELETE CASCADE,
    CONSTRAINT fk_silver_camara_tram_job FOREIGN KEY (etl_job_id)
                                      REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.camara_tramitacao IS
    'Silver — tramitações Câmara, fiel ao endpoint /proposicoes/{id}/tramitacoes';
```

---

## 5. Modelo de Dados Silver — Senado (API JSON)

### 5.1 `silver.senado_materia`

Combina campos da **pesquisa/lista** (carga full) com os campos do **endpoint de detalhe** (`/materia/{id}.json`), consultados opcionalmente.

```sql
CREATE TABLE silver.senado_materia (

    -- ── Controle ──────────────────────────────────────────────────────
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    etl_job_id      UUID,
    ingerido_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP,
    content_hash    VARCHAR(64),
    origem_carga    VARCHAR(30)  NOT NULL DEFAULT 'PESQUISA',
                    -- 'PESQUISA' | 'DETALHE' | 'INCREMENTAL'

    -- ── Campos de pesquisa/lista.json (novo schema) ───────────────────
    -- JSON: PesquisaBasicaMateria.Materias.Materia[]
    codigo                      VARCHAR(20)   NOT NULL,  -- "Codigo"
    identificacao_processo      VARCHAR(30),             -- "IdentificacaoProcesso"
    descricao_identificacao     VARCHAR(100),            -- "DescricaoIdentificacao"
    sigla                       VARCHAR(20),             -- "Sigla"
    numero                      VARCHAR(20),             -- "Numero" (com zeros à esq.)
    ano                         INTEGER,                 -- "Ano"
    ementa                      TEXT,                    -- "Ementa"
    autor                       VARCHAR(500),            -- "Autor"
    data                        VARCHAR(30),             -- "Data" (data de apresentação)
    url_detalhe_materia         VARCHAR(500),            -- "UrlDetalheMateria"

    -- ── Campos do endpoint de detalhe (/materia/{id}.json) ───────────
    -- IdentificacaoMateria
    det_sigla_casa_identificacao        VARCHAR(10),
    det_sigla_subtipo                   VARCHAR(20),
    det_descricao_subtipo               VARCHAR(200),
    det_descricao_objetivo_processo     VARCHAR(200),
    det_indicador_tramitando            VARCHAR(10),   -- "Sim" / "Não"

    -- DadosBasicosMateria
    det_indexacao                       TEXT,      -- "IndexacaoMateria"
    det_casa_iniciadora                 VARCHAR(10),
    det_indicador_complementar          VARCHAR(10),
    det_natureza_codigo                 VARCHAR(20),
    det_natureza_nome                   VARCHAR(100),
    det_natureza_descricao              VARCHAR(200),

    -- OrigemMateria
    det_sigla_casa_origem               VARCHAR(10),

    -- Classificacoes (desnormalizado como JSONB — pode ter múltiplas)
    det_classificacoes                  JSONB,

    -- OutrasInformacoes (URLs de serviço — preservado como JSONB)
    det_outras_informacoes              JSONB,

    -- Dados do endpoint /textos/{codigo}.json
    url_texto                           VARCHAR(1000),         -- UrlTexto (inteiro teor)
    data_texto                          VARCHAR(30),           -- DataTexto

    -- ── Flag de controle de promoção Silver→Gold ─────────────────────
    gold_sincronizado    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_silver_senado_materia  PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_materia  UNIQUE (codigo),      -- idempotência
    CONSTRAINT fk_silver_senado_job      FOREIGN KEY (etl_job_id)
                                         REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE  silver.senado_materia IS
    'Silver — matérias do Senado, fiel à API pesquisa/lista + detalhe';
COMMENT ON COLUMN silver.senado_materia.codigo IS
    'Código único da matéria na API do Senado (campo "Codigo")';
COMMENT ON COLUMN silver.senado_materia.numero IS
    'Número com zeros à esquerda, preservado como vem da fonte (ex: "00001")';
COMMENT ON COLUMN silver.senado_materia.det_classificacoes IS
    'Array de classificações temáticas (JSONB) — mantido fiel ao array da API';
```

### 5.2 `silver.senado_movimentacao`

Espelha o payload de `/dadosabertos/materia/{id}/movimentacoes.json`.

```sql
CREATE TABLE silver.senado_movimentacao (

    -- ── Controle ──────────────────────────────────────────────────────
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    senado_materia_id       UUID         NOT NULL,   -- FK para silver.senado_materia
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP    NOT NULL DEFAULT NOW(),
    content_hash            VARCHAR(64),

    -- ── Campos do payload /movimentacoes ─────────────────────────────
    sequencia_movimentacao  VARCHAR(20),
    data_movimentacao       VARCHAR(30),
    identificacao_tramitacao VARCHAR(30),
    descricao_movimentacao  TEXT,
    descricao_situacao      VARCHAR(500),
    despacho                TEXT,
    ambito                  VARCHAR(200),
    sigla_local             VARCHAR(100),
    nome_local              VARCHAR(500),

    CONSTRAINT pk_silver_senado_mov    PRIMARY KEY (id),
    CONSTRAINT uq_silver_senado_mov    UNIQUE (senado_materia_id, sequencia_movimentacao),
    CONSTRAINT fk_silver_senado_mov    FOREIGN KEY (senado_materia_id)
                                       REFERENCES silver.senado_materia(id)
                                       ON DELETE CASCADE,
    CONSTRAINT fk_silver_senado_mov_job FOREIGN KEY (etl_job_id)
                                       REFERENCES public.etl_job_control(id)
);

COMMENT ON TABLE silver.senado_movimentacao IS
    'Silver — movimentações Senado, fiel ao endpoint /movimentacoes';
```

---

## 6. Camada Gold — Alterações de Rastreabilidade

As tabelas `proposicao` e `tramitacao` (schema `public`) mantêm seu contrato atual. As alterações são limitadas à adição de colunas de rastreabilidade (FK) para o Silver:

```sql
-- Migração V13 — rastreabilidade Silver→Gold em proposicao
ALTER TABLE proposicao
    ADD COLUMN silver_camara_id UUID REFERENCES silver.camara_proposicao(id),
    ADD COLUMN silver_senado_id UUID REFERENCES silver.senado_materia(id);

COMMENT ON COLUMN proposicao.silver_camara_id IS
    'FK para silver.camara_proposicao — rastreabilidade bidirecional Silver→Gold';
COMMENT ON COLUMN proposicao.silver_senado_id IS
    'FK para silver.senado_materia — rastreabilidade bidirecional Silver→Gold';

-- Migração V13 — rastreabilidade Silver→Gold em tramitacao
ALTER TABLE tramitacao
    ADD COLUMN silver_camara_tramitacao_id UUID REFERENCES silver.camara_tramitacao(id),
    ADD COLUMN silver_senado_movimentacao_id UUID REFERENCES silver.senado_movimentacao(id);

COMMENT ON COLUMN tramitacao.silver_camara_tramitacao_id IS
    'FK para silver.camara_tramitacao — rastreabilidade Silver→Gold';
COMMENT ON COLUMN tramitacao.silver_senado_movimentacao_id IS
    'FK para silver.senado_movimentacao — rastreabilidade Silver→Gold';
```

Nenhuma coluna existente é removida ou alterada.

---

## 7. Novo Fluxo ETL

```
[Bronze]          [Silver]               [Gold]
                  
CSV download  →  SilverCamaraLoader  →  GoldCamaraPromoter  →  proposicao
                   ↓                       ↓
               silver.camara_proposicao  public.proposicao
               silver.camara_tramitacao  public.tramitacao

API Senado    →  SilverSenadoLoader  →  GoldSenadoPromoter  →  proposicao
                   ↓                       ↓
               silver.senado_materia     public.proposicao
               silver.senado_movimentacao public.tramitacao
```

### 7.1 Etapa Bronze → Silver

- **Câmara CSV**: `CamaraCSVExtractor` continua igual; o destino muda de `ProposicaoLoader` para `SilverCamaraLoader`.
- **Câmara API (incremental)**: `CamaraApiExtractor` continua igual; destino muda para `SilverCamaraLoader`.
- **Senado API**: `SenadoApiExtractor` continua igual; destino muda para `SilverSenadoLoader`.
- Deduplicação Silver: `UNIQUE(camara_id)` e `UNIQUE(codigo)` garantem idempotência via `ON CONFLICT DO UPDATE`.
- `content_hash` calculado sobre campos da fonte (sem campos Gold).

### 7.2 Etapa Silver → Gold

- `SilverToGoldService` lê registros Silver com `gold_sincronizado = FALSE` (flag de controle) ou re-processa todos em carga full.
- Para cada linha Silver, aplica as transformações atuais (normalização de tipo, merge de ementa, etc.) e persiste em `proposicao`/`tramitacao` com FK Silver.
- Deduplicação Gold: mantém o mecanismo atual de `content_hash` sobre campos Gold.

### 7.3 Sequência no Job ETL

```
1. Adquire lock distribuído (etl_lock)
2. Extrai Bronze (CSV / API)
3. Persiste Silver  ← NOVO
4. Promove Silver → Gold ← NOVO (substitui passo atual de loader direto)
5. Libera lock
6. Atualiza etl_job_control
```

---

## 8. Análise de Impacto no Código

### 8.1 Novos Arquivos (criação)

```
src/main/java/br/leg/congresso/etl/
│
├── domain/silver/
│   ├── SilverCamaraProposicao.java      ← @Entity silver.camara_proposicao
│   ├── SilverCamaraTramitacao.java      ← @Entity silver.camara_tramitacao
│   ├── SilverSenadoMateria.java         ← @Entity silver.senado_materia
│   └── SilverSenadoMovimentacao.java    ← @Entity silver.senado_movimentacao
│
├── repository/silver/
│   ├── SilverCamaraProposicaoRepository.java
│   ├── SilverCamaraTramitacaoRepository.java
│   ├── SilverSenadoMateriaRepository.java
│   └── SilverSenadoMovimentacaoRepository.java
│
├── loader/silver/
│   ├── SilverCamaraLoader.java          ← substitui ProposicaoLoader p/ Câmara
│   └── SilverSenadoLoader.java          ← substitui ProposicaoLoader p/ Senado
│
├── promoter/
│   ├── SilverToGoldService.java         ← orquestra promoção Silver→Gold
│   ├── CamaraGoldPromoter.java          ← transforma SilverCamara → Proposicao
│   └── SenadoGoldPromoter.java          ← transforma SilverSenado → Proposicao
│
└── transformer/silver/
    ├── SilverCamaraHashGenerator.java   ← hash sobre campos brutos da Câmara
    └── SilverSenadoHashGenerator.java   ← hash sobre campos brutos do Senado
```

### 8.2 Arquivos Alterados (modificação)

> **Pré-requisito**: antes de iniciar a Silver, o DTO `CamaraProposicaoCSVRow.java` deve ser reescrito para mapear os 31 nomes reais de colunas do CSV (ver §2.3).

| Arquivo | Tipo de mudança | Impacto |
|---------|-----------------|---------|
| `FullLoadOrchestrator.java` | Substitui chamada a `ProposicaoLoader` por `SilverCamaraLoader` + `SilverToGoldService` | Médio |
| `IncrementalLoadOrchestrator.java` | Mesmo padrão | Médio |
| `ProposicaoLoader.java` | Passa a ser invocado apenas pelo `SilverToGoldService`; rename para `GoldProposicaoLoader` | Baixo |
| `CamaraProposicaoCSVRow.java` | **Reescrever completamente**: trocar `ultimaSituacao*` → `ultimoStatus_*`; adicionar os 13 campos CSV não mapeados (ver §2.3). Bug bloqueante. | **Crítico** |
| `CamaraCSVExtractor.java` | Alterar tipo do `Consumer<List<Proposicao>>` para `Consumer<List<SilverCamaraProposicao>>` — ou manter intermediário via DTO e separar mapeamento | Alto |
| `CamaraProposicaoMapper.java` | Adicionar métodos `csvRowToSilver(CamaraProposicaoCSVRow)` e `apiDtoToSilver(CamaraProposicaoDTO)` | Médio |
| `SenadoApiExtractor.java` | Alterar tipo de retorno para `List<SilverSenadoMateria>` | Alto |
| `SenadoMateriaMapper.java` | Renomear/adaptar para `mapToSilver(SenadoMateriaDTO.Materia)` | Médio |
| `DeduplicationService.java` | Separar em `SilverDeduplicationService` (hash bruto) + `GoldDeduplicationService` (hash normalizado) | Médio |
| `ProposicaoTransformer.java` | Separar lógica: Silver recebe passthrough puro; Gold recebe normalização+hash. Atualmente invoca `deduplicationService.enriquecerComHash()` direto em `Proposicao` | Médio |
| `BatchUpsertHelper.java` | Generalizar para suportar upsert Silver (repositórios Silver terão native queries próprias) ou criar `SilverBatchUpsertHelper` | Médio |
| `CamaraProposicaoDTO.java` | Expandir `UltimoStatus` com campos faltantes da API (`codTipoTramitacao`, `codSituacao`, `ambito`, `apreciacao`, `url`, `uriUltimoRelator`) | Médio |
| `Proposicao.java` | Adicionar campos `silver_camara_id` e `silver_senado_id` (FK Silver) | Baixo |
| `Tramitacao.java` | Adicionar campos `silver_camara_tramitacao_id` e `silver_senado_movimentacao_id` (FK Silver) | Baixo |
| `EtlOrchestrator.java` | Coordenar dois estágios (Silver, Gold) em vez de um | Baixo |
| `AdminEtlController.java` | Adicionar endpoints de consulta Silver (status, contagem, amostra) | Baixo |
| `application.yml` | Configurar Flyway para `defaultSchema` + `schemas: public, silver` | Baixo |

### 8.3 Arquivos Sem Alteração

- `ContentHashGenerator.java` — reutilizado pelo `SilverToGoldService` para Gold
- `TipoProposicaoNormalizer.java` — apenas no Gold
- `EtlJobControlService.java` / `EtlLockService.java` — inalterados
- `EtlMetrics.java` — inalterado (reutilizado para métricas Silver)
- `SecurityConfig.java`, `WebClientConfig.java`, `Resilience4jConfig.java`, `ExecutorConfig.java`, `JpaConfig.java` — inalterados
- `AdaptiveRateLimiter.java` — inalterado
- `CamaraFileDownloader.java` — inalterado (download CSV não muda)
- `CamaraTramitacaoDTO.java`, `SenadoMovimentacaoDTO.java`, `SenadoTextoMateriaDTO.java` — inalterados
- Entidades ETL (`EtlJobControl.java`, `EtlFileControl.java`, `EtlErrorLog.java`, `EtlLock.java`) — inalteradas
- Migrations V1–V7 — inalteradas

### 8.4 Estratégia de Mapeamento — Câmara

Atualmente o mapeamento é feito em dois caminhos que convergem direto para `Proposicao` (Gold). Com a refatoração:

```
Caminho Atual:
  CamaraProposicaoCSVRow  →  [CamaraProposicaoMapper]  →  Proposicao (Gold)
  CamaraProposicaoDTO     →  [CamaraProposicaoMapper]  →  Proposicao (Gold)

Caminho Proposto:
  CamaraProposicaoCSVRow  →  [CamaraProposicaoMapper]  →  SilverCamaraProposicao
  CamaraProposicaoDTO     →  [CamaraProposicaoMapper]  →  SilverCamaraProposicao
                               (enriquece silver se já existe)
  SilverCamaraProposicao  →  [CamaraGoldPromoter]       →  Proposicao (Gold)
```

### 8.5 Estratégia de Mapeamento — Senado

```
Caminho Atual:
  SenadoMateriaDTO.Materia  →  [SenadoMateriaMapper]  →  Proposicao (Gold)

Caminho Proposto:
  SenadoMateriaDTO.Materia  →  [SenadoMateriaMapper]  →  SilverSenadoMateria
  SilverSenadoMateria       →  [SenadoGoldPromoter]   →  Proposicao (Gold)
```

---

## 9. Plano de Migrações Flyway

| Versão | Arquivo | Conteúdo |
|--------|---------|----------|
| V8 | `V8__create_silver_schema.sql` | `CREATE SCHEMA silver;` + tabela `silver.camara_proposicao` |
| V9 | `V9__create_silver_camara_tramitacao.sql` | Tabela `silver.camara_tramitacao` |
| V10 | `V10__create_silver_senado_materia.sql` | Tabela `silver.senado_materia` |
| V11 | `V11__create_silver_senado_movimentacao.sql` | Tabela `silver.senado_movimentacao` |
| V12 | `V12__create_silver_indexes.sql` | Índices no schema `silver` (camara_id, codigo, gold_sincronizado) |
| V13 | `V13__add_gold_silver_fk.sql` | Colunas `silver_camara_id` e `silver_senado_id` em `proposicao`; colunas `silver_camara_tramitacao_id` e `silver_senado_movimentacao_id` em `tramitacao` |

---

## 10. Estratégia de Implementação por Fases

### Fase 1 — Hotfix DTO CSV + Infraestrutura Silver

Objetivo: corrigir o bug crítico do DTO e criar tabelas Silver sem alterar a lógica de carga.

- **Corrigir `CamaraProposicaoCSVRow.java`**: reescrever com os 31 `@CsvBindByName` corretos (ver §2.3)
- **Re-executar full-load Câmara** para popular `situacao`, `despachoAtual`, `dataAtualizacao` no Gold
- Criar migrações V8–V13
- Criar entidades JPA Silver (`@Table(schema = "silver", ...)`)
- Criar repositórios Silver
- Adicionar configuração Flyway para `schemas: public, silver`

**Critério de aceite:** bug do DTO corrigido (Gold com campos de status populados); aplicação sobe; Flyway aplica migrations; tabelas Silver existem e vazias.

### Fase 2 — Loader Silver para Câmara

Objetivo: persistir CSV e API da Câmara na Silver antes de ir para Gold.

- Adicionar `csvRowToSilver` e `apiDtoToSilver` em `CamaraProposicaoMapper`
- Expandir `CamaraProposicaoDTO.UltimoStatus` com campos faltantes da API
- Criar `SilverCamaraLoader` com upsert `ON CONFLICT (camara_id) DO UPDATE`
- Criar `SilverCamaraHashGenerator`
- Integrar ao `FullLoadOrchestrator` (Câmara) mantendo chamada Gold em paralelo

**Critério de aceite:** full-load Câmara 2024 → Silver tem 5.419 linhas com todos os 31 campos do CSV populados; Gold continua funcionando normalmente.

### Fase 3 — Loader Silver para Senado

Objetivo: persistir API do Senado na Silver.

- Criar `SilverSenadoLoader`, `SilverSenadoHashGenerator`
- Adaptar `SenadoMateriaMapper` para `mapToSilver`
- Integrar ao `FullLoadOrchestrator` (Senado)

**Critério de aceite:** full-load Senado 2024 → Silver tem 1.431 linhas; Gold inalterado.

### Fase 4 — Promoção Silver → Gold

Objetivo: rerouting completo — Gold passa a ser alimentado exclusivamente pela Silver.

- Criar `CamaraGoldPromoter` e `SenadoGoldPromoter`
- Criar `SilverToGoldService`
- Remover chamada direta de `ProposicaoLoader` nos orchestrators; substituir por Silver → Gold
- Preencher `silver_camara_id` / `silver_senado_id` em `proposicao`

**Critério de aceite:** full-load Câmara + Senado 2024 → Silver e Gold com dados; FK Silver←→Gold consistentes; todos testes passam.

### Fase 5 — Enriquecimento Silver (detalhe Senado + tramitações Câmara)

Objetivo: fechar os campos atualmente não capturados.

- Integrar endpoint `/materia/{id}.json` do Senado na fase de Silver (campos `det_*`)
- Integrar endpoint `/proposicoes/{id}/tramitacoes` da Câmara na Silver (tabela `silver.camara_tramitacao`)
- Integrar `/materia/{id}/movimentacoes.json` do Senado na Silver (tabela `silver.senado_movimentacao`)

**Critério de aceite:** Silver Senado com campos `det_*` preenchidos; tramitações e movimentações persistidas nas tabelas Silver correspondentes.

### Fase 6 — Testes e Validação

- Adicionar testes unitários para os novos mappers Silver
- Adicionar testes unitários para `CamaraProposicaoCSVRow` com os 31 campos corretos
- Adicionar testes de integração verificando integridade Silver x Gold  
- Adicionar endpoint admin: `GET /admin/etl/silver/status` (contagem Silver x Gold por casa/ano)
- Validação final via `curl` comparando API fonte x Silver x Gold

---

## 11. Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Performance — dois writes por ingestão (Silver + Gold) | Média | Médio | Transação única; Silver e Gold no mesmo commit; batch idêntico ao atual |
| Inconsistência Silver x Gold em caso de falha parcial | Baixa | Alto | `SilverToGoldService` dentro de transação; flag `gold_sincronizado` permite reprocessamento isolado do Gold sem re-extração |
| Duplicação de lógica de transformação | Média | Médio | Garantir que toda transformação normalizadora viva **apenas** nos Promoters; mappers Silver devem ser passthrough puros |
| Schema `silver` requer configuração JPA adicional | Baixa | Baixo | Spring JPA suporta `@Table(schema="silver")` nativamente; apenas adicionar `spring.jpa.properties.hibernate.default_schema` em `application-local.yml` |
| Aumento de armazenamento (~2× mais linhas) | Baixa | Baixo | Estimativa: 5.419 Câmara + 1.431 Senado = ~7K linhas extras por ano; custo negligenciável |
| API Senado descontinuada (aviso na resposta) | Alta | Alto | Silver persiste o dado atual do endpoint legado; migração futura para `/dadosabertos/processo` pode ser feita nas fases posteriores sem reprocessar tudo |
| Bug no DTO `CamaraProposicaoCSVRow` (bindings incorretos) | **Confirmado** | **Alto** | Campos `situacao`, `despachoAtual`, `dataAtualizacao` estão `null` no Gold para toda carga full-CSV. **Pré-requisito bloqueante**: reescrever DTO com os 31 nomes corretos antes da Fase 2 |
| Evolução futura do schema CSV da Câmara | Baixa | Médio | O formato se manteve estável de 2001 a 2026 (31 colunas idênticas). Caso a Câmara altere o CSV, a Silver tolera colunas novas via `null` e as colunas extras podem ser adicionadas via migration incremental |

---

## 12. Decisões em Aberto

As seguintes decisões precisam de validação antes da implementação:

1. **Escopo do enriquecimento detalhe Senado na Fase 5**: consultar `/materia/{id}.json` para cada matéria aumenta ~1.431 chamadas por carga Senado 2024. Confirmar se isso é aceitável dado o rate limit (10 req/s → ~3 min extras).

2. **Granularidade da promoção Silver → Gold**: promover **imediatamente** após cada batch Silver (menor latência), ou promover **ao final** de toda a carga Silver (menor overhead). Recomendação: ao final de cada ano processado.

3. **Retenção de dados Silver**: definir política de retenção (manter histórico indefinidamente ou purgar Silver após N dias / após confirmação do Gold). Recomendação inicial: manter indefinidamente — é o ponto de reprodutibilidade.

4. **Preenchimento retroativo**: os 5.419 registros Câmara e 1.431 Senado já em Gold devem ser retroativamente recriados na Silver? Recomendação: sim — re-executar carga full com a nova lógica após implementação da Fase 4.

5. **Novo schema ou mesmo schema**: usar `silver` como schema PostgreSQL separado (proposto) ou prefixo de tabela (`silver_camara_proposicao` em `public`)? Recomendação: schema separado — melhor isolamento de permissões e clareza arquitetural.

6. **Correção do DTO imediata ou junto à Silver?**: corrigir `CamaraProposicaoCSVRow.java` (bindings incorretos) como hotfix independente (beneficia o Gold imediatamente, corrigindo campos `null`) ou fazer a correção apenas durante a implementação da Fase 2 (Silver Câmara). Recomendação: hotfix imediato — os dados Gold atuais estão com campos `situacao`, `despachoAtual` e `dataAtualizacao` vazios para toda a carga CSV.

7. **Enriquecimento Silver Senado textos**: o endpoint `/dadosabertos/materia/textos/{codigo}.json` retorna a URL do inteiro teor. Deve ser consumido na Fase 5 (junto ao detalhe) ou já na Fase 3 (carga Silver inicial)? Recomendação: Fase 3, pois o `SenadoApiExtractor` já possui o método `fetchUrlTextoMateria()`.
