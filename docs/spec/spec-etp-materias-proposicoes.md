# 📘 Proposta Técnica

## Processo ETL para Extração de Matérias Legislativas via APIs da Câmara dos Deputados e do Senado Federal

---

## 1\. Objetivo

Projetar e implementar um **pipeline ETL (Extract, Transform, Load)** para:

- Extrair dados de **matérias legislativas e proposições que podem virar lei**, incluindo:
  - Projeto de Lei (PL)
  - Projeto de Lei Complementar (PLP)
  - Medida Provisória (MP)
  - Proposta de Emenda à Constituição (PEC)
  - Projeto de Decreto Legislativo (PDL)
  - Projeto de Resolução (PR)

- Priorizar consumo **em lote via CSV** (quando disponível — especialmente Câmara).
- Persistir dados estruturados em **PostgreSQL**.
- Implementar solução em **Java 21 + Spring Boot 3.x**.
- Permitir cargas iniciais massivas + atualizações incrementais.

---

# 2\. Arquitetura Proposta

## 2.1 Arquitetura Lógica

                +---------------------------+
                |   Scheduler (Spring)      |
                +-------------+-------------+
                              |
                              v
                +---------------------------+
                |  ETL Orchestrator Service |
                +-------------+-------------+
                              |
          +-------------------+-------------------+
          |                                       |
          v                                       v

+-----------------------+ +---------------------------+  
| Câmara CSV Loader | | Senado API JSON Loader |  
| (Download em lote) | | (REST incremental) |  
+-----------+-----------+ +-------------+-------------+  
 | |  
 v v  
 +-------------+ +-------------+  
 | Transformer | | Transformer |  
 +------+------+ +------+------+  
 | |  
 +-------------------+-----------------+  
 v  
 +------------------+  
 | PostgreSQL |  
 | (Modelo Normal.) |  
 +------------------+

---

# 3\. Estratégia de Extração (Extract)

---

## 3.1 Câmara dos Deputados – Prioridade CSV

A Câmara dos Deputados disponibiliza arquivos CSV estáticos de dados legislativos.

### Estratégia:

- Utilizar endpoints de **arquivos CSV estáticos**
- Realizar:
  - Download completo (carga inicial)
  - Download incremental (verificando timestamp / checksum)

### Vantagens

- Redução de chamadas REST
- Maior performance
- Melhor para histórico completo
- Menor risco de rate limiting

### Processo Técnico

1.  Download do CSV via `WebClient`
2.  Armazenamento temporário em:

    /tmp/etl/camara/{yyyyMMdd}/

3.  Parsing com:
    - `OpenCSV` ou `Jackson CSV`

4.  Stream processing (evitar carregar tudo em memória)

---

## 3.2 Senado Federal – Consumo via API REST

O Senado Federal oferece APIs REST com suporte a JSON/XML/CSV, porém com limitação de 10 req/s.

### Estratégia:

- Carga inicial paginada
- Atualizações incrementais por:
  - `dataAtualizacao`
  - `materias atualizadas`

- Implementar:
  - Rate limiting (Resilience4j)
  - Retry exponencial
  - Controle de janela temporal

---

# 4\. Transformação (Transform)

## 4.1 Normalização de Tipos de Proposição

Mapear siglas para tipos padronizados:

| Sigla | Tipo Normalizado         |
| ----- | ------------------------ |
| PL    | LEI\\\_ORDINARIA         |
| PLP   | LEI\\\_COMPLEMENTAR      |
| MPV   | MEDIDA\\\_PROVISORIA     |
| PEC   | EMENDA\\\_CONSTITUCIONAL |
| PDL   | DECRETO\\\_LEGISLATIVO   |
| PR    | RESOLUCAO                |

---

## 4.2 Enriquecimento

- Identificação da Casa de origem
- Status atual
- Situação legislativa
- Última tramitação
- Se já virou norma

---

## 4.3 Deduplicação

Chave natural:

(casa, tipo, numero, ano)

Chave técnica:

id UUID

---

# 5\. Modelo de Dados PostgreSQL

## 5.1 Tabela Principal: proposicao

SQL

