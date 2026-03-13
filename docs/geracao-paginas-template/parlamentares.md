# Proposta Técnica — Geração de Páginas Estáticas de Parlamentares

> **Data:** 2026-03-12
> **Escopo:** Gerar páginas HTML estáticas de perfis de parlamentares (Deputados Federais e Senadores) a partir do banco de dados PostgreSQL do projeto `etl-congresso-java`, integrando-as ao site Nuxt SSG em `open-data`.

---

## 1. Status de Implementação

### 1.1 O que já foi implementado

A funcionalidade de geração de páginas de parlamentares está **parcialmente implementada**. A tabela abaixo resume o estado atual:

| Componente | Status | Localização |
|---|---|---|
| Controller REST (`/admin/etl/pages/parlamentares`) | ✅ Implementado | `AdminPageGenParlamentaresController.java` |
| Serviço de geração | ✅ Implementado | `ParlamentaresPageGeneratorService.java` |
| DTO `DeputadoPageDTO` | ✅ Implementado | `pagegen/dto/DeputadoPageDTO.java` |
| DTO `SenadorPageDTO` | ✅ Implementado | `pagegen/dto/SenadorPageDTO.java` |
| Template Thymeleaf — deputado | ✅ Implementado | `templates/pages/parlamentares/deputado.html` |
| Template Thymeleaf — senador | ✅ Implementado | `templates/pages/parlamentares/senador.html` |
| Suporte no `scripts/ingest.sh` (`pages-generate --parlamentares`) | ✅ Implementado | `scripts/ingest.sh` |
| Compatibilidade de rotas com o frontend Nuxt (`/parlamentares/{casa}-{id}`) | ❌ **Incompatível no estado atual** | `ParlamentaresPageGeneratorService.java` |
| Sitemap `sitemap-parlamentares.xml` | ❌ **Pendente** | — |
| Índice JSON paginado (`parlamentares/indice/data/`) | ❌ **Pendente** | — |
| DTO `ParlamentarIndexItemDTO` | ❌ **Pendente** | — |
| Página Nuxt `/parlamentares/indice/` | ❌ **Pendente** | — |
| Schema.org JSON-LD (`Person`) nos templates | ✅ Implementado | `ParlamentaresPageGeneratorService.java` (inline no DTO) |
| Testes unitários `ParlamentaresPageGeneratorService` | ✅ Implementado | `ParlamentaresPageGeneratorServiceTest.java` (11 testes) |

### 1.2 O que o serviço atual já faz

O `ParlamentaresPageGeneratorService` percorre em lotes a tabela `silver.camara_deputado` e `silver.senado_senador`, monta os DTOs e renderiza os templates Thymeleaf, gravando os HTMLs em:

```
open-data/public/parlamentares/deputados/{camaraId}/index.html
open-data/public/parlamentares/senadores/{codigoSenador}/index.html
```

Esse núcleo está implementado, mas há um bloqueio funcional relevante: o frontend Nuxt já usa rotas no formato ` /parlamentares/{casa}-{id} `, por exemplo ` /parlamentares/camara-204554 ` e ` /parlamentares/senado-5529 `. Os links internos já existentes no app seguem esse padrão, enquanto o gerador Java escreve HTMLs em ` /parlamentares/deputados/{id} ` e ` /parlamentares/senadores/{id} `. Isso significa que a geração atual não está integrada ponta a ponta com o roteamento real do frontend.

Portanto, a conclusão correta é: **a geração de páginas de perfil existe, mas a funcionalidade ainda não está totalmente implementada de forma utilizável no site atual**.

O endpoint admin e o script de ingestão já funcionam. A execução é feita via:

```bash
bash scripts/ingest.sh --env container --mode pages-generate --parlamentares
```

---

## 2. Contexto e Motivação

### 2.1 Problema Atual

A aplicação `open-data` (Nuxt 4, SSG) exibe perfis de parlamentares na rota `/parlamentares/[casa]-[id]` carregando dados **no browser** via APIs públicas da Câmara e do Senado. Isso gera os mesmos problemas já identificados para proposições:

1. **SEO prejudicado** — crawlers recebem HTML esqueleto sem dados. O conteúdo de perfil (nome, partido, UF, foto, biografia) não é indexado pelos motores de busca.
2. **Dependência de APIs externas** — instabilidades nas APIs legislativas resultam em tela de loading para o usuário.
3. **Build Nuxt frágil** — o prerender de rotas de parlamentares exige chamadas HTTP para APIs sujeitas a rate-limiting.

