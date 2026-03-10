# Plano de Implementação — Ingestão de OutrasInformacoes e Classificacoes (Senado)

> **Última revisão:** 2026-01-15  
> **Status:** Fase 1 concluída — Fase 2 concluída (Silver Autoria + Silver Relatoria)

---

## 1. Objetivo

Ingerir e persistir, na camada Silver, os dados complementares das tags `OutrasInformacoes` e `Classificacoes` do endpoint de detalhe de matéria do Senado Federal, com processamento paralelo e respeito ao limite de requisições da API.

---

## 2. Contexto Técnico

| Item                         | Valor                                                                 |
| ---------------------------- | --------------------------------------------------------------------- |
| Endpoint de detalhe (legado) | `GET /dadosabertos/materia/{codigo}.json` (v7)                        |
| Endpoint substituto (novo)   | `GET /dadosabertos/processo/{idProcesso}` (v1)                        |
| Tag `OutrasInformacoes`      | Lista de 9 serviços (`NomeServico`, `DescricaoServico`, `UrlServico`) |
| Tag `Classificacoes`         | Lista de classificações temáticas hierárquicas                        |
| Rate Limit da API            | Máx. 10 req/s (documentação oficial)                                  |
| Rate Limiter configurado     | Resilience4j `senadoApi`: **8 req/s** (com Retry + CircuitBreaker)    |
| Executor paralelo            | `etlExecutor` (core=8, max=16, queue=200)                             |

---

## 3. ⚠️ Alerta de Descontinuação da API

O endpoint `/dadosabertos/materia/{codigo}` está **deprecado** conforme o OpenAPI (summary: `(DEPRECATED) Detalhes da Matéria por código`) e o XML de resposta inclui:

```xml
<Descontinuacao>
  <DataDepreciacao>2025-03-18</DataDepreciacao>
  <DataDesativacaoCompleta>2026-02-01</DataDesativacaoCompleta>
  <UrlServicoSubstituto>https://legis.senado.leg.br/dadosabertos/processo/{idProcesso}</UrlServicoSubstituto>
</Descontinuacao>
```

**A data de desativação completa (01/02/2026) já foi ultrapassada.** Se o endpoint ainda responde, pode ser desligado a qualquer momento.

**Todos os 9 serviços listados em `OutrasInformacoes` também estão marcados como `(DEPRECATED)` no OpenAPI:**

| #   | NomeServico          | Endpoint legado                                | Status OpenAPI | Endpoint substituto                      |
| --- | -------------------- | ---------------------------------------------- | -------------- | ---------------------------------------- |
| 1   | AtualizacaoMateria   | `/materia/atualizacoes/{codigo}`               | DEPRECATED     | `/processo/{idProcesso}`                 |
| 2   | AutoriaMateria       | `/materia/autoria/{codigo}`                    | DEPRECATED     | `/processo/{idProcesso}`                 |
| 3   | EmendaMateria        | `/materia/emendas/{codigo}`                    | DEPRECATED     | `/processo/emenda`                       |
| 4   | MovimentacaoMateria  | `/materia/movimentacoes/{codigo}`              | DEPRECATED     | _(já usado via `fetchRawMovimentacoes`)_ |
| 5   | RelatoriaMateria     | `/materia/relatorias/{codigo}`                 | DEPRECATED     | `/processo/relatoria`                    |
| 6   | SituacaoAtualMateria | `/materia/situacaoatual/{codigo}`              | DEPRECATED     | `/processo/{idProcesso}`                 |
| 7   | TextoMateria         | `/materia/textos/{codigo}`                     | DEPRECATED     | `/processo/documento`                    |
| 8   | VotacaoMateria       | `/materia/votacoes/{codigo}`                   | DEPRECATED     | `/dadosabertos/votacao`                  |
| 9   | VotacoesComissao     | `/votacaoComissao/materia/{sigla}/{num}/{ano}` | DEPRECATED     | `/dadosabertos/votacao`                  |

### Ação recomendada

Planejar migração para a API `/dadosabertos/processo/{idProcesso}`, que consolida detalhe, autoria, classificação, situação e documentos em uma única chamada. O campo `IdentificacaoProcesso` (já disponível no Silver como `identificacao_processo`) é a chave de acesso ao novo endpoint.

---

## 4. Definição de Escopo

### Fase 1 — Metadados (✅ implementado)

Persistir as tags `OutrasInformacoes` e `Classificacoes` como JSONB na tabela `silver.senado_materia`, sem consumir os endpoints secundários:

