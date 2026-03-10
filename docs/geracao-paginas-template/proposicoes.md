# Proposta Técnica — Geração de Páginas Estáticas de Proposições

> **Data:** 2026-03-09  
> **Escopo:** Gerar páginas HTML estáticas de proposições e matérias legislativas a partir do banco de dados PostgreSQL do projeto `etl-congresso-java`, integrando-as ao site Nuxt SSG em `open-data`.

---

## 1. Contexto e Motivação

### 1.1 Problema Atual

A aplicação `open-data` (Nuxt 4, SSG) gera páginas estáticas via `nuxt build`, mas o conteúdo das páginas de detalhe (`/proposicoes/[casa]-[id]`) é carregado **no browser** chamando as APIs públicas da Câmara e do Senado. Isso gera três problemas:

1. **SEO prejudicado** — crawlers (Googlebot, Bingbot) recebem um HTML esqueleto sem dados. O conteúdo depende de JavaScript para ser renderizado, e os dados chegam de APIs externas que podem estar lentas ou fora do ar no momento da indexação.
2. **Dependência de APIs externas** — se a API da Câmara ou do Senado estiver indisponível/lenta, o usuário vê um estado de loading ou erro.
3. **Build lento e frágil** — o prerender atual tenta renderizar ~284 mil rotas fazendo chamadas HTTP para as APIs legislativas, sujeito a rate-limiting (429), timeouts e falhas intermitentes.

### 1.2 Solução Proposta

Gerar **páginas HTML estáticas completas** diretamente a partir do banco de dados PostgreSQL do `etl-congresso-java` (camada Gold + Silver), usando **Thymeleaf** como engine de templates Java. As páginas geradas serão servidas pelo Nuxt/Nitro como conteúdo estático pré-construído, eliminando chamadas a APIs externas tanto no build quanto no runtime.

---

## 2. Arquitetura da Solução

```
┌──────────────────────────────────────────────────────────────────────┐
│                       ETL Congresso (Java)                          │
│                                                                      │
│   PostgreSQL ──► PageGenerator ──► Thymeleaf ──► HTML estático      │
│   (Gold+Silver)   (Spring Bean)     (templates)   (open-data/       │
│                                                    .output/public/) │
└─────────────────────────────┬────────────────────────────────────────┘
                              │  arquivos .html gerados
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      open-data (Nuxt 4 SSG)                         │
│                                                                      │
│   public/proposicoes/        ◄── HTML estático com dados completos  │
│     camara-12345/index.html      + JSON-LD Schema.org               │
│     senado-67890/index.html      + meta tags SEO                    │
│                                                                      │
│   app/pages/proposicoes/     ◄── SPA fallback (hidratação Vue)      │
│     [casa]-[id].vue              para interatividade dinâmica        │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.1 Fluxo de Geração

```
1. ETL completo (ingestão)
   └── Bronze → Silver → Gold  (fluxo existente)

2. Geração de páginas (novo módulo)
   ├── Consulta Gold (proposicao + tramitacao) via JPA
   ├── Enriquece com Silver (autores, temas, keywords, ementa detalhada)
   ├── Monta DTO de renderização (ProposicaoPageDTO)
   ├── Renderiza templates Thymeleaf → HTML
   ├── Grava em: open-data/public/proposicoes/{casa}-{id}/index.html
   └── Gera sitemap-proposicoes.xml auxiliar

3. Build Nuxt (sem chamadas a APIs externas)
   └── Copia public/ → .output/public/  (comportamento padrão Nuxt)