CREATE TABLE proposicao (  
 id UUID PRIMARY KEY,  
 casa VARCHAR(20) NOT NULL,  
 tipo VARCHAR(50) NOT NULL,  
 sigla VARCHAR(20),  
 numero INTEGER,  
 ano INTEGER,  
 ementa TEXT,  
 situacao VARCHAR(255),  
 data_apresentacao DATE,  
 data_atualizacao TIMESTAMP,  
 status_final VARCHAR(100),  
 virou_lei BOOLEAN DEFAULT FALSE,  
 criado_em TIMESTAMP DEFAULT NOW(),  
 atualizado_em TIMESTAMP  
);

---

## 5.2 Tabela: tramitacao

SQL

CREATE TABLE tramitacao (  
 id UUID PRIMARY KEY,  
 proposicao_id UUID REFERENCES proposicao(id),  
 data_evento DATE,  
 orgao VARCHAR(255),  
 descricao TEXT,  
 situacao VARCHAR(255)  
);

---

## 5.3 Índices Estratégicos

SQL

CREATE INDEX idx_proposicao_tipo ON proposicao(tipo);  
CREATE INDEX idx_proposicao_ano ON proposicao(ano);  
CREATE INDEX idx_proposicao_status ON proposicao(status_final);  
CREATE INDEX idx_proposicao_data_update ON proposicao(data_atualizacao);

---

# 6\. Camadas da Aplicação (Spring Boot)

## 6.1 Módulos

etl-legislativo  
 ├── config  
 ├── scheduler  
 ├── extractor  
 │ ├── camara  
 │ └── senado  
 ├── transformer  
 ├── repository  
 ├── domain  
 └── orchestrator

---

## 6.2 Stack Tecnológica

- Java 21
- Spring Boot 3.x
- Spring Data JPA
- WebClient (Reactor)
- Resilience4j
- Flyway (migrações)
- PostgreSQL 15+
- Docker

---

# 7\. Estratégia de Carga

## 7.1 Carga Inicial (Full Load)

- Câmara → CSV completo
- Senado → Paginação por ano (ex: 1988 → atual)

Executado manualmente via:

/admin/etl/full-load

---

## 7.2 Carga Incremental

Executada via Scheduler:

Java

@Scheduled(cron \= "0 0 3 \* \* ?")  
public void runIncrementalLoad() {  
 etlOrchestrator.runIncremental();  
}

Critério:

- Buscar apenas registros atualizados nos últimos 2 dias.

---

# 8\. Performance e Escalabilidade

## Estratégias

- Batch insert com `saveAll`
- Desabilitar flush automático
- Usar `COPY` do PostgreSQL para cargas massivas
- Processamento paralelo por ano
- Pool de conexões Hikari otimizado

---

# 9\. Governança e Observabilidade

## Logs Estruturados

- Total processado
- Total inserido
- Total atualizado
- Total com erro

## Métricas (Micrometer + Prometheus)

- tempo_execucao_etl
- registros_por_segundo
- falhas_api_senado

---

# 10\. Tratamento de Erros

- Retry exponencial (até 3x)
- Circuit breaker para API Senado
- Persistência de falhas em tabela:

SQL

CREATE TABLE etl_error_log (  
 id UUID PRIMARY KEY,  
 origem VARCHAR(20),  
 payload TEXT,  
 erro TEXT,  
 criado_em TIMESTAMP DEFAULT NOW()  
);

---

# 11\. Segurança

- Aplicação interna
- Sem exposição pública
- Configuração via variáveis de ambiente
- Controle de acesso via Spring Security (perfil admin)

---

# 12\. Roadmap de Implementação

| Fase | Entrega                       |
| ---- | ----------------------------- |
| 1    | Modelo de dados + Flyway      |
| 2    | Loader CSV Câmara             |
| 3    | Loader REST Senado            |
| 4    | Transformações e normalização |
| 5    | Carga incremental             |
| 6    | Monitoramento e métricas      |
| 7    | Otimizações de performance    |

---

# 13\. Considerações Arquiteturais Estratégicas

- Priorizar CSV para reduzir overhead
- API Senado apenas para atualização incremental
- Banco preparado para futura indexação full-text
- Estrutura compatível com futura camada de Analytics (Power BI / Metabase)

---

# 14\. Resultado Esperado

Sistema capaz de:

- Manter histórico completo de proposições legislativas
- Identificar quais efetivamente viraram norma
- Permitir consultas analíticas por:
  - Tipo
  - Casa
  - Ano
  - Status
  - Tramitação

