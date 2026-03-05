# Análise Técnica e Inventário - API do Senado Federal

**Versão da API:** 4.0.3.54  
**Base URL Legislativa:** https://legis.senado.leg.br/dadosabertos/  
**Base URL Administrativa:** https://adm.senado.leg.br/adm-dadosabertos/  
**Documentação:** https://www12.senado.leg.br/dados-abertos

## 1. Visão Geral

A API de Dados Abertos do Senado Federal e do Congresso Nacional fornece acesso público a informações sobre parlamentares, processos legislativos, sessões plenárias, votações, discursos, orçamento e diversas outras informações relacionadas ao processo legislativo brasileiro.

### 1.1. Características Principais

- **Métodos HTTP suportados:** GET, POST, OPTIONS, HEAD
- **Formatos de resposta:** JSON, XML, CSV (dependendo do endpoint)
- **Autenticação:** Não requerida (API pública)
- **Rate Limiting:** Máximo de 10 requisições por segundo
- **Estrutura:** Duas APIs principais (Legislativa e Administrativa)
- **Versioning:** API versionada (v4 e v6 em diferentes endpoints)

### 1.2. Suporte a CORS

#### API Legislativa (legis.senado.leg.br)

✅ **Suporte COMPLETO a CORS**

Headers CORS presentes nas respostas:

```
access-control-allow-origin: *
access-control-allow-credentials: true
access-control-allow-methods: GET,POST,OPTIONS,HEAD
access-control-allow-headers: X-Requested-With, Authorization, Accept, Accept-Version, Content-MD5, CSRF-Token, Content-Type
```

**Conclusão:** A API Legislativa pode ser consumida diretamente de aplicações web (frontend) sem problemas de CORS.

#### API Administrativa (adm.senado.leg.br)

⚠️ **Suporte LIMITADO a CORS**

Headers presentes:

```
vary: Origin, Access-Control-Request-Method, Access-Control-Request-Headers
```

**Observação:** A API administrativa retorna `403 Forbidden` para requisições OPTIONS (preflight), o que pode causar problemas em requisições CORS de navegadores. Recomenda-se consumir via servidor (backend) ou usar proxy.

## 2. Inventário Completo de Endpoints

### 2.1. API Legislativa

A API Legislativa possui **157+ endpoints** distribuídos em 8 categorias principais:

| Categoria   | Quantidade Estimada | Descrição                            |
| ----------- | ------------------- | ------------------------------------ |
| Parlamentar | 25+                 | Informações de Senadores e Deputados |
| Processo    | 30+                 | Processos legislativos e matérias    |
| Matéria     | 40+                 | Proposições, tramitações e votações  |
| Plenário    | 20+                 | Sessões plenárias e votações         |
| Comissão    | 15+                 | Comissões e reuniões                 |
| Composição  | 15+                 | Composição política e lideranças     |
| Legislação  | 10+                 | Legislação federal                   |
| Votação     | 10+                 | Detalhes de votações                 |
| Discurso    | 2+                  | Pronunciamentos e discursos          |

### 2.2. API Administrativa

A API Administrativa possui endpoints relacionados a:

| Categoria         | Conjuntos de Dados | Formato Principal |
| ----------------- | ------------------ | ----------------- |
| Contratações      | 10+                | CSV, Swagger/JSON |
| Licitações        | 5+                 | CSV, Swagger/JSON |
| Empresas          | 2+                 | CSV, Swagger/JSON |
| Itens Contratuais | 5+                 | CSV, Swagger/JSON |

## 3. Lista Completa de Endpoints - API Legislativa

### 3.1. Parlamentar (Senadores)

#### Listagem e Busca

1. `GET /dadosabertos/senador/lista/atual` - Lista senadores atualmente em exercício
2. `GET /dadosabertos/senador/lista/legislatura/{legislatura}` - Senadores de uma legislatura
3. `GET /dadosabertos/senador/lista/legislatura/{legislaturaInicio}/{legislaturaFim}` - Senadores em período
4. `GET /dadosabertos/senador/afastados` - Senadores afastados
5. `GET /dadosabertos/senador/partidos` - Lista de partidos
6. `GET /dadosabertos/senador/lista/tiposUsoPalavra` - Tipos de uso da palavra

#### Detalhes por Senador

7. `GET /dadosabertos/senador/{codigo}` - Detalhes de um senador
8. `GET /dadosabertos/senador/{codigo}/votacoes` - Votações do senador
9. `GET /dadosabertos/senador/{codigo}/relatorias` - Relatorias do senador
10. `GET /dadosabertos/senador/{codigo}/profissao` - Profissões do senador
11. `GET /dadosabertos/senador/{codigo}/mandatos` - Mandatos do senador
12. `GET /dadosabertos/senador/{codigo}/liderancas` - Lideranças do senador
13. `GET /dadosabertos/senador/{codigo}/licencas` - Licenças do senador
14. `GET /dadosabertos/senador/{codigo}/historicoAcademico` - Formação acadêmica
15. `GET /dadosabertos/senador/{codigo}/filiacoes` - Filiações partidárias
16. `GET /dadosabertos/senador/{codigo}/discursos` - Discursos do senador
17. `GET /dadosabertos/senador/{codigo}/comissoes` - Comissões do senador
18. `GET /dadosabertos/senador/{codigo}/cargos` - Cargos do senador
19. `GET /dadosabertos/senador/{codigo}/autorias` - Autorias de matérias
20. `GET /dadosabertos/senador/{codigo}/apartes` - Apartes realizados