```

---

## 3. Biblioteca de Templates: Thymeleaf

### 3.1 Justificativa

| Critério                             | Thymeleaf                   | Freemarker | Pebble   |
| ------------------------------------ | --------------------------- | ---------- | -------- |
| Integração Spring Boot               | Nativa (starter)            | Média      | Manual   |
| Uso offline (sem servlet)            | `TemplateEngine` standalone | Sim        | Sim      |
| HTML natural (pode abrir no browser) | Sim (natural templates)     | Não        | Não      |
| Facilidade de manutenção             | Alta — HTML válido          | Média      | Média    |
| Maturidade/comunidade                | Muito alta                  | Alta       | Moderada |

**Thymeleaf** é a escolha ideal porque:

- Integra-se nativamente com Spring Boot (já usa starter-web).
- Templates são HTML válido — podem ser visualizados diretamente no browser durante o desenvolvimento.
- Possui modo _standalone_ (`TemplateEngine` + `FileTemplateResolver`) para gerar HTML sem precisar de um web server ativo.
- Suporte nativo a fragmentos (`th:fragment`, `th:replace`) para reuso de cabeçalho, rodapé e meta tags.

### 3.2 Dependência Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

---

## 4. Estrutura do Módulo de Geração

### 4.1 Pacotes Java

```
src/main/java/br/leg/congresso/etl/
├── pagegen/                            # Novo módulo
│   ├── PageGeneratorService.java       # Orquestrador principal
│   ├── ProposicaoPageDTO.java          # DTO de renderização
│   ├── ProposicaoPageAssembler.java    # Gold+Silver → DTO
│   ├── ThymeleafRenderer.java          # Renderiza template → HTML
│   └── SitemapGenerator.java           # Gera sitemap auxiliar
```

### 4.2 Templates Thymeleaf

```
src/main/resources/templates/pages/
├── proposicao.html          # Template principal (detalhe)
├── fragments/
│   ├── head-seo.html        # Meta tags, OG, JSON-LD
│   ├── header.html          # Cabeçalho com breadcrumb
│   ├── info-tab.html        # Aba Informações Gerais
│   ├── autores-tab.html     # Aba Autores
│   ├── tramitacao-tab.html  # Aba Tramitação (timeline)
│   └── footer.html          # Rodapé
└── sitemap-proposicoes.xml  # Template de sitemap
```

### 4.3 DTO de Renderização

```java
@Builder
public class ProposicaoPageDTO {
    // Identificação
    private String casa;              // "camara" | "senado"
    private String casaLabel;         // "Câmara dos Deputados" | "Senado Federal"
    private String idOriginal;        // ID na API de origem
    private String siglaTipo;
    private String descricaoTipo;
    private Integer numero;
    private Integer ano;

    // Conteúdo
    private String ementa;
    private String ementaDetalhada;
    private String situacao;
    private String situacaoTramitacao; // "tramitando" | "encerrada"
    private String dataApresentacao;   // formatada "dd/MM/yyyy"
    private String orgaoAtual;
    private String regime;
    private String urlInteiroTeor;
    private List<String> keywords;
    private List<String> temas;

    // Autores
    private String autoriaResumo;
    private List<AutorDTO> autores;

    // Tramitação
    private List<TramitacaoDTO> tramitacoes;

    // SEO
    private String canonicalUrl;
    private String seoTitle;
    private String seoDescription;
    private String schemaOrgJson;      // JSON-LD Legislation pré-serializado
    private String dataPublicacao;     // ISO 8601 para Schema.org
    private String dataAtualizacao;    // ISO 8601

    // Controle
    private String geradoEm;          // timestamp da geração
}
```

### 4.4 Assembler (Gold + Silver → DTO)

O `ProposicaoPageAssembler` consulta:

| Dado                            | Fonte (Gold)                  | Fonte (Silver)                                                                |
| ------------------------------- | ----------------------------- | ----------------------------------------------------------------------------- |
| Identificação, ementa, situação | `proposicao`                  | —                                                                             |
| Tramitações                     | `tramitacao`                  | —                                                                             |
| Ementa detalhada                | —                             | `silver.camara_proposicao.ementa_detalhada` ou `silver.senado_materia.ementa` |
| Autores                         | —                             | `silver.camara_proposicao_autor` ou `silver.senado_autoria`                   |
| Temas/Classificações            | —                             | `silver.camara_proposicao_tema` ou `silver.senado_materia.det_classificacoes` |
| Keywords                        | `proposicao.keywords`         | —                                                                             |
| URL inteiro teor                | `proposicao.url_inteiro_teor` | —                                                                             |
| Órgão atual, regime             | —                             | `silver.camara_proposicao.ultimo_status_*` ou `silver.senado_materia.det_*`   |

### 4.5 Renderizador Thymeleaf

```java
@Service
public class ThymeleafRenderer {