### 2.2 Solução Proposta (componentes pendentes)

Completar a implementação já existente com:

1. **Correção do padrão de rotas e paths gerados** — alinhar o Java ao formato já usado pelo frontend: ` /parlamentares/camara-{id} ` e ` /parlamentares/senado-{id} `.
2. **Geração de índice paginado JSON** — dados resumidos de todos os parlamentares para a página de índice ` /parlamentares/indice/ `, que já está linkada no footer.
3. **Sitemap XML** — `sitemap-parlamentares.xml` para indexação por crawlers.
4. **Schema.org `Person`** — JSON-LD embutido nos templates para enriquecimento semântico.
5. **Página Nuxt `/parlamentares/indice/`** — consome os JSONs gerados pelo Java, exibindo lista paginada de parlamentares.
6. **Testes unitários** — cobertura do serviço `ParlamentaresPageGeneratorService`.

---

## 3. Arquitetura da Solução Completa

```
┌──────────────────────────────────────────────────────────────────────┐
│                       ETL Congresso (Java)                          │
│                                                                      │
│  PostgreSQL ──► ParlamentaresPageGeneratorService ──► Thymeleaf     │
│  (Silver)        (Spring Bean)                        ──► HTML      │
│                                                                      │
│  silver.camara_deputado ──► DeputadoPageDTO ──► deputado.html       │
│  silver.senado_senador  ──► SenadorPageDTO  ──► senador.html        │
│                                                                      │
│  [PENDENTE] ──► ParlamentarIndexItemDTO ──► page-N.json + meta.json │
│  [PENDENTE] ──► SitemapParlamentaresGenerator ──► sitemap-parl.xml  │
└─────────────────────────────┬────────────────────────────────────────┘
                              │  arquivos gerados
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      open-data (Nuxt 4 SSG)                         │
│                                                                      │
│  public/parlamentares/camara-{id}/index.html     ❌ alvo correto    │
│  public/parlamentares/senado-{id}/index.html     ❌ alvo correto    │
│  public/parlamentares/deputados/{id}/index.html  ⚠️ gerado hoje     │
│  public/parlamentares/senadores/{id}/index.html  ⚠️ gerado hoje     │
│  public/parlamentares/indice/data/page-N.json    ❌ pendente        │
│  public/parlamentares/indice/data/meta.json      ❌ pendente        │
│  public/sitemap-parlamentares.xml                ❌ pendente        │
│                                                                      │
│  app/pages/parlamentares/[casa]-[id].vue  ← SPA fallback existente  │
│  app/pages/parlamentares/indice/index.vue ❌ pendente               │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.1 Fluxo de Geração

```
1. Ingestão Silver (já implementada)
   └── silver.camara_deputado  +  silver.senado_senador  populadas

2. Geração de páginas (parcialmente implementada)
  ├── [⚠️] Hoje: deputado.html → public/parlamentares/deputados/{id}/
  ├── [⚠️] Hoje: senador.html  → public/parlamentares/senadores/{id}/
  ├── [❌] Correção necessária → public/parlamentares/camara-{id}/index.html
  ├── [❌] Correção necessária → public/parlamentares/senado-{id}/index.html
  ├── [❌] Índice JSON paginado → public/parlamentares/indice/data/page-{n}.json
  ├── [❌] meta.json do índice  → public/parlamentares/indice/data/meta.json
  └── [❌] Sitemap XML          → public/sitemap-parlamentares.xml

3. Build Nuxt (static preset)
   └── copia public/ → .output/public/ (comportamento padrão Nuxt)
```

---

## 4. Componentes Pendentes de Implementação

### 4.1 DTO de Índice — `ParlamentarIndexItemDTO`

Item resumido para listas paginadas, análogo ao `ProposicaoIndexItemDTO`:

```java
/**
 * Item resumido de um parlamentar para uso na página-índice estática.
 *
 * @param slugId       identificador slug, ex: {@code camara-2430}
 * @param nome         nome parlamentar
 * @param partido      sigla do partido
 * @param uf           sigla da UF
 * @param casa         "Câmara dos Deputados" | "Senado Federal"
 * @param urlFoto      URL da foto oficial (pode ser null)
 * @param participacao "Titular" | "Suplente" (senadores) ou null (deputados)
 * @param url          caminho relativo, ex: {@code /parlamentares/camara-2430}
 */