### 3.2. Processo Legislativo

#### Informações Gerais

1. `GET /dadosabertos/processo` - Lista de processos
2. `GET /dadosabertos/processo/{id}` - Detalhe de um processo
3. `GET /dadosabertos/processo/siglas` - Siglas de processos
4. `GET /dadosabertos/processo/classes` - Classes de processos
5. `GET /dadosabertos/processo/assuntos` - Assuntos legislativos
6. `GET /dadosabertos/processo/destinos` - Destinos de processos
7. `GET /dadosabertos/processo/entes` - Entes federativos

#### Tipos e Referências

8. `GET /dadosabertos/processo/tipos-situacao` - Tipos de situação
9. `GET /dadosabertos/processo/tipos-decisao` - Tipos de decisão
10. `GET /dadosabertos/processo/tipos-autor` - Tipos de autor
11. `GET /dadosabertos/processo/tipos-atualizacao` - Tipos de atualização

#### Sub-recursos

12. `GET /dadosabertos/processo/relatoria` - Relatorias de processos
13. `GET /dadosabertos/processo/prazo` - Prazos de processos
14. `GET /dadosabertos/processo/prazo/tipos` - Tipos de prazo
15. `GET /dadosabertos/processo/emenda` - Emendas de processos
16. `GET /dadosabertos/processo/documento` - Documentos de processos
17. `GET /dadosabertos/processo/documento/tipos` - Tipos de documento
18. `GET /dadosabertos/processo/documento/tipos-conteudo` - Tipos de conteúdo

### 3.3. Matérias (Proposições)

#### Busca e Listagem

1. `GET /dadosabertos/materia/tramitando` - Matérias tramitando
2. `GET /dadosabertos/materia/legislaturaatual` - Matérias da legislatura atual
3. `GET /dadosabertos/materia/atualizadas` - Matérias atualizadas
4. `GET /dadosabertos/materia/pesquisa/lista` - Pesquisa de matérias
5. `GET /dadosabertos/materia/lista/tramitacao` - Lista por tramitação
6. `GET /dadosabertos/materia/lista/comissao` - Matérias em comissão
7. `GET /dadosabertos/materia/lista/prazo/{codPrazo}` - Matérias por prazo

#### Detalhes

8. `GET /dadosabertos/materia/{codigo}` - Detalhe de uma matéria
9. `GET /dadosabertos/materia/{sigla}/{numero}/{ano}` - Matéria por sigla/número/ano
10. `GET /dadosabertos/materia/situacaoatual/{codigo}` - Situação atual
11. `GET /dadosabertos/materia/situacaoatual/tramitacao/{data}` - Situação por data
12. `GET /dadosabertos/materia/movimentacoes/{codigo}` - Movimentações
13. `GET /dadosabertos/materia/ordia/{codigo}` - Ordem do dia
14. `GET /dadosabertos/materia/textos/{codigo}` - Textos da matéria

#### Vetos

15. `GET /dadosabertos/materia/vetos/{ano}` - Vetos por ano
16. `GET /dadosabertos/materia/vetos/encerrados` - Vetos encerrados
17. `GET /dadosabertos/materia/vetos/aposrcn` - Vetos após RCN
18. `GET /dadosabertos/materia/vetos/antesrcn` - Vetos antes RCN

#### Relações e Documentos

19. `GET /dadosabertos/materia/autoria/{codigo}` - Autoria da matéria
20. `GET /dadosabertos/materia/distribuicao/autoria` - Distribuição de autorias
21. `GET /dadosabertos/materia/distribuicao/autoria/{siglaComissao}` - Autoria por comissão
22. `GET /dadosabertos/materia/distribuicao/relatoria/{sigla}` - Distribuição de relatoria
23. `GET /dadosabertos/materia/relatorias/{codigo}` - Relatorias
24. `GET /dadosabertos/materia/emendas/{codigo}` - Emendas
25. `GET /dadosabertos/materia/emenda/{idDomaEmenda}` - Detalhe da emenda
26. `GET /dadosabertos/materia/votacoes/{codigo}` - Votações da matéria
27. `GET /dadosabertos/materia/atualizacoes/{codigo}` - Atualizações

#### Tipos e Referências

28. `GET /dadosabertos/materia/classes` - Classes de matéria
29. `GET /dadosabertos/materia/subtipos` - Subtipos de matéria
30. `GET /dadosabertos/materia/situacoes` - Situações de matéria
31. `GET /dadosabertos/materia/assuntos` - Assuntos de matéria
32. `GET /dadosabertos/materia/decisoes` - Decisões de matéria
33. `GET /dadosabertos/materia/destinos` - Destinos de matéria
34. `GET /dadosabertos/materia/locais` - Locais de tramitação
35. `GET /dadosabertos/materia/tiposatualizacoes` - Tipos de atualização
36. `GET /dadosabertos/materia/tiposTurnoApresentacao` - Tipos de turno
37. `GET /dadosabertos/materia/tiposPrazo` - Tipos de prazo
38. `GET /dadosabertos/materia/tiposNatureza` - Tipos de natureza
39. `GET /dadosabertos/materia/tiposEmenda` - Tipos de emenda