    private final TemplateEngine engine;

    public ThymeleafRenderer() {
        var resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/pages/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    public String render(String templateName, Map<String, Object> variables) {
        var context = new Context(Locale.of("pt", "BR"));
        context.setVariables(variables);
        return engine.process(templateName, context);
    }
}
```

---

## 5. Template HTML — Estrutura SEO

### 5.1 Cabeçalho SEO (`head-seo.html`)

```html
<head th:fragment="seo(page)">
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <title th:text="${page.seoTitle}">PL 123/2024 — Câmara dos Deputados</title>
  <meta name="description" th:content="${page.seoDescription}" />
  <link rel="canonical" th:href="${page.canonicalUrl}" />

  <!-- Open Graph -->
  <meta property="og:type" content="article" />
  <meta property="og:title" th:content="${page.seoTitle}" />
  <meta property="og:description" th:content="${page.seoDescription}" />
  <meta property="og:url" th:content="${page.canonicalUrl}" />
  <meta
    property="og:image"
    content="https://www.translegis.com.br/og-image.png"
  />
  <meta property="og:site_name" content="Transparência Legislativa" />
  <meta property="og:locale" content="pt_BR" />

  <!-- Twitter Card -->
  <meta name="twitter:card" content="summary" />
  <meta name="twitter:title" th:content="${page.seoTitle}" />
  <meta name="twitter:description" th:content="${page.seoDescription}" />

  <!-- Schema.org JSON-LD -->
  <script type="application/ld+json" th:utext="${page.schemaOrgJson}"></script>