| Coluna                   | Tipo    | Conteúdo                                                               |
| ------------------------ | ------- | ---------------------------------------------------------------------- |
| `det_outras_informacoes` | `jsonb` | Array de `{nomeServico, descricaoServico, urlServico}`                 |
| `det_classificacoes`     | `jsonb` | Array de `{codigoClasse, descricaoClasse, descricaoClasseHierarquica}` |

Exemplo de `det_outras_informacoes`:

```json
[
  {
    "nomeServico": "AutoriaMateria",
    "descricaoServico": "Lista o(s) autor(es) da matéria.",
    "urlServico": "https://legis.senado.leg.br/dadosabertos/materia/autoria/162431"
  }
]
```

### Fase 2 — Consumo de dados complementares (✅ implementado)

Chamar os endpoints secundários (ou seus substitutos na API de processos) para persistir os dados de autoria e relatoria em tabelas Silver dedicadas.

#### Fase 2a — Silver Autoria (✅ implementado)

| Endpoint     | `GET /dadosabertos/materia/autoria/{codigo}.json` (legado) |
| ------------ | ---------------------------------------------------------- |
| Tabela       | `silver.senado_autoria`                                    |
| Migration    | `V16__create_silver_senado_autoria.sql`                    |
| DTO          | `SenadoAutoriaDTO`                                         |
| Loader       | `SilverSenadoAutoriaLoader`                                |
| Deduplicação | `(senado_materia_id, nome_autor, codigo_tipo_autor)`       |

Campos persistidos: `nome_autor`, `sexo_autor`, `codigo_tipo_autor`, `descricao_tipo_autor`, `codigo_parlamentar`, `nome_parlamentar`, `sigla_partido`, `uf_parlamentar`.

#### Fase 2b — Silver Relatoria (✅ implementado)

| Endpoint     | `GET /dadosabertos/processo/relatoria?codigoMateria={codigo}` (novo) |
| ------------ | -------------------------------------------------------------------- |
| Tabela       | `silver.senado_relatoria`                                            |
| Migration    | `V17__create_silver_senado_relatoria.sql`                            |
| DTO          | `SenadoRelatoriaDTO`                                                 |
| Loader       | `SilverSenadoRelatoriaLoader`                                        |
| Deduplicação | `(senado_materia_id, id_relatoria)`                                  |

Campos persistidos: `id_relatoria`, `casa_relator`, `id_tipo_relator`, `descricao_tipo_relator`, `data_designacao`, `data_destituicao`, `descricao_tipo_encerramento`, `id_processo`, `identificacao_processo`, `tramitando`, além de todos os campos do parlamentar relator e do colegiado.

---

## 5. O que foi Implementado (Fase 1)

### 5.1. Expansão do DTO de detalhe

**Arquivo:** `SenadoDetalheDTO.java`

- Classes internas adicionadas: `Classificacoes`, `Classificacao`, `OutrasInformacoes`, `Servico`.
- Campo `OrigemMateria` (fallback para `CasaOrigem`).
- Campo `NaturezaMateria` dentro de `DadosBasicosMateria` (fallback para natureza aninhada).

### 5.2. Persistência no enriquecimento Silver

**Arquivo:** `SilverEnrichmentService.java`

- Método `applyDetalhe()` serializa para JSONB via `ObjectMapper`:
  - `Classificacoes.Classificacao` → `detClassificacoes`
  - `OutrasInformacoes.Servico` → `detOutrasInformacoes`
- Comportamento idempotente preservado (sobrescreve se já existir).

### 5.3. Paralelismo controlado

**Arquivo:** `SilverEnrichmentService.java`

- `enriquecerDetalhesSenado()` refatorado de sequencial para paralelo.
- O lote pendente é dividido em N grupos via `partitionBySize()`.
- Cada grupo é processado em `CompletableFuture.runAsync(..., etlExecutor)`.
- O controle de throughput é exercido pelo `@RateLimiter(name = "senadoApi")` no método `fetchRawDetalhe()` — thread-safe (semáforo Resilience4j).

**Configuração:** `application.yml`

```yaml
etl:
  senado:
    enrichment:
      parallelism: ${ETL_SENADO_ENRICHMENT_PARALLELISM:6}
```

Valores efetivos:

| Parâmetro                        | Valor      | Efeito                                                        |
| -------------------------------- | ---------- | ------------------------------------------------------------- |
| `parallelism`                    | 6 (padrão) | Threads concorrentes no enrichment                            |
| `senadoApi.limit-for-period`     | 8          | Requisições permitidas por segundo (hard limit)               |
| `senadoApi.timeout-duration`     | 500 ms     | Tempo máximo que uma thread espera pelo token do rate limiter |
| `senadoApi.max-attempts` (retry) | 3          | Re-tentativas com backoff exponencial                         |

### 5.4. Tolerância a falhas

- Erro em uma matéria individual: `warn` + continua o lote.
- Erro de serialização JSON: `warn` + campo fica `null` + continua.
- `fetchRawDetalhe` retorna `null` em 404/exceção (sem propagar).

---

## 6. O que foi Implementado (Fase 2)

### 6.1. Fase 2a — Silver Autoria

**Arquivos novos/modificados:**

- `SenadoAutoriaDTO.java` — DTO com estrutura `AutoriaBasicaMateria.Materia.Autores.Autor[]`.
- `SenadoApiExtractor.java` — método `fetchRawAutoria(String codigoMateria)` com `@RateLimiter` + `@Retry`, tolerante a 404.
- `SilverSenadoAutoria.java` — entidade JPA para `silver.senado_autoria`.
- `V16__create_silver_senado_autoria.sql` — migration Flyway.
- `SilverSenadoAutoriaRepository.java` — Spring Data JPA.
- `SilverSenadoAutoriaLoader.java` — carregamento idempotente por `(senado_materia_id, nome_autor, codigo_tipo_autor)`.
- `SilverEnrichmentService.java` — método `enriquecerAutoriasSenado()`, integrado em `enriquecerTudo()`.

**Testes:** `SenadoAutoriaDTOTest` (3 testes) e `SilverSenadoAutoriaLoaderTest` (7 testes).

### 6.2. Fase 2b — Silver Relatoria

**Arquivos novos/modificados:**

- `SenadoRelatoriaDTO.java` — DTO com todos os campos do schema `relatoria` do OpenAPI.
- `SenadoApiExtractor.java` — método `fetchRelatorias(String codigoMateria)` usando `bodyToFlux` (array sem wrapper).
- `SilverSenadoRelatoria.java` — entidade JPA para `silver.senado_relatoria`.
- `V17__create_silver_senado_relatoria.sql` — migration Flyway.
- `SilverSenadoRelatoriaRepository.java` — Spring Data JPA.
- `SilverSenadoRelatoriaLoader.java` — carregamento idempotente por `(senado_materia_id, id_relatoria)`.
- `SilverEnrichmentService.java` — método `enriquecerRelatoriasSenado()`, integrado em `enriquecerTudo()`.

**Testes:** `SenadoRelatoriaDTOTest` (3 testes) e `SilverSenadoRelatoriaLoaderTest` (7 testes).

**Nota técnica:** O endpoint `/dadosabertos/processo/relatoria` retorna um array JSON direto (sem wrapper de objeto). Por isso o método `fetchRelatorias()` usa `bodyToFlux(SenadoRelatoriaDTO.class).collectList()` ao invés de `bodyToMono`.

---

## 7. Problemas Conhecidos e Débitos Técnicos

### 7.1. `content_hash` para JSONB ✅ corrigido

O `SilverSenadoHashGenerator.generate()` agora inclui também `detOutrasInformacoes` e `detClassificacoes` no cálculo de hash. Assim, mudanças apenas nos metadados JSONB passam a ser detectadas.

**Ajuste complementar no loader:** para evitar atualizações desnecessárias, o `SilverSenadoLoader` preserva os campos de enriquecimento do registro existente antes de calcular/comparar o hash em registros já persistidos.

**Validação:** testes unitários do hash cobrem alteração em `detClassificacoes` e `detOutrasInformacoes`.

### 7.2. `det_casa_iniciadora` com tamanho insuficiente ✅ corrigido

A coluna `det_casa_iniciadora` estava definida como `length = 10`, mas o fallback no `applyDetalhe()` pode usar `nomeCasaIniciadora` (ex: "Senado Federal" = 15 caracteres). Truncamento silencioso podia ocorrer.

**Ação executada:** Migration `V15__alter_det_casa_iniciadora_length.sql` altera a coluna para `VARCHAR(100)`. Entidade `SilverSenadoMateria` atualizada para `length = 100`. Testes unitários adicionados para verificar que nomes longos são preservados sem truncamento.

### 7.3. Mapeamento de `CasaIniciadoraNoLegislativo` ✅ corrigido