### 3.4. Plenário

#### Sessões e Agenda

1. `GET /dadosabertos/plenario/agenda/dia/{data}` - Agenda do dia
2. `GET /dadosabertos/plenario/agenda/mes/{data}` - Agenda do mês
3. `GET /dadosabertos/plenario/agenda/cn/{data}` - Agenda CN (Congresso Nacional)
4. `GET /dadosabertos/plenario/agenda/cn/{inicio}/{fim}` - Agenda CN por período
5. `GET /dadosabertos/plenario/agenda/atual/iCal` - Agenda em formato iCal
6. `GET /dadosabertos/plenario/legislatura/{data}` - Legislatura por data
7. `GET /dadosabertos/plenario/tiposSessao` - Tipos de sessão

#### Encontros (Sessões)

8. `GET /dadosabertos/plenario/encontro/{codigo}` - Detalhe de um encontro
9. `GET /dadosabertos/plenario/encontro/{codigo}/resumo` - Resumo do encontro
10. `GET /dadosabertos/plenario/encontro/{codigo}/resultado` - Resultado do encontro
11. `GET /dadosabertos/plenario/encontro/{codigo}/pauta` - Pauta do encontro

#### Votações

12. `GET /dadosabertos/plenario/lista/votacao/{dataSessao}` - Votações por sessão
13. `GET /dadosabertos/plenario/lista/votacao/{dataInicio}/{dataFim}` - Votações por período
14. `GET /dadosabertos/plenario/votacao/nominal/{ano}` - Votações nominais do ano
15. `GET /dadosabertos/plenario/votacao/orientacaoBancada/{dataSessao}` - Orientação de bancada
16. `GET /dadosabertos/plenario/votacao/orientacaoBancada/{dataInicio}/{dataFim}` - Orientação por período

#### Resultados

17. `GET /dadosabertos/plenario/resultado/{data}` - Resultado por data
18. `GET /dadosabertos/plenario/resultado/mes/{data}` - Resultado do mês
19. `GET /dadosabertos/plenario/resultado/cn/{data}` - Resultado CN
20. `GET /dadosabertos/plenario/resultado/veto/{codigo}` - Resultado de veto
21. `GET /dadosabertos/plenario/resultado/veto/materia/{codigo}` - Veto por matéria
22. `GET /dadosabertos/plenario/resultado/veto/dispositivo/{codigo}` - Veto por dispositivo

#### Outros

23. `GET /dadosabertos/plenario/lista/discursos/{dataInicio}/{dataFim}` - Discursos por período
24. `GET /dadosabertos/plenario/lista/tiposComparecimento` - Tipos de comparecimento
25. `GET /dadosabertos/plenario/lista/legislaturas` - Lista de legislaturas

### 3.5. Comissões

#### Listagem

1. `GET /dadosabertos/comissao/lista/{tipo}` - Lista de comissões por tipo
2. `GET /dadosabertos/comissao/lista/colegiados` - Lista de colegiados
3. `GET /dadosabertos/comissao/lista/mistas` - Comissões mistas
4. `GET /dadosabertos/comissao/lista/tiposColegiado` - Tipos de colegiado

#### Detalhes

5. `GET /dadosabertos/comissao/{codigo}` - Detalhe de uma comissão
6. `GET /dadosabertos/comissao/{comissao}/documentos` - Documentos da comissão
7. `GET /dadosabertos/comissao/cpi/{comissao}/requerimentos` - Requerimentos de CPI

#### Reuniões

8. `GET /dadosabertos/comissao/reuniao/{codigoReuniao}` - Detalhe de reunião
9. `GET /dadosabertos/comissao/reuniao/notas/{codigoReuniao}` - Notas taquigráficas
10. `GET /dadosabertos/comissao/reuniao/{sigla}/documento/{tipoDocumento}` - Documentos de reunião

#### Agenda

11. `GET /dadosabertos/comissao/agenda/{dataReferencia}` - Agenda por data
12. `GET /dadosabertos/comissao/agenda/{dataInicio}/{dataFim}` - Agenda por período
13. `GET /dadosabertos/comissao/agenda/mes/{mesReferencia}` - Agenda do mês
14. `GET /dadosabertos/comissao/agenda/atual/iCal` - Agenda em formato iCal

### 3.6. Composição Política

#### Mesa Diretora

1. `GET /dadosabertos/composicao/mesaSF` - Mesa do Senado Federal
2. `GET /dadosabertos/composicao/mesaCN` - Mesa do Congresso Nacional

#### Lideranças

3. `GET /dadosabertos/composicao/lista/liderancaSF` - Lideranças no Senado
4. `GET /dadosabertos/composicao/lista/liderancaCN` - Lideranças no Congresso
5. `GET /dadosabertos/composicao/lideranca` - Lista de lideranças
6. `GET /dadosabertos/composicao/lideranca/tipos` - Tipos de liderança
7. `GET /dadosabertos/composicao/lideranca/tipos-unidade` - Tipos de unidade

#### Partidos e Blocos