# 📘 Complemento da Proposta Técnica

## Controle de Ingestão, Rate Limit Dinâmico e Execução Paralela

Este complemento adiciona:

1.  **Mecanismo robusto de controle de ingestão (idempotência + reprocessamento controlado)**
2.  **Rate limit dinâmico adaptativo**
3.  **Execução paralela multi-thread segura e escalável**

Arquitetura base permanece:

- Extração via CSV (prioritário) da Câmara dos Deputados
- Extração via REST do Senado Federal
- Persistência em PostgreSQL
- Implementação em Java 21 + Spring Boot 3.x

---

# 1️⃣ Controle de Ingestão (Idempotência e Reprocessamento Controlado)

## 1.1 Objetivos

Evitar:

- Reprocessamento desnecessário
- Duplicação de dados
- Sobrecarga do banco
- Processamento redundante de CSVs já ingeridos

Permitir:

- Reprocessamento forçado sob demanda
- Auditoria de execuções
- Carga incremental segura

---

# 2️⃣ Estratégia Geral

O controle será baseado em três camadas:

1.  **Controle de arquivo (CSV fingerprint)**
2.  **Controle de registro (chave natural + hash de conteúdo)**
3.  **Controle de execução (job metadata)**

---

# 3️⃣ Controle de Arquivos CSV (Câmara)

## 3.1 Tabela de Controle de Arquivos

SQL

CREATE TABLE etl_file_control (  
 id UUID PRIMARY KEY,  
 origem VARCHAR(20), \-- CAMARA  
 nome_arquivo VARCHAR(255),  
 checksum VARCHAR(64),  
 tamanho_bytes BIGINT,  
 data_referencia DATE,  
 processado_em TIMESTAMP,  
 status VARCHAR(50), \-- SUCCESS / FAILED  
 forcar_reprocessamento BOOLEAN DEFAULT FALSE  
);

---

## 3.2 Lógica de Ingestão CSV

### Passo 1 — Download

- Baixar CSV
- Calcular SHA-256
- Verificar na tabela `etl_file_control`

### Regra:

| Situação                       | Ação      |
| ------------------------------ | --------- |
| Arquivo não existe na tabela   | Processar |
| Existe com mesmo checksum      | Ignorar   |
| Existe com checksum diferente  | Processar |
| Usuário forçou reprocessamento | Processar |

---

## 3.3 Endpoint para Forçar Reprocessamento

http

POST /admin/etl/camara/reprocess?data=2024-01-01

Implementação:

Java

public void forceReprocess(LocalDate dataReferencia) {  
 fileControlRepository.markForReprocess(dataReferencia);  
}

---

# 4️⃣ Controle de Registro (Deduplicação Inteligente)

Mesmo que um CSV seja reprocessado, registros não devem ser duplicados.

## 4.1 Chave Natural

SQL

UNIQUE (casa, sigla, numero, ano)

---

## 4.2 Hash de Conteúdo

Adicionar campo:

SQL

ALTER TABLE proposicao ADD COLUMN content_hash VARCHAR(64);

### Processo:

1.  Normalizar campos relevantes
2.  Gerar hash SHA-256
3.  Comparar com hash armazenado

### Regra:

| Situação                | Ação    |
| ----------------------- | ------- |
| Registro não existe     | INSERT  |
| Existe e hash igual     | IGNORAR |
| Existe e hash diferente | UPDATE  |

---

# 5️⃣ Controle de Execução (Job Tracking)

## 5.1 Tabela de Controle de Jobs

SQL

CREATE TABLE etl_job_control (  
 id UUID PRIMARY KEY,  
 origem VARCHAR(20), \-- CAMARA / SENADO  
 tipo_execucao VARCHAR(20), \-- FULL / INCREMENTAL  
 iniciado_em TIMESTAMP,  
 finalizado_em TIMESTAMP,  
 total_processado INT,  
 total_inserido INT,  
 total_atualizado INT,  
 total_ignorados INT,  
 status VARCHAR(50)  
);

Permite:

- Auditoria
- Métricas
- Diagnóstico
- Reprocessamento seletivo

---

# 6️⃣ Rate Limit Dinâmico (Especialmente para Senado)