public record ParlamentarIndexItemDTO(
        String slugId,
        String nome,
        String partido,
        String uf,
        String casa,
        String urlFoto,
        String participacao,
        String url) {
}
```

**Localização:** `src/main/java/br/leg/congresso/etl/pagegen/dto/ParlamentarIndexItemDTO.java`

---

### 4.2 Geração do Índice JSON no Serviço

Adicionar ao `ParlamentaresPageGeneratorService` um método `generateIndice(Path base)` análogo ao `generateIndice()` do `PageGeneratorService`:

```java
/**
 * Gera os arquivos JSON paginados do índice de parlamentares em
 * {@code base/parlamentares/indice/data/}.
 *
 * Estrutura de saída:
 *   page-0.json  → primeiros PAGE_SIZE parlamentares
 *   page-1.json  → próximos PAGE_SIZE
 *   meta.json    → { totalDeputados, totalSenadores, totalPaginas, pageSize, geradoEm }
 */
private void generateIndice(Path base) {
    // 1. Consulta deputados e senadores paginados
    // 2. Serializa cada lote para page-N.json
    // 3. Grava meta.json com totais
}
```

O método `generateAll()` deve invocar `generateIndice(base)` após gerar os HTMLs individuais.

**Tamanho de página recomendado:** 100 itens por `page-N.json` (balanceia tamanho do arquivo com número de requisições no frontend).

**Ordenação:** Alfabética por nome parlamentar, deputados e senadores intercalados com campo `casa` para diferenciação.

**Observação importante:** o campo `url` deve seguir exatamente o padrão já usado pelos componentes Vue existentes, por exemplo ` /parlamentares/camara-204554 ` e ` /parlamentares/senado-5529 `.

**Localização dos arquivos gerados:**
```
open-data/public/parlamentares/indice/data/page-0.json
open-data/public/parlamentares/indice/data/page-1.json
...
open-data/public/parlamentares/indice/data/meta.json
```

---

### 4.3 Geração de Sitemap — `SitemapParlamentaresGenerator`

Novo componente para gerar `sitemap-parlamentares.xml`, análogo ao `SitemapGenerator`:

```java
/**
 * Gera o arquivo {@code sitemap-parlamentares.xml} em {@code base/}.
 *
 * <p>
 * Formato: Sitemap 0.9, UTF-8. URLs apontam para
 * {@code /parlamentares/camara-{id}} e {@code /parlamentares/senado-{id}}.
 * Prioridade: deputados = 0.8, senadores = 0.8.
 * Frequência: monthly (dados parlamentares mudam com menor frequência).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SitemapParlamentaresGenerator {

    private static final String BASE_URL = "https://www.translegis.com.br";

    private final SilverCamaraDeputadoRepository deputadoRepository;
    private final SilverSenadoSenadorRepository senadorRepository;

    public void generate(Path base) { ... }
}
```

O `ParlamentaresPageGeneratorService.generateAll()` deve invocar `sitemapGenerator.generate(base)` ao final.

**Localização:** `src/main/java/br/leg/congresso/etl/pagegen/SitemapParlamentaresGenerator.java`

---

### 4.4 Schema.org JSON-LD (`Person`) nos Templates

Adicionar bloco de dados estruturados nos `<head>` dos templates `deputado.html` e `senador.html`, análogo ao `Legislation` já existente em `proposicao.html`:

```html
<!-- JSON-LD Schema.org Person -->
<script type="application/ld+json" th:inline="javascript">
{
  "@context": "https://schema.org",
  "@type": "Person",
  "name": /*[[${page.nomeParlamentar}]]*/ "",
  "jobTitle": "Deputado(a) Federal",
  "affiliation": {
    "@type": "Organization",
    "name": /*[[${page.partido}]]*/ ""
  },
  "image": /*[[${page.urlFoto}]]*/ null,
  "url": /*[[${page.canonicalUrl}]]*/ "",
  "sameAs": /*[[${page.redesSociais}]]*/ []
}
</script>
```

Para senadores, incluir `"memberOf"` apontando para o Senado Federal.

---

### 4.5 Página Nuxt `/parlamentares/indice/`

Criar a página Vue que consome os JSONs gerados pelo Java, seguindo o padrão de `proposicoes/indice/index.vue`:

**Arquivo:** `open-data/app/pages/parlamentares/indice/index.vue`

**Comportamento:**
- Consome `GET /parlamentares/indice/data/meta.json` para obter o total de páginas.
- Consome `GET /parlamentares/indice/data/page-{n}.json` para listar os parlamentares paginados.
- Suporta filtros client-side por: nome (busca textual), partido, UF, casa (deputados/senadores).
- Exibe cards com foto, nome, partido/UF e link para o perfil completo.
- SEO: título "Índice de Parlamentares", meta description, canonical.

**Rota associada:** `[...slug].vue` para subpáginas da paginação (ex: `/parlamentares/indice/pagina-2/`).

**Estrutura de arquivos:**
```
open-data/app/pages/parlamentares/
├── index.vue              ← busca unificada (já existe)
├── [casa]-[id].vue        ← perfil dinâmico (já existe)
└── indice/
    ├── index.vue          ← ❌ PENDENTE — lista estática paginada
    └── [...slug].vue      ← ❌ PENDENTE — paginação por slug