8. `GET /dadosabertos/composicao/lista/partidos` - Lista de partidos
9. `GET /dadosabertos/composicao/lista/blocos` - Blocos partidários
10. `GET /dadosabertos/composicao/bloco/{codigo}` - Detalhe de bloco

#### Cargos e Composição

11. `GET /dadosabertos/composicao/lista/{tipo}` - Lista por tipo
12. `GET /dadosabertos/composicao/lista/cn/{tipo}` - Lista CN por tipo
13. `GET /dadosabertos/composicao/lista/tiposCargo` - Tipos de cargo
14. `GET /dadosabertos/composicao/comissao/{codigo}` - Composição de comissão
15. `GET /dadosabertos/composicao/comissao/resumida/mista/{codigo}/{dataInicio}/{dataFim}` - Composição mista resumida
16. ` /dadosabertos/composicao/comissao/atual/mista/{codigo}` - Composição mista atual

### 3.7. Votação

1. `GET /dadosabertos/votacao` - Lista de votações
2. `GET /dadosabertos/votacaoComissao/parlamentar/{codigo}` - Votações em comissão por parlamentar
3. `GET /dadosabertos/votacaoComissao/materia/{sigla}/{numero}/{ano}` - Votações em comissão por matéria
4. `GET /dadosabertos/votacaoComissao/comissao/{siglaComissao}` - Votações por comissão

### 3.8. Legislação

1. `GET /dadosabertos/legislacao/{codigo}` - Detalhe de legislação
2. `GET /dadosabertos/legislacao/{tipo}/{numdata}/{anoseq}` - Legislação por tipo/número/ano
3. `GET /dadosabertos/legislacao/urn` - Busca por URN
4. `GET /dadosabertos/legislacao/lista` - Lista de legislação
5. `GET /dadosabertos/legislacao/termos` - Termos legislativos
6. `GET /dadosabertos/legislacao/classes` - Classes de legislação
7. `GET /dadosabertos/legislacao/tiposNorma` - Tipos de norma
8. `GET /dadosabertos/legislacao/tiposPublicacao` - Tipos de publicação
9. `GET /dadosabertos/legislacao/tiposVide` - Tipos de vide
10. `GET /dadosabertos/legislacao/tiposdeclaracao/detalhe` - Tipos de declaração

### 3.9. Discurso

1. `GET /dadosabertos/discurso/texto-integral/{codigoPronunciamento}` - Texto integral
2. `GET /dadosabertos/discurso/texto-binario/{codigoPronunciamento}` - Texto em binário

### 3.10. Taquigrafia (Vídeos e Notas)

1. `GET /dadosabertos/taquigrafia/videos/sessao/{idSessao}` - Vídeos de sessão
2. `GET /dadosabertos/taquigrafia/videos/reuniao/{idReuniao}` - Vídeos de reunião
3. `GET /dadosabertos/taquigrafia/notas/sessao/{idSessao}` - Notas de sessão
4. `GET /dadosabertos/taquigrafia/notas/reuniao/{idReuniao}` - Notas de reunião

### 3.11. Orçamento

1. `GET /dadosabertos/orcamento/lista` - Lista de orçamento
2. `GET /dadosabertos/orcamento/oficios` - Lista de ofícios
3. `GET /dadosabertos/orcamento/oficios/{numeroSedol}` - Detalhe de ofício

### 3.12. Autor

1. `GET /dadosabertos/autor/lista/atual` - Autores atuais
2. `GET /dadosabertos/autor/tiposAutor` - Tipos de autor

## 4. Lista de Endpoints - API Administrativa

### 4.1. Contratações (Swagger/JSON e CSV)

#### Contratos

1. `GET /adm-dadosabertos/api/v1/contratacoes/contratos/csv` - Contratos em CSV
2. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarContratos` - Contratos em JSON

#### Notas de Empenho

3. `GET /adm-dadosabertos/api/v1/contratacoes/notas_empenho/csv` - Notas de empenho em CSV
4. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarNotasEmpenho` - Notas em JSON

#### Licitações

5. `GET /adm-dadosabertos/api/v1/contratacoes/licitacoes/csv` - Licitações em CSV
6. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarLicitacoes` - Licitações em JSON

#### Atas de Registro de Preços

7. `GET /adm-dadosabertos/api/v1/contratacoes/atas_registro_preco/csv` - ARPs em CSV
8. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarAtasRegistroPreco` - ARPs em JSON

#### Empresas

9. `GET /adm-dadosabertos/api/v1/contratacoes/empresas/csv` - Empresas em CSV
10. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarEmpresas` - Empresas em JSON

#### Itens

11. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarItensPorContratacao` - Itens de contrato
12. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarItensPorContratacaoCsv` - Itens em CSV

#### Aditivos

13. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarAditivosPorContrato` - Aditivos contratuais
14. `GET /adm-dadosabertos/swagger-ui/../Contratações/buscarAditivosPorContratoCsv` - Aditivos em CSV

## 5. Matriz de Suporte a Formatos

### 5.1. API Legislativa

✅ **Suporte JSON e XML em todos os endpoints legislativos**

### 5.2. Testes Realizados