O Senado Federal impõe limite de ~10 req/s.

Implementaremos:

## 6.1 Estratégia

- Token Bucket dinâmico
- Backoff exponencial
- Ajuste adaptativo baseado em resposta HTTP

---

## 6.2 Comportamento Adaptativo

| HTTP Code | Ação                      |
| --------- | ------------------------- |
| 200       | Aumenta gradualmente taxa |
| 429       | Reduz taxa imediatamente  |
| 5xx       | Reduz e ativa retry       |

---

## 6.3 Implementação Técnica

### Dependência:

- Resilience4j RateLimiter
- Retry
- Bulkhead

### Configuração Base:

YAML

resilience4j.ratelimiter:  
 instances:  
 senadoApi:  
 limitForPeriod: 8  
 limitRefreshPeriod: 1s  
 timeoutDuration: 500ms

---

## 6.4 Rate Limit Dinâmico

Criar componente customizado:

Java

public class AdaptiveRateLimiter {

    private volatile int currentLimit \= 8;

    public void adjustOnSuccess() {
        currentLimit \= Math.min(currentLimit + 1, 10);
    }

    public void adjustOnTooManyRequests() {
        currentLimit \= Math.max(currentLimit \- 2, 2);
    }

}

---

# 7️⃣ Execução Paralela Multi-Thread

## 7.1 Objetivo

- Paralelizar por ano
- Paralelizar por página (Senado)
- Paralelizar parsing CSV

Sem violar:

- Limites de API
- Integridade transacional

---

# 8️⃣ Configuração do Executor

Java

@Bean  
public Executor etlExecutor() {  
 ThreadPoolTaskExecutor executor \= new ThreadPoolTaskExecutor();  
 executor.setCorePoolSize(8);  
 executor.setMaxPoolSize(16);  
 executor.setQueueCapacity(200);  
 executor.setThreadNamePrefix("etl-worker-");  
 executor.initialize();  
 return executor;  
}

---

# 9️⃣ Estratégias de Paralelização

## 9.1 Câmara (CSV)

- Dividir CSV em chunks de 10.000 linhas
- Processar cada chunk em paralelo
- Inserção batch por chunk

---

## 9.2 Senado (REST)

Paralelização por:

- Ano
- Página

Exemplo:

Java

years.parallelStream().forEach(this::processYear);

OU

Java

CompletableFuture.supplyAsync(() -> processPage(page), executor);

---

# 🔟 Controle de Concorrência no Banco

## Estratégias:

- Upsert com `ON CONFLICT`
- Transações curtas
- Batch size = 500–1000
- Desabilitar flush automático

Exemplo:

SQL

INSERT INTO proposicao (...)  
VALUES (...)  
ON CONFLICT (casa, sigla, numero, ano)  
DO UPDATE SET ...

---

# 11️⃣ Proteção contra Condição de Corrida

- Lock lógico por ano:

SQL

CREATE TABLE etl_lock (  
 recurso VARCHAR(50) PRIMARY KEY,  
 locked_at TIMESTAMP  
);

Antes de processar ano 2024:

- Tentar inserir lock
- Se falhar → já em execução

---

# 12️⃣ Modo de Execução

| Modo             | Paralelismo | Rate Limit  |
| ---------------- | ----------- | ----------- |
| FULL LOAD        | Alto        | Conservador |
| INCREMENTAL      | Médio       | Adaptativo  |
| FORCED REPROCESS | Controlado  | Conservador |

---

# 13️⃣ Observabilidade

Adicionar métricas:

- etl_ingestao_ignorados_total
- etl_reprocessamentos_forcados_total
- etl_taxa_req_por_segundo
- etl_threads_ativas

---

# 14️⃣ Fluxo Final Consolidado

1.  Scheduler inicia Job
2.  Cria registro em `etl_job_control`
3.  Verifica controle de ingestão
4.  Aplica rate limit adaptativo
5.  Executa paralelização controlada
6.  Realiza UPSERT idempotente
7.  Atualiza métricas
8.  Finaliza job

---

# 15️⃣ Benefícios Arquiteturais

✔ Idempotência garantida  
✔ Zero duplicação  
✔ Reprocessamento controlado  
✔ Alta performance  
✔ Respeito aos limites institucionais  
✔ Escalável horizontalmente  
✔ Auditável