  <!-- Referência ao CSS do Nuxt (será resolvido em runtime) -->
  <link rel="stylesheet" href="/_nuxt/entry.css" />
</head>
```

### 5.2 Schema.org — `Legislation`

```json
{
  "@context": "https://schema.org",
  "@type": "Legislation",
  "name": "PL 1234/2024",
  "alternateName": "Altera a Lei nº ...",
  "description": "Ementa completa da proposição ...",
  "legislationIdentifier": "PL 1234/2024",
  "legislationType": "Projeto de Lei",
  "datePublished": "2024-03-15",
  "dateModified": "2024-06-20T14:30:00",
  "legislationPassedBy": {
    "@type": "GovernmentOrganization",
    "name": "Câmara dos Deputados"
  },
  "publisher": {
    "@type": "GovernmentOrganization",
    "name": "Câmara dos Deputados",
    "url": "https://www.camara.leg.br"
  },
  "url": "https://www.translegis.com.br/proposicoes/camara-1234",
  "about": ["Saúde", "Educação"],
  "keywords": "saúde, educação, SUS"
}
```

### 5.3 Conteúdo Principal

O corpo HTML reproduz a mesma estrutura visual da página Vue (`[casa]-[id].vue`), com as seguintes seções renderizadas estaticamente:

1. **Breadcrumb** — `Início > Proposições > PL 1234/2024`
2. **Cabeçalho** — sigla/número/ano, badge da casa, badge da situação, ementa
3. **Metadados** — autoria, data de apresentação, órgão atual, regime
4. **Keywords** — badges com palavras-chave
5. **Informações Gerais** — tabela de campos
6. **Autores** — lista com links para perfis de parlamentares
7. **Tramitação** — timeline vertical (mesma estrutura do componente Vue)

> As abas "Votações", "Documentos", "Temas detalhados" e "Relacionadas" são carregadas dinamicamente pela aplicação Vue após hidratação. O HTML estático fornece o conteúdo principal indexável.

---

## 6. Integração com a Aplicação Nuxt (`open-data`)

### 6.1 Estratégia: Páginas Estáticas no `public/`

O Nuxt copia automaticamente o conteúdo de `public/` para `.output/public/` durante o build. As páginas geradas pelo Java serão gravadas diretamente em:

```
open-data/public/proposicoes/
  camara-12345/index.html
  camara-67890/index.html
  senado-11111/index.html
  ...
```

### 6.2 Comportamento de Coexistência

O Nuxt 4 com `routeRules: { '/proposicoes/**': { prerender: true } }` tentará gerar estas páginas também via prerender. Para evitar conflito e **priorizar os HTMLs gerados pelo Java**:

**Opção A — Desativar prerender para proposições (recomendada)**

```typescript
// nuxt.config.ts
routeRules: {
  '/proposicoes': { prerender: true },           // listagem continua prerender Nuxt
  '/proposicoes/**': { prerender: false },        // detalhe: HTML do Java
}
```

O HTML estático gerado pelo Java já estará em `public/proposicoes/{casa}-{id}/index.html`. Quando o Nuxt faz o build com `preset: 'static'`, ele copia tudo de `public/` para `.output/public/`, incluindo estes HTMLs.

**Opção B — Modo híbrido com fallback SPA**

Mantém o prerender ativo, mas as rotas já cobertas pelo Java (que já terão `index.html` em `.output/public/`) são automaticamente puladas pelo hook incremental existente (`prerender:routes`). Rotas sem HTML pré-gerado recebem fallback SPA (`200.html`).

### 6.3 Hidratação Vue

Quando o usuário acessa uma página estática diretamente (link, bookmark, crawler), recebe o HTML completo com dados. Ao navegar dentro do SPA (cliques internos), o Vue Router assume e carrega dados via API normalmente.

Para que as páginas estáticas geradas pelo Java possam ser enriquecidas pela aplicação Vue após carregamento:

1. O HTML gerado inclui um `<div id="__static_data__">` com metadados em atributos `data-*`
2. O componente Vue `[casa]-[id].vue` detecta se há dados estáticos pré-carregados e prioriza-os
3. Abas secundárias (Votações, Documentos, Relacionadas) continuam carregando via API sob demanda

```html
<!-- Incluído no HTML gerado pelo Java -->
<script
  type="application/json"
  id="__proposicao_data__"
  th:utext="${proposicaoJson}"
></script>
```

```typescript
// Em [casa]-[id].vue — leitura do dado pré-embutido
const staticData = import.meta.client
  ? JSON.parse(
      document.getElementById("__proposicao_data__")?.textContent || "null",
    )
  : null;
```

### 6.4 CSS e Estilização

As páginas geradas pelo Java precisam usar as mesmas classes CSS do Nuxt UI / Tailwind. Duas abordagens:

**Abordagem recomendada — Classes utilitárias inline:**
O template Thymeleaf usa as mesmas classes Tailwind presentes no componente Vue. O CSS utilitário do Tailwind já estará no bundle do Nuxt (`/_nuxt/entry.css`), que é referenciado no `<head>` do HTML gerado.

**Alternativa — CSS embarcado:**
Gerar um CSS mínimo embarcado (`<style>`) no próprio HTML para garantir renderização mesmo sem o bundle do Nuxt.

---

## 7. Endpoint de Administração

### 7.1 Novo Endpoint

```
POST /admin/etl/pages/generate
  ?casa=camara|senado|all     (default: all)
  &ano=2024                   (opcional: filtrar por ano)
  &incremental=true|false     (default: false)
  &outputDir=../open-data/public  (default: configurável)
```

**Resposta:** `202 Accepted` com job assíncrono (mesmo padrão dos endpoints de ingestão existentes).

### 7.2 Configuração (application.yml)

```yaml
etl:
  pagegen:
    enabled: true
    output-dir: ${PAGEGEN_OUTPUT_DIR:../open-data/public}
    batch-size: 500
    concurrent-writers: 4
    recreate-existing: false # incremental por padrão
```

### 7.3 Integração com o Pipeline ETL

Opcionalmente, a geração de páginas pode ser executada **automaticamente** ao final de cada ingestão:

```
Ingestão (full/incremental)
  └── Bronze → Silver → Gold
       └── (novo) Gold → Geração de páginas HTML
            └── Apenas proposições com `atualizado_em > última_geração`
```

---

## 8. Geração de Sitemap Auxiliar

O `SitemapGenerator` produz um arquivo XML complementar:

```
open-data/public/sitemap-proposicoes.xml
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://www.translegis.com.br/proposicoes/camara-12345</loc>
    <lastmod>2024-06-20</lastmod>
    <changefreq>weekly</changefreq>
    <priority>0.7</priority>
  </url>
  <!-- ... -->
</urlset>
```

Este sitemap pode ser referenciado no `nuxt.config.ts` como sitemap adicional, ou incluído no `robots.txt`.

---

## 9. Otimizações SEO

### 9.1 Boas Práticas Implementadas

| Prática                     | Implementação                                                  |
| --------------------------- | -------------------------------------------------------------- |
| **Título único por página** | `{SiglaTipo} {Numero}/{Ano} — {Casa}`                          |
| **Meta description**        | Ementa truncada em 155 caracteres                              |
| **Canonical URL**           | `https://www.translegis.com.br/proposicoes/{casa}-{id}`        |
| **JSON-LD Legislation**     | Schema.org com type, identifier, datePublished, publisher      |
| **Open Graph**              | og:type article, og:title, og:description, og:url              |
| **Conteúdo textual rico**   | Ementa, ementa detalhada, autores, tramitação — tudo indexável |
| **Heading hierarchy**       | H1 (sigla/número), H2 (seções), H3 (subseções)                 |
| **Breadcrumbs**             | Estruturados com Schema.org `BreadcrumbList`                   |
| **Sitemap XML**             | Gerado automaticamente com `lastmod`                           |
| **Internal linking**        | Links para parlamentares e proposições relacionadas            |

### 9.2 Schema.org BreadcrumbList

```json
{
  "@context": "https://schema.org",
  "@type": "BreadcrumbList",
  "itemListElement": [
    {
      "@type": "ListItem",
      "position": 1,
      "name": "Início",
      "item": "https://www.translegis.com.br/"
    },
    {
      "@type": "ListItem",
      "position": 2,
      "name": "Proposições",
      "item": "https://www.translegis.com.br/proposicoes"
    },
    { "@type": "ListItem", "position": 3, "name": "PL 1234/2024" }
  ]
}
```

---

## 10. Script de Conveniência

Adicionar ao `Makefile`:

```makefile
pages-generate: ## Gera páginas estáticas de proposições (requer banco de dados)
	@echo "Gerando páginas estáticas..."
	curl -s -X POST "http://localhost:8080/admin/etl/pages/generate" \
		-H "Authorization: Basic $$(echo -n '$(ADMIN_USERNAME):$(ADMIN_PASSWORD)' | base64)" \
		| python3 -m json.tool

pages-generate-ano: ## Gera páginas de um ano específico (ANO=2024)
	curl -s -X POST "http://localhost:8080/admin/etl/pages/generate?ano=$(ANO)" \
		-H "Authorization: Basic $$(echo -n '$(ADMIN_USERNAME):$(ADMIN_PASSWORD)' | base64)" \
		| python3 -m json.tool
```

Adicionar ao `scripts/ingest.sh` como etapa opcional:

```bash
# Após ingestão completa, gerar páginas estáticas
if [ "$GENERATE_PAGES" = "true" ]; then
    curl -X POST "$BASE_URL/admin/etl/pages/generate?incremental=true" \
         -H "Authorization: Basic $AUTH"
fi
```

---

## 11. Volumetria e Performance

### 11.1 Estimativa de Volume

| Casa      | Proposições (1988–2026) | Tamanho estimado/página  | Total estimado |
| --------- | ----------------------- | ------------------------ | -------------- |
| Câmara    | ~250.000                | ~15 KB (HTML minificado) | ~3,5 GB        |
| Senado    | ~35.000                 | ~12 KB                   | ~0,4 GB        |
| **Total** | **~285.000**            | —                        | **~3,9 GB**    |

### 11.2 Performance de Geração

- **Batch processing**: Lê do banco em lotes de 500 proposições
- **Escrita paralela**: 4 writers concorrentes (configurável)
- **Estimativa**: ~2.000 páginas/segundo → ~285.000 páginas em ~2,5 minutos
- **Modo incremental**: Gera apenas proposições com `atualizado_em > última geração` → segundos

---

## 12. Plano de Implementação

### Fase 1 — Infraestrutura (1–2 dias)

- [ ] Adicionar dependência `spring-boot-starter-thymeleaf` ao `pom.xml`
- [ ] Criar pacote `pagegen` com classes base
- [ ] Implementar `ThymeleafRenderer` standalone
- [ ] Criar template `proposicao.html` com fragmentos SEO

### Fase 2 — Assembler e Geração (2–3 dias)

- [ ] Implementar `ProposicaoPageAssembler` (Gold + Silver → DTO)
- [ ] Implementar `PageGeneratorService` com batch processing
- [ ] Implementar `SitemapGenerator`
- [ ] Adicionar endpoint `POST /admin/etl/pages/generate`

### Fase 3 — Integração Nuxt (1 dia)

- [ ] Ajustar `routeRules` no `nuxt.config.ts`
- [ ] Testar coexistência HTML estático + SPA Vue
- [ ] Adicionar detecção de dados pré-embutidos em `[casa]-[id].vue`
- [ ] Referenciar `sitemap-proposicoes.xml` no config do sitemap

### Fase 4 — Validação e Deploy (1 dia)

- [ ] Testar indexação com Google Search Console (inspeção de URL)
- [ ] Validar Schema.org com validator.schema.org
- [ ] Validar meta tags com Open Graph debugger
- [ ] Testes unitários do assembler e renderer
- [ ] Documentar no README e no Makefile

---

## 13. Riscos e Mitigações

| Risco                                                   | Mitigação                                                                                         |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| Volume de disco (~4 GB)                                 | Gzip/Brotli no CloudFront reduz para ~400 MB transferidos; considerar gerar apenas últimos N anos |
| CSS desatualizado no HTML estático                      | Regenerar páginas no pipeline de deploy; não embarcar CSS inline — referenciar bundle Nuxt        |
| Dados inconsistentes (Gold vs Silver)                   | Assembler faz join explícito e valida campos obrigatórios antes de renderizar                     |
| Conflito de rotas Nuxt vs HTML estático                 | Opção A (recomendada) desativa prerender para `/proposicoes/**`                                   |
| Templates Thymeleaf ficarem defasados em relação ao Vue | Manter template Thymeleaf simplificado (conteúdo indexável) — a experiência rica fica no Vue      |

---

## 14. Decisões em Aberto

1. **Escopo temporal**: Gerar páginas de todas as proposições desde 1988, ou apenas da legislatura atual (2023–2027)?
2. **Opção A vs B de integração Nuxt**: Desativar prerender (mais simples) ou manter modo híbrido incremental?
3. **Embarcar JSON completo** no HTML para hidratação Vue, ou deixar o Vue buscar dados via API ao navegar internamente?
4. **Tailwind CSS**: Referenciar bundle Nuxt (acoplamento de build) ou gerar CSS standalone mínimo?