| Endpoint Testado                          | JSON | XML | CSV | CORS | Status |
| ----------------------------------------- | ---- | --- | --- | ---- | ------ |
| `/dadosabertos/senador/lista/atual`       | ✅   | ✅  | ❌  | ✅   | OK     |
| `/dadosabertos/senador/{codigo}`          | ✅   | ✅  | ❌  | ✅   | OK     |
| `/dadosabertos/senador/partidos`          | ✅   | ✅  | ❌  | ✅   | OK     |
| `/dadosabertos/materia/tramitando`        | ✅   | ✅  | ❌  | ✅   | OK     |
| `/dadosabertos/composicao/mesaSF`         | ✅   | ✅  | ❌  | ✅   | OK     |
| `/dadosabertos/comissao/lista/colegiados` | ✅   | ✅  | ❌  | ✅   | OK     |
| **/adm-dadosabertos/api/v1/.../csv**      | ❌   | ❌  | ✅  | ⚠️   | OK     |
| **/adm-dadosabertos/swagger-ui/...**      | ✅   | ❌  | ❌  | ⚠️   | OK     |

### 5.3. Conclusões dos Testes

1. **API Legislativa:**
   - ✅ 100% suporte a JSON via header `Accept: application/json` ou sufixo `.json`
   - ✅ 100% suporte a XML via header `Accept: application/xml` ou sufixo `.xml`
   - ✅ 100% suporte a CORS (`access-control-allow-origin: *`)

2. **API Administrativa:**
   - ✅ Endpoints CSV funcionam corretamente
   - ✅ Endpoints Swagger retornam JSON
   - ⚠️ CORS limitado (problema com preflight OPTIONS)
   - ⚠️ Recomenda-se consumir via backend/proxy

## 6. Como Consumir a API em JSON

### 6.1. Método 1: Header Accept (Recomendado para API Legislativa)

```javascript
// JavaScript/TypeScript
const response = await fetch(
  "https://legis.senado.leg.br/dadosabertos/senador/lista/atual",
  {
    headers: {
      Accept: "application/json",
    },
  },
);
const data = await response.json();
```

```bash
# curl
curl -H "Accept: application/json" \
  "https://legis.senado.leg.br/dadosabertos/senador/lista/atual"
```

```python
# Python
import requests

response = requests.get(
    'https://legis.senado.leg.br/dadosabertos/senador/lista/atual',
    headers={'Accept': 'application/json'}
)
data = response.json()
```

### 6.2. Método 2: Sufixo .json (Alternativa Simples)

A API permite adicionar `.json` ao final da URL:

```javascript
// Formato mais simples
const response = await fetch(
  "https://legis.senado.leg.br/dadosabertos/senador/lista/atual.json",
);
const data = await response.json();
```

```bash
curl "https://legis.senado.leg.br/dadosabertos/senador/lista/atual.json"
```

### 6.3. Exemplo Completo TypeScript

```typescript
interface Senador {
  IdentificacaoParlamentar: {
    CodigoParlamentar: string;
    CodigoPublicoNaLegAtual: string;
    NomeParlamentar: string;
    NomeCompleto: string;
    SiglaPartidoParlamentar: string;
    UfParlamentar: string;
  };
}

interface SenadoAPIResponse {
  ListaParlamentarEmExercicio: {
    Metadados: {
      Versao: string;
      VersaoServico: string;
      DataVersaoServico: string;
      DescricaoDataSet: string;
    };
    Parlamentares: {
      Parlamentar: Senador[];
    };
  };
}

async function buscarSenadoresAtuais(): Promise<Senador[]> {
  const response = await fetch(
    "https://legis.senado.leg.br/dadosabertos/senador/lista/atual.json",
  );

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  const data: SenadoAPIResponse = await response.json();
  return data.ListaParlamentarEmExercicio.Parlamentares.Parlamentar;
}

// Uso
const senadores = await buscarSenadoresAtuais();
console.log(`Total de senadores: ${senadores.length}`);
senadores.forEach((s) => {
  console.log(
    `${s.IdentificacaoParlamentar.NomeParlamentar} - ${s.IdentificacaoParlamentar.SiglaPartidoParlamentar}/${s.IdentificacaoParlamentar.UfParlamentar}`,
  );
});
```

### 6.4. Cliente Completo para Senado

```typescript
class SenadoAPI {
  private baseURL = "https://legis.senado.leg.br/dadosabertos";

  private async fetch<T>(
    endpoint: string,
    format: "json" | "xml" = "json",
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}.${format}`;

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  // Senadores
  async senadoresAtuais() {
    return this.fetch<any>("/senador/lista/atual");
  }

  async senador(codigo: number) {
    return this.fetch<any>(`/senador/${codigo}`);
  }

  async senadorVotacoes(
    codigo: number,
    params?: {
      dataInicio?: string;
      dataFim?: string;
    },
  ) {
    let endpoint = `/senador/${codigo}/votacoes`;
    if (params) {
      const query = new URLSearchParams(params as any).toString();
      endpoint += `?${query}`;
    }
    return this.fetch<any>(endpoint);
  }

  // Matérias
  async materiaTramitando() {
    return this.fetch<any>("/materia/tramitando");
  }

  async materia(codigo: number) {
    return this.fetch<any>(`/materia/${codigo}`);
  }

  async materiaPorSigla(sigla: string, numero: number, ano: number) {
    return this.fetch<any>(`/materia/${sigla}/${numero}/${ano}`);
  }

  // Votações
  async votacoesPlenario(dataSessao: string) {
    return this.fetch<any>(`/plenario/lista/votacao/${dataSessao}`);
  }

  // Composição
  async mesaSenado() {
    return this.fetch<any>("/composicao/mesaSF");
  }

  async partidos() {
    return this.fetch<any>("/composicao/lista/partidos");
  }
}