```

---

### 4.6 Testes Unitários

Criar teste análogo ao `PageGeneratorServiceTest.java`:

**Arquivo:** `src/test/java/br/leg/congresso/etl/pagegen/ParlamentaresPageGeneratorServiceTest.java`

**Casos de teste:**
- `generateAll_comDeputadosESenadores_geraArquivosHtml()` — verifica que HTMLs são gravados nos caminhos corretos.
- `generateAll_semDados_retornaZeroEPulaGeracao()` — banco vazio, sem erros.
- `toDeputadoDTO_mapeiaCorretamenteCampos()` — valida mapeamento Silver → DTO.
- `toSenadorDTO_mapeiaCorretamenteCampos()` — valida mapeamento Silver → DTO.
- `generateAll_invocaSitemap()` — verifica que `SitemapParlamentaresGenerator.generate()` é chamado.

---

## 5. Estrutura dos Arquivos Gerados

### 5.1 Páginas HTML de Perfil (já geradas)

```
open-data/public/parlamentares/
├── camara-204554/index.html     ← Deputado(a) ID 204554
├── camara-204521/index.html
├── senado-5529/index.html     ← Senador(a) código 5529
├── senado-5011/index.html
└── ...
```

> **Estado atual do código:** hoje o Java ainda grava em subpastas `deputados/` e `senadores/`. A estrutura acima representa o **alvo correto** para compatibilidade com o frontend existente.

### 5.2 Índice JSON Paginado (pendente)

```
open-data/public/parlamentares/indice/data/
├── meta.json
│   {
│     "totalDeputados": 513,
│     "totalSenadores": 81,
│     "totalRegistros": 594,
│     "totalPaginas": 6,
│     "pageSize": 100,
│     "geradoEm": "2026-03-12T10:00:00"
│   }
├── page-0.json
│   [
│     { "slugId": "camara-204554", "nome": "...", "partido": "PT",
│       "uf": "SP", "casa": "Câmara dos Deputados",
│       "urlFoto": "https://...", "participacao": null,
│       "url": "/parlamentares/camara-204554" },
│     ...
│   ]
├── page-1.json
└── ...
```

### 5.3 Sitemap (pendente)

```
open-data/public/sitemap-parlamentares.xml
  <?xml version="1.0" encoding="UTF-8"?>
  <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <url>
      <loc>https://www.translegis.com.br/parlamentares/camara-204554</loc>
      <changefreq>monthly</changefreq>
      <priority>0.8</priority>
    </url>
    ...
  </urlset>