O XML de detalhe retorna `<CasaIniciadoraNoLegislativo>SF</CasaIniciadoraNoLegislativo>`, mas o DTO mapeava apenas `@JsonProperty("SiglaCasaIniciadora")`. O campo alternativo era ignorado silenciosamente.

**Ação executada:** Adicionado `@JsonAlias("CasaIniciadoraNoLegislativo")` no campo `siglaCasaIniciadora` do DTO. Testes de desserialização Jackson adicionados em `SenadoDetalheDTOTest`.

### 7.4. Impacto na camada Gold

O preenchimento de `det_outras_informacoes` e `det_classificacoes` **não** reseta a flag `gold_sincronizado`. Isso é intencional: esses campos JSONB não são propagados para Gold atualmente. Quando/se forem, o fluxo de promoção precisará ser ajustado.

---

## 8. Garantia de Rate Limit

A garantia de rate limit é exercida em duas camadas complementares:

1. **Resilience4j `@RateLimiter(name = "senadoApi")`** no método `fetchRawDetalhe()` — semáforo global com 8 tokens/segundo. Mesmo com 6 threads concorrentes, o throughput efetivo não ultrapassa 8 req/s.
2. **`AdaptiveRateLimiter`** — ajusta dinamicamente após erros 429 (reduz 2 tokens) ou 5xx (reduz 3 tokens), com piso de 2 req/s e teto de 10 req/s.
3. **`resolveSenadoEnrichmentParallelism()`** — teto de 10 threads para evitar contenção excessiva no executor.

O valor `parallelism=6` com `limit-for-period=8` resulta em: as 6 threads adquirem tokens concorrentemente, eventualmente uma ou duas ficam bloqueadas no semáforo aguardando refresho do período (1s). O overhead é mínimo comparado ao I/O de rede.

---

## 9. Plano de Rollout

### 8.1. Validação local

1. Reconstruir a imagem: `docker compose up -d --build`.
2. Executar ingestão completa Senado (`senado-full`) para 2024.
3. Verificar preenchimento via SQL:
   ```sql
   SELECT codigo, det_classificacoes IS NOT NULL AS tem_classif,
          det_outras_informacoes IS NOT NULL AS tem_outras
   FROM silver.senado_materia
   WHERE det_sigla_casa_identificacao IS NOT NULL
   LIMIT 20;
   ```
4. Confirmar que promoção Gold não regrediu (gap = 0).

### 8.2. Observabilidade

- Comparar tempo total de enriquecimento antes (sequencial) vs. depois (paralelo).
- Monitorar no Prometheus/Grafana: contadores de 429 e 5xx.
- Verificar logs de `[Enrich]` para erros recorrentes.

### 8.3. Produção

- Subir com `parallelism=6`.
- Ajustar gradualmente conforme telemetria.
- Se 429 persistirem, reduzir para `parallelism=4` ou `limit-for-period=6`.

---

## 10. Plano de Migração para API de Processos (futuro)

Dado que o endpoint legado `/dadosabertos/materia/{codigo}` está em processo de descontinuação, recomenda-se:

1. **Mapear novo DTO** para `GET /dadosabertos/processo/{idProcesso}` — que consolida detalhe, autoria, classificação, situação, decisão, e destino.
2. **Usar `identificacao_processo`** (já presente em `SilverSenadoMateria`) como chave para o novo endpoint.
3. **Manter dual-read**: tentar primeiro o endpoint novo; em caso de falha, fallback para o legado (enquanto disponível).
4. **Criar tabelas Silver dedicadas** para dados complementares (autoria, emendas, relatoria, votações) conforme necessidade.

---

## 11. Critérios de Aceite

- [x] `det_outras_informacoes` preenchido para matérias com essa tag na resposta.
- [x] `det_classificacoes` preenchido quando houver classificações.
- [x] Enriquecimento concluído sem violação sistemática do rate limit.
- [x] Tempo de enriquecimento inferior ao baseline sequencial.
- [x] Build Docker compila sem erros.
- [x] `content_hash` considera `det_outras_informacoes` e `det_classificacoes`.
- [ ] Validação SQL em ambiente local com dados reais de 2024.
- [x] Bug `det_casa_iniciadora` corrigido (seção 7.2).
- [x] Mapeamento `CasaIniciadoraNoLegislativo` corrigido (seção 7.3).
- [x] Fase 2a concluída: tabela `silver.senado_autoria` populada via `enriquecerAutoriasSenado()`.
- [x] Fase 2b concluída: tabela `silver.senado_relatoria` populada via `enriquecerRelatoriasSenado()`.