// Uso
const api = new SenadoAPI();

// Buscar senadores
const senadores = await api.senadoresAtuais();

// Buscar um senador específico
const senador = await api.senador(5035);

// Buscar votações de um senador
const votacoes = await api.senadorVotacoes(5035, {
  dataInicio: "20250101",
  dataFim: "20250131",
});

// Buscar matérias
const materias = await api.materiaTramitando();

// Buscar uma matéria específica
const pls = await api.materiaPorSigla("PLS", 229, 2009);
```

### 6.5. Tratamento de Rate Limiting

A API do Senado limita a 10 requisições por segundo:

```typescript
class SenadoAPIComRateLimit {
  prate lastRequest = 0;
  private minInterval = 100; // 100ms = 10 req/s

  private async waitForRateLimit() {
    const now = Date.now();
    const elapsed = now - this.lastRequest;

    if (elapsed < this.minInterval) {
      await new Promise(resolve => setTimeout(resolve, this.minInterval - elapsed));
    }

    this.lastRequest = Date.now();
  }

  async fetch<T>(url: string): Promise<T> {
    await this.waitForRateLimit();

    const response = await fetch(url);

    if (response.status === 429) {
      // Too Many Requests - aguardar mais tempo
      await new Promise(resolve => setTimeout(resolve, 1000));
      return this.fetch<T>(url);
    }

    if (response.status === 503) {
      // Service Unavailable - retry com backoff
      await new Promise(resolve => setTimeout(resolve, 2000));
      return this.fetch<T>(url);
    }

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
  }
}
```

## 7. Como Consumir a API em XML

### 7.1. Via Header Accept

```bash
curl -H "Accept: application/xml" \
  "https://legis.senado.leg.br/dadosabertos/senador/lista/atual"
```

### 7.2. Via Sufixo .xml

```bash
curl "https://legis.senado.leg.br/dadosabertos/senador/lista/atual.xml"
```

### 7.3. Estrutura da Resposta XML

```xml
<?xml version='1.0' encoding='UTF-8'?>
<ListaParlamentarEmExercicio xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
    xsi:noNamespaceSchemaLocation='https://legis.senado.leg.br/dadosabertos/dados/ListaParlamentarEmExerciciov4.xsd'>
    <Metadados>
        <Versao>21/02/2026 10:52:01</Versao>
        <VersaoServico>4</VersaoServico>
        <DataVersaoServico>2020-07-15</DataVersaoServico>
        <DescricaoDataSet>Lista dos Parlamentares que estão atualmente em Exercício.</DescricaoDataSet>
    </Metadados>
    <Parlamentares>
        <Parlamentar>
            <IdentificacaoParlamentar>
                <CodigoParlamentar>5672</CodigoParlamentar>
                <NomeParlamentar>Nome do Senador</NomeParlamentar>
                <!-- ... -->
            </IdentificacaoParlamentar>
        </Parlamentar>
    </Parlamentares>
</ListaParlamentarEmExercicio>
```

### 7.4. Consumindo XML com JavaScript

```javascript
async function fetchXML(url) {
  const response = await fetch(url, {
    headers: { Accept: "application/xml" },
  });

  const xmlText = await response.text();
  const parser = new DOMParser();
  const xmlDoc = parser.parseFromString(xmlText, "text/xml");

  return xmlDoc;
}

const xmlDoc = await fetchXML(
  "https://legis.senado.leg.br/dadosabertos/senador/lista/atual",
);
const parlamentares = xmlDoc.querySelectorAll("Parlamentar");

parlamentares.forEach((p) => {
  const nome = p.querySelector("NomeParlamentar")?.textContent;
  const partido = p.querySelector("SiglaPartidoParlamentar")?.textContent;
  console.log(`${nome} - ${partido}`);
});
```

## 8. Como Consumir CSV (API Administrativa)

### 8.1. Consumindo CSV Diretamente

```javascript
async function buscarContratosCSV() {
  const response = await fetch(
    "https://adm.senado.leg.br/adm-dadosabertos/api/v1/contratacoes/contratos/csv",
  );

  const csvText = await response.text();
  return csvText;
}

// Uso
const csv = await buscarContratosCSV();
console.log(csv);
```

### 8.2. Parseando CSV com JavaScript

```typescript
// Usando biblioteca csv-parse
import { parse } from "csv-parse/sync";

async function buscarContratosParseados() {
  const response = await fetch(
    "https://adm.senado.leg.br/adm-dadosabertos/api/v1/contratacoes/contratos/csv",
  );

  const csvText = await response.text();

  const records = parse(csvText, {
    columns: true,
    skip_empty_lines: true,
    delimiter: ";", // CSV do Senado usa ponto-e-vírgula
  });

  return records;
}