```

---

## 6. Integração com a Aplicação Nuxt

### 6.1 Estratégia: Páginas Estáticas no `public/`

O Nuxt copia automaticamente `public/` → `.output/public/` no build. Para coexistir corretamente com o frontend já existente, as páginas de perfil geradas pelo Java devem estar em `public/parlamentares/camara-{id}/index.html` e `public/parlamentares/senado-{id}/index.html`, pois esse é o padrão já consumido pelos componentes e páginas do app.

A página de índice Nuxt (`/parlamentares/indice/`) faz fetch dos JSONs gerados pelo Java via `$fetch('/parlamentares/indice/data/meta.json')` — sem chamada a APIs externas.

### 6.2 Coexistência com o Roteamento Nuxt

| Rota | Servida por | Observação |
|------|-------------|------------|
| `/parlamentares/` | Vue SPA (index.vue) | Busca unificada via API |
| `/parlamentares/[casa]-[id]` | HTML estático gerado pelo Java | Só funciona quando o Java gerar paths no mesmo padrão da rota |
| `/parlamentares/indice/` | Vue SPA (indice/index.vue) — pendente | Consome JSON estáticos |
| `/parlamentares/indice/data/*.json` | Arquivo estático | Gerado pelo Java |

### 6.3 Configuração `nuxt.config.ts`

Para evitar conflito entre as páginas estáticas Java e o prerender Nuxt nas rotas de perfil:

```typescript
// nuxt.config.ts
routeRules: {
  // Perfis individuais: HTML estático será gerado no mesmo padrão da rota atual
  '/parlamentares/**': { prerender: false },
  // Índice: gerado pelo Nuxt consumindo JSON estáticos
  '/parlamentares/indice/**': { prerender: true },
  '/parlamentares': { prerender: true },
}
```

### 6.4 Link no Footer

O `LandingFooter.vue` já possui o link para o índice:

```typescript
{ label: 'Índice de Parlamentares', to: '/parlamentares/indice/' }
```

Quando a página `indice/index.vue` for implementada, este link passará a funcionar automaticamente.

---

## 7. Ajustes nos Templates Existentes

### 7.1 Melhorias no `deputado.html`

Os seguintes itens estão ausentes no template atual e devem ser adicionados:

| Item | Justificativa |
|------|---------------|
| JSON-LD `Person` | SEO semântico — necessário para rich results no Google |
| Link para o índice no breadcrumb | UX — retorno ao `/parlamentares/indice/` |
| Favicon e referência ao CSS do Nuxt (`/_nuxt/entry.css`) | Visual consistente com o restante do site |
| `<data>` embutido para hidratação Vue (`__static_data__`) | Permite que o componente Vue evite re-fetch |

**Exemplo de link para Nuxt CSS no `<head>`:**

```html
<!-- Referência ao CSS gerado pelo Nuxt (path do build) -->
<link
  rel="stylesheet"
  href="/_nuxt/entry.css"
  crossorigin="anonymous"
/>
<link rel="icon" type="image/svg+xml" href="/favicon.svg" />
```

> **Nota:** O path `/_nuxt/entry.css` muda a cada build do Nuxt (hash no filename). Para produção, o ideal é usar um CSS minificado gerado separadamente e versionado, ou usar a abordagem de CSS inline embutido no template — garantindo renderização correta mesmo sem o bundle Nuxt.

### 7.2 Melhorias no `senador.html`

Mesmos itens listados para `deputado.html`, mais:

- Campo `legislatura` no breadcrumb secundário.
- Badge de participação (`Titular` / `Suplente`) já está implementado mas pode ser melhorado visualmente.

---

## 8. Endpoint de Administração (estado atual)

O controller já expõe:

```
POST /admin/etl/pages/parlamentares/generate
  → Dispara geração assíncrona de todas páginas (deputados + senadores)
  → 202 Accepted | 409 Conflict (se já em andamento)

GET /admin/etl/pages/parlamentares/status
  → { "emAndamento": true|false, "status": "em_andamento"|"ocioso" }
```

### 8.1 Melhorias sugeridas no Controller

Adicionar ao response do `POST /generate` os totais gerados (atualmente logados mas não retornados ao cliente):

```java
// Após geração concluída, expor via status endpoint:
Map.of(
    "status", "concluido",
    "totalDeputados", totais[0],
    "totalSenadores", totais[1],
    "totalPaginas", totalPaginas,
    "duracaoMs", duracaoMs
)
```

---

## 9. Sequência de Implementação Recomendada

Dado que o núcleo (controller + service + templates HTML) já está implementado, os itens pendentes devem ser implementados na seguinte ordem:

| # | Tarefa | Complexidade | Impacto |
|---|--------|--------------|---------|
| 1 | Corrigir paths e `canonicalUrl` do Java para `camara-{id}` / `senado-{id}` | Média | Remove o bloqueio principal de integração |
| 2 | Criar `ParlamentarIndexItemDTO` | Baixa | Base para o índice |
| 3 | Adicionar `generateIndice()` ao service | Média | Habilita a página de índice |
| 4 | Criar `SitemapParlamentaresGenerator` | Baixa | SEO — crawlers |
| 5 | Invocar sitemap e índice em `generateAll()` | Baixa | Integra os novos geradores |
| 6 | Adicionar JSON-LD `Person` aos templates | Baixa | SEO semântico |
| 7 | Criar `parlamentares/indice/index.vue` | Média | Frontend — índice navegável |
| 8 | Criar `parlamentares/indice/[...slug].vue` | Baixa | Paginação URL-based |
| 9 | Criar testes unitários do service | Média | Qualidade e regressão |

---

## 10. Padrões a Seguir

A implementação deve seguir os mesmos padrões já estabelecidos para proposições:

| Aspecto | Padrão (Proposições) | Equivalente (Parlamentares) |
|---------|---------------------|----------------------------|
| Service | `PageGeneratorService` | `ParlamentaresPageGeneratorService` |
| DTO perfil | `ProposicaoPageDTO` | `DeputadoPageDTO` / `SenadorPageDTO` |
| DTO índice | `ProposicaoIndexItemDTO` | `ParlamentarIndexItemDTO` (pendente) |
| Template HTML | `proposicao.html` | `parlamentares/deputado.html` / `senador.html` |
| Template índice | `proposicoes-indice.html` | não se aplica (JSON puro) |
| Sitemap | `SitemapGenerator` | `SitemapParlamentaresGenerator` (pendente) |
| Controller | `AdminPageGenController` | `AdminPageGenParlamentaresController` |
| Testes | `PageGeneratorServiceTest` | `ParlamentaresPageGeneratorServiceTest` (pendente) |
| Script modo | `pages-generate [--ano=AAAA] [--proposicoes]` | `pages-generate --parlamentares` |
| Output HTML | `public/proposicoes/{casa}-{id}/` | `public/parlamentares/{casa}-{id}/` |
| Output índice | `public/proposicoes/indice/data/` | `public/parlamentares/indice/data/` |
| Output sitemap | `public/sitemap-proposicoes.xml` | `public/sitemap-parlamentares.xml` |

---

## 11. Modelo de Contato — Email de Deputados (Regra Operacional)

> **Data da descoberta:** 2026-03-12
> **Referência:** Investigação do campo `det_status_email` zerado após enriquecimento completo.

### 11.1 Resumo

A API da Câmara (`GET /api/v2/deputados/{id}`) retorna **sistematicamente `null`** para o campo `ultimoStatus.email` em todos os 7.878 deputados testados (legislaturas 1 a 57). O email de contato efetivo é fornecido exclusivamente via `ultimoStatus.gabinete.email`.

Isso não é um bug do ETL — é o comportamento da API-fonte.

### 11.2 Mapeamento de Campos

| Campo da API | Coluna Silver | Valor observado |
|---|---|---|
| `ultimoStatus.email` | `det_status_email` | Sempre `null` (100% dos registros) |
| `ultimoStatus.gabinete.email` | `det_gabinete_email` | Preenchido para deputados em Exercício (513 de 526 na legislatura 58) |

### 11.3 Casos sem email (13 deputados em Exercício)

Existem 13 deputados com `detStatusSituacao = 'Exercício'` que não possuem email em **nenhum** dos dois campos. Confirmado via chamada direta à API para cada um — a fonte não fornece o dado. São lacunas do dado de origem, não falhas de ingestão.

IDs confirmados: 73470, 73475, 73487, 73532, 73669, 73789, 73888, 73896, 74064, 74192, 74199, 74233, 74826.

### 11.4 Implementação no Código

- **Entidade:** `SilverCamaraDeputado.getContatoEmail()` centraliza o fallback: retorna `detStatusEmail` se não-vazio, senão `detGabineteEmail` se não-vazio, senão `null`.
- **Geração de páginas:** `ParlamentaresPageGeneratorService` usa `getContatoEmail()` para montar o DTO e o JSON-LD `Person`.
- **Métricas operacionais:** O endpoint `GET /admin/etl/silver/status` expõe:
  - `camaraDeputadosTotal` — total de registros na tabela Silver
  - `camaraDeputadosPendentesEnriquecimento` — deputados ainda não enriquecidos (detStatusId = null)
  - `camaraDeputadosComContatoEmail` — deputados com pelo menos um email de contato
  - `camaraDeputadosEmExercicioSemContatoEmail` — deputados em Exercício sem nenhum email (lacuna da fonte)

### 11.5 Regra de Monitoramento

- **`camaraDeputadosPendentesEnriquecimento > 0`** indica enriquecimento incompleto (ação necessária).
- **`camaraDeputadosEmExercicioSemContatoEmail > 0`** é esperado (~13 casos na legislatura 58) — não indica falha do ETL.
- A ausência de `det_status_email` preenchido é **comportamento normal** da API e não deve gerar alertas.