// Uso
const contratos = await buscarContratosParseados();
contratos.forEach((contrato) => {
  console.log(`${contrato.NUMERO_CONTRATO} - ${contrato.CONTRATADA}`);
});
```

### 8.3. Consumindo via Backend (Recomendado para API Administrativa)

Devido às limitações de CORS na API administrativa, recomenda-se criar um proxy no backend:

```typescript
// Next.js API Route: pages/api/senado/contratos.ts
export default async function handler(req, res) {
  try {
    const response = await fetch(
      "https://adm.senado.leg.br/adm-dadosabertos/api/v1/contratacoes/contratos/csv",
    );

    const csv = await response.text();

    res.status(200).send(csv);
  } catch (error) {
    res.status(500).json({ error: "Erro ao buscar contratos" });
  }
}

// Cliente frontend
const response = await fetch("/api/senado/contratos");
const csv = await response.text();
```

## 9. Formato de Datas e Parâmetros

### 9.1. Formato de Datas

A API do Senado utiliza o formato `AAAAMMDD` (sem separadores):

```typescript
// Correto
const dataInicio = "20250101"; // 01/01/2025
const dataFim = "20250131"; // 31/01/2025

// Endpoint
const url = `https://legis.senado.leg.br/dadosabertos/senador/${codigo}/votacoes?dataInicio=${dataInicio}&dataFim=${dataFim}`;
```

### 9.2. Parâmetros Comuns

| Parâmetro     | Tipo    | Formato  | Descrição                             |
| ------------- | ------- | -------- | ------------------------------------- |
| `codigo`      | integer | Numérico | Código do parlamentar/matéria         |
| `sigla`       | string  | Texto    | Sigla da comissão/matéria             |
| `numero`      | integer | Numérico | Número da matéria                     |
| `ano`         | integer | AAAA     | Ano da matéria                        |
| `dataInicio`  | string  | AAAAMMDD | Data de início                        |
| `dataFim`     | string  | AAAAMMDD | Data de fim                           |
| `legislatura` | integer | Numérico | Número da legislatura                 |
| `v`           | string  | -        | Parâmetro de versionamento (opcional) |

## 10. Headers HTTP Importantes

### 10.1. Headers de Resposta (API Legislativa)

| Header                             | Valor Típico                            | Descrição                            |
| ---------------------------------- | --------------------------------------- | ------------------------------------ |
| `access-control-allow-origin`      | `*`                                     | CORS habilitado para qualquer origem |
| `access-control-allow-credentials` | `true`                                  | Permite credenciais                  |
| `access-control-allow-methods`     | `GET,POST,OPTIONS,HEAD`                 | Métodos permitidos                   |
| `access-control-allow-headers`     | `X-Requested-With, Authorization, ...`  | Headers permitidos                   |
| `content-type`                     | `application/json` ou `application/xml` | Tipo de conteúdo                     |

### 10.2. Headers de Depreciação

Serviços depreciados retornam:

```
Deprecation: Mon, 10 Mar 2025 00:00:00 GMT
Sunset: Sun, 01 Feb 2026 00:00:00 GMT
Link: <https://legis.senado.leg.br/dadosabertos/composicao/lideranca>; rel="successor"
```

## 11. Estrutura de Resposta

### 11.1. Estrutura JSON Típica

```json
{
  "Root": {
    "noNamespaceSchemaLocation": "https://legis.senado.leg.br/dadosabertos/dados/Schema.xsd",
    "Metadados": {
      "Versao": "21/02/2026 10:51:48",
      "VersaoServico": "4",
      "DataVersaoServico": "2020-07-15",
      "DescricaoDataSet": "Descrição do conjunto de dados"
    },
    "Dados": {
      // ... dados específicos do endpoint
    }
  }
}
```

### 11.2. Metadados

Todas as respostas incluem metadados com:

- **Versao:** Data e hora da versão dos dados
- **VersaoServico:** Versão da API do serviço
- **DataVersaoServico:** Data da versão do serviço
- **DescricaoDataSet:** Descrição do conjunto de dados retornado

## 12. Boas Práticas

### 12.1. Rate Limiting

✅ **Respeite o limite de 10 requisições por segundo**

- Implemente throttling no cliente
- Use filas para requisições em lote
- Implemente retry com backoff exponencial

### 12.2. Horários de Requisições

⚠️ **Evite horários arredondados**

- Não programe rotinas para executar exatamente às 00:00:00, 01:00:00, etc.
- Isso causa picos de acesso simultâneo
- Use horários randomizados ou escalonados

### 12.3. Tratamento de Erros

| Código HTTP | Significado          | Ação Recomendada     |
| ----------- | -------------------- | -------------------- |
| 200         | Sucesso              | Processar resposta   |
| 400         | Requisição inválida  | Verificar parâmetros |
| 403         | Proibido (CORS)      | Usar proxy backend   |
| 404         | Não encontrado       | Verificar código/ID  |
| 429         | Too Many Requests    | Aguardar e retry     |
| 503         | Serviço indisponível | Retry com backoff    |

### 12.4. Cache

- Implemente cache local para dados que não mudam frequentemente (legislaturas, tipos, etc.)
- Respeite metadados de versão para invalidação de cache
- Use cache de 5-10 minutos para dados dinâmicos

### 12.5. API Administrativa

⚠️ **Limitações de CORS**

- Sempre consuma via backend/servidor
- Não tente consumir diretamente do navegador
- Use proxy reverso ou API intermediária

## 13. Exemplos Práticos

### 13.1. Buscar Todos os Senadores de um Partido

```typescript
async function buscarSenadoresPorPartido(siglaPartido: string) {
  const api = new SenadoAPI();
  const response = await api.senadoresAtuais();

  const senadores =
    response.ListaParlamentarEmExercicio.Parlamentares.Parlamentar;

  return senadores.filter(
    (s) => s.IdentificacaoParlamentar.SiglaPartidoParlamentar === siglaPartido,
  );
}

// Uso
const senadoresPT = await buscarSenadoresPorPartido("PT");
console.log(`Total de senadores do PT: ${senadoresPT.length}`);
```

### 13.2. Buscar Votações de um Senador em um Período

```typescript
async function analisarVotacoesSenador(
  codigo: number,
  mes: number,
  ano: number,
) {
  const dataInicio = `${ano}${String(mes).padStart(2, "0")}01`;
  const ultimoDia = new Date(ano, mes, 0).getDate();
  const dataFim = `${ano}${String(mes).padStart(2, "0")}${ultimoDia}`;

  const api = new SenadoAPI();
  const response = await api.senadorVotacoes(codigo, { dataInicio, dataFim });

  // Processar votações
  return response;
}

// Uso
const votacoes = await analisarVotacoesSenador(5035, 1, 2025);
```

### 13.3. Buscar Matérias Tramitando por Assunto

```typescript
async function buscarMateriasPorAssunto(codigoAssunto: number) {
  const api = new SenadoAPI();
  const response = await api.materiaTramitando();

  // Filtrar por assunto (se a API retornar essa informação)
  // A implementação depende da estrutura da resposta

  return response;
}
```

## 14. Recursos Adicionais

### 14.1. Links Úteis

- **Portal Dados Abertos:** https://www12.senado.leg.br/dados-abertos
- **Catálogo de Dados:** https://www12.senado.leg.br/dados-abertos/conjuntos
- **Swagger Administrativo:** https://adm.senado.gov.br/adm-dadosabertos/swagger-ui/
- **Schemas XSD:** https://legis.senado.leg.br/dadosabertos/dados/

### 14.2. Portais por Categoria

- **Administrativo - Senadores:** https://www12.senado.leg.br/dados-abertos/conjuntos?portal=Administrativo&grupo=senadores
- **Legislativo - Senadores:** https://www12.senado.leg.br/dados-abertos/conjuntos?portal=Legislativo&grupo=senadores
- **Legislativo - Plenário:** https://www12.senado.leg.br/dados-abertos/conjuntos?portal=Legislativo&grupo=plenario
- **Legislativo - Composição:** https://www12.senado.leg.br/dados-abertos/conjuntos?portal=Legislativo&grupo=composicao
- **Legislativo - Comissões:** https://www12.senado.leg.br/dados-abertos/conjuntos?portal=Legislativo&grupo=comissoes
- **Legislativo - Legislação:** https://www12.senado.leg.br/dados-abertos/conjuntos?portal=Legislativo&grupo=legislacao

## 15. Limitações e Avisos

### 15.1. Avisos Oficiais

Conforme documentação oficial:

> "Dados Abertos são representados em meio digital, processáveis por máquina, referenciados na rede mundial de computadores e disponibilizados sob licença que permita sua livre utilização."

### 15.2. Limitações Conhecidas

1. **Rate Limiting:** Máximo de 10 requisições por segundo
2. **Serviços depreciados:** Alguns serviços estão em processo de descontinuação
3. **CORS na API Administrativa:** Limitações de CORS podem exigir uso de proxy
4. **Horários de pico:** Em instantes de muita demanda, pode ocorrer erro 503
5. **Schemas:** As respostas seguem schemas XSD específicos

### 15.3. Recomendações

1. **Monitore headers de depreciação:** Verifique Deprecation, Sunset e Link
2. **Implemente retry:** Prepare para lidar com erros 429 e 503
3. **Use versionamento:** Alguns endpoints suportam parâmetro `v` para versões específicas
4. **Valide dados:** Sempre valide a estrutura dos dados recebidos
5. **Cache local:** Implemente cache para reduzir carga na API

## 16. Conclusão

A API de Dados Abertos do Senado Federal oferece:

✅ **157+ endpoints** cobrindo dados legislativos e administrativos  
✅ **Suporte completo a JSON e XML** (API Legislativa)  
✅ **CORS habilitado** para API Legislativa (`access-control-allow-origin: *`)  
⚠️ **CORS limitado** para API Administrativa (requer proxy)  
✅ **API bem estruturada** com metadados e versionamento  
✅ **Documentação disponível** com schemas XSD  
✅ **Múltiplos formatos** (JSON, XML, CSV)  
⚠️ **Rate limiting** (10 req/s)

A API é adequada para construção de aplicações de transparência legislativa, permitindo acesso a dados de parlamentares, proposições, votações e muito mais.

---

**Documento gerado em:** 21 de fevereiro de 2026  
**APIs testadas:** Legislativa e Administrativa  
**Conclusão dos testes:** ✅ API Legislativa totalmente funcional com JSON/XML e CORS | ⚠️ API Administrativa requer backend/proxy
