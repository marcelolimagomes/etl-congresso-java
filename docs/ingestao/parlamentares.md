# Plano de Ingestão — Dados de Parlamentares (Silver Layer)

**Última atualização:** Julho 2025  
**Status:** Planejamento  
**Flyway a partir de:** V30

---

## 1. Objetivo

Ingerir **todos os endpoints** relacionados a dados de parlamentares da Câmara dos Deputados e do Senado Federal na camada Silver, seguindo o padrão já estabelecido no projeto para proposições/matérias:

- Uma tabela por endpoint/CSV
- Colunas espelhando exatamente a estrutura da fonte
- CSV-first para a Câmara (complemento via API quando não há CSV)
- API-only para o Senado
- Mesmo padrão de colunas de controle (`id UUID`, `etl_job_id`, `content_hash`, `ingerido_em`, `atualizado_em`, `origem_carga`, `gold_sincronizado`)

---

## 2. Inventário de Fontes

### 2.1. Câmara dos Deputados — CSV (carga principal)

| #   | Dataset / Arquivo                    | URL de Download                                                                                               | Periodicidade |
| --- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------- | ------------- |
| C1  | `deputados.csv`                      | `https://dadosabertos.camara.leg.br/arquivos/deputados/csv/deputados.csv`                                     | Diária        |
| C2  | `deputadosProfissoes.csv`            | `https://dadosabertos.camara.leg.br/arquivos/deputadosProfissoes/csv/deputadosProfissoes.csv`                 | Diária        |
| C3  | `deputadosOcupacoes.csv`             | `https://dadosabertos.camara.leg.br/arquivos/deputadosOcupacoes/csv/deputadosOcupacoes.csv`                   | Diária        |
| C4  | `orgaosDeputados-L{leg}.csv`         | `https://dadosabertos.camara.leg.br/arquivos/orgaosDeputados/csv/orgaosDeputados-L{leg}.csv`                  | Diária        |
| C5  | `frentesDeputados.csv`               | `https://dadosabertos.camara.leg.br/arquivos/frentesDeputados/csv/frentesDeputados.csv`                       | Diária        |
| C6  | `eventosPresencaDeputados-{ano}.csv` | `https://dadosabertos.camara.leg.br/arquivos/eventosPresencaDeputados/csv/eventosPresencaDeputados-{ano}.csv` | Diária        |
| C7  | `Ano-{ano}.csv` (CEAP Despesas)      | `http://www.camara.leg.br/cotas/Ano-{ano}.csv.zip`                                                            | Diária        |
| C8  | `legislaturasMesas.csv`              | `https://dadosabertos.camara.leg.br/arquivos/legislaturasMesas/csv/legislaturasMesas.csv`                     | Diária        |
| C9  | `gruposMembros.csv`                  | `https://dadosabertos.camara.leg.br/arquivos/gruposMembros/csv/gruposMembros.csv`                             | Diária        |

### 2.2. Câmara dos Deputados — API (complemento, sem CSV equivalente)

| #   | Endpoint                               | Descrição                                                         |
| --- | -------------------------------------- | ----------------------------------------------------------------- |
| A1  | `GET /deputados/{id}`                  | Detalhe completo (CPF, nascimento, escolaridade, gabinete, redes) |
| A2  | `GET /deputados/{id}/discursos`        | Discursos proferidos                                              |
| A3  | `GET /deputados/{id}/eventos`          | Eventos que o deputado participou                                 |
| A4  | `GET /deputados/{id}/historico`        | Histórico de alterações de mandato                                |
| A5  | `GET /deputados/{id}/mandatosExternos` | Mandatos em outras esferas                                        |

> **Nota:** Os endpoints `GET /deputados/{id}/frentes`, `/orgaos`, `/ocupacoes`, `/profissoes` e `/despesas` são redundantes com os CSVs C5, C4, C3, C2 e C7 respectivamente. A API serve como fonte incremental quando o CSV já foi carregado.

### 2.3. Senado Federal — API (16 endpoints ativos)

| #   | Endpoint                                        | Descrição                              |
| --- | ----------------------------------------------- | -------------------------------------- |
| S1  | `GET /senador/lista/atual`                      | Lista de senadores em exercício        |
| S2  | `GET /senador/lista/legislatura/{leg}`          | Senadores por legislatura              |
| S3  | `GET /senador/lista/legislatura/{inicio}/{fim}` | Senadores em intervalo de legislaturas |
| S4  | `GET /senador/afastados`                        | Senadores afastados                    |
| S5  | `GET /senador/partidos`                         | Partidos com representação             |
| S6  | `GET /senador/lista/tiposUsoPalavra`            | Tipos de uso da palavra (referência)   |
| S7  | `GET /senador/{codigo}`                         | Detalhes completos do senador          |
| S8  | `GET /senador/{codigo}/profissao`               | Profissões do senador                  |
| S9  | `GET /senador/{codigo}/mandatos`                | Mandatos do senador                    |
| S10 | `GET /senador/{codigo}/licencas`                | Licenças do senador                    |
| S11 | `GET /senador/{codigo}/historicoAcademico`      | Formação acadêmica                     |
| S12 | `GET /senador/{codigo}/filiacoes`               | Filiações partidárias                  |
| S13 | `GET /senador/{codigo}/discursos`               | Discursos do senador                   |
| S14 | `GET /senador/{codigo}/comissoes`               | Participação em comissões              |
| S15 | `GET /senador/{codigo}/cargos`                  | Cargos ocupados                        |
| S16 | `GET /senador/{codigo}/apartes`                 | Apartes realizados                     |

### 2.4. Senado Federal — Endpoints Deprecados (NÃO implementar)

| Endpoint                           | Sunset     | Endpoint Substituto                      |
| ---------------------------------- | ---------- | ---------------------------------------- |
| `GET /senador/{codigo}/votacoes`   | 01/02/2026 | `GET /dadosabertos/votacao`              |
| `GET /senador/{codigo}/relatorias` | 01/02/2026 | `GET /dadosabertos/processo/relatoria`   |
| `GET /senador/{codigo}/liderancas` | 01/02/2026 | `GET /dadosabertos/composicao/lideranca` |
| `GET /senador/{codigo}/autorias`   | 01/02/2026 | `GET /dadosabertos/processo`             |

> **Decisão:** Os 4 endpoints deprecados **não serão implementados**. Quando os dados de votações, relatorias, lideranças e autorias de senadores forem necessários, devem ser ingeridos a partir dos **endpoints substitutos** (que pertencem a domínios próprios: votação, processo, composição).

---

## 3. Definição de Tabelas Silver

### Convenções

- Schema: `silver`
- Prefixo Câmara: `camara_deputado_*` (principal: `camara_deputado`)
- Prefixo Senado: `senado_senador_*` (principal: `senado_senador`)
- PK: `id UUID` gerado automaticamente
- Colunas de controle: `etl_job_id`, `content_hash`, `ingerido_em`, `atualizado_em`, `origem_carga`, `gold_sincronizado`
- Chave natural (unique constraint): espelha o identificador da fonte

---

### 3.1. Câmara — Tabelas CSV

#### C1 · `silver.camara_deputado`

**Fonte:** `deputados.csv`  
**Chave natural:** `camara_id`  
**Complemento API:** `GET /deputados/{id}` (campos `det_*`)

Colunas do CSV:

| Coluna                 | Tipo           | Descrição                         |
| ---------------------- | -------------- | --------------------------------- |
| `camara_id`            | `VARCHAR(20)`  | ID numérico do deputado na Câmara |
| `uri`                  | `VARCHAR(500)` | URI no Dados Abertos              |
| `nome_civil`           | `VARCHAR(300)` | Nome civil completo               |
| `nome_parlamentar`     | `VARCHAR(300)` | Nome parlamentar                  |
| `nome_eleitoral`       | `VARCHAR(300)` | Nome eleitoral                    |
| `sexo`                 | `VARCHAR(10)`  | Sexo                              |
| `data_nascimento`      | `VARCHAR(30)`  | Data de nascimento (texto fonte)  |
| `data_falecimento`     | `VARCHAR(30)`  | Data de falecimento (texto fonte) |
| `uf_nascimento`        | `VARCHAR(5)`   | UF de nascimento                  |
| `municipio_nascimento` | `VARCHAR(200)` | Município de nascimento           |
| `cpf`                  | `VARCHAR(20)`  | CPF                               |
| `escolaridade`         | `VARCHAR(100)` | Escolaridade                      |
| `url_website`          | `VARCHAR(500)` | Website pessoal                   |
| `url_foto`             | `VARCHAR(500)` | URL da foto                       |
| `primeira_legislatura` | `VARCHAR(10)`  | Primeira legislatura              |
| `ultima_legislatura`   | `VARCHAR(10)`  | Última legislatura                |

Colunas de complemento API (`GET /deputados/{id}`):

| Coluna                          | Tipo           | Descrição                              |
| ------------------------------- | -------------- | -------------------------------------- |
| `det_rede_social`               | `TEXT`         | Redes sociais (JSON array serializado) |
| `det_status_id`                 | `VARCHAR(20)`  | ultimoStatus.id                        |
| `det_status_id_legislatura`     | `VARCHAR(10)`  | ultimoStatus.idLegislatura             |
| `det_status_nome`               | `VARCHAR(300)` | ultimoStatus.nome                      |
| `det_status_nome_eleitoral`     | `VARCHAR(300)` | ultimoStatus.nomeEleitoral             |
| `det_status_sigla_partido`      | `VARCHAR(20)`  | ultimoStatus.siglaPartido              |
| `det_status_sigla_uf`           | `VARCHAR(5)`   | ultimoStatus.siglaUf                   |
| `det_status_email`              | `VARCHAR(200)` | ultimoStatus.email                     |
| `det_status_situacao`           | `VARCHAR(50)`  | ultimoStatus.situacao                  |
| `det_status_condicao_eleitoral` | `VARCHAR(50)`  | ultimoStatus.condicaoEleitoral         |
| `det_status_descricao`          | `VARCHAR(500)` | ultimoStatus.descricaoStatus           |
| `det_status_data`               | `VARCHAR(30)`  | ultimoStatus.data                      |
| `det_status_uri_partido`        | `VARCHAR(500)` | ultimoStatus.uriPartido                |
| `det_status_url_foto`           | `VARCHAR(500)` | ultimoStatus.urlFoto                   |
| `det_gabinete_nome`             | `VARCHAR(100)` | ultimoStatus.gabinete.nome             |
| `det_gabinete_predio`           | `VARCHAR(100)` | ultimoStatus.gabinete.predio           |
| `det_gabinete_sala`             | `VARCHAR(20)`  | ultimoStatus.gabinete.sala             |
| `det_gabinete_andar`            | `VARCHAR(20)`  | ultimoStatus.gabinete.andar            |
| `det_gabinete_telefone`         | `VARCHAR(30)`  | ultimoStatus.gabinete.telefone         |
| `det_gabinete_email`            | `VARCHAR(200)` | ultimoStatus.gabinete.email            |

---

#### C2 · `silver.camara_deputado_profissao`

**Fonte:** `deputadosProfissoes.csv` / `GET /deputados/{id}/profissoes`  
**Chave natural:** `(camara_deputado_id, titulo, cod_tipo_profissao)`

| Coluna               | Tipo           | Descrição                   |
| -------------------- | -------------- | --------------------------- |
| `camara_deputado_id` | `VARCHAR(20)`  | ID do deputado              |
| `titulo`             | `VARCHAR(300)` | Título da profissão         |
| `cod_tipo_profissao` | `INTEGER`      | Código do tipo de profissão |
| `data_hora`          | `VARCHAR(30)`  | Data/hora do registro       |

---

#### C3 · `silver.camara_deputado_ocupacao`

**Fonte:** `deputadosOcupacoes.csv` / `GET /deputados/{id}/ocupacoes`  
**Chave natural:** `(camara_deputado_id, titulo, ano_inicio)`

| Coluna               | Tipo           | Descrição            |
| -------------------- | -------------- | -------------------- |
| `camara_deputado_id` | `VARCHAR(20)`  | ID do deputado       |
| `titulo`             | `VARCHAR(300)` | Título da ocupação   |
| `ano_inicio`         | `INTEGER`      | Ano de início        |
| `ano_fim`            | `INTEGER`      | Ano de fim           |
| `entidade`           | `VARCHAR(300)` | Entidade empregadora |
| `entidade_uf`        | `VARCHAR(5)`   | UF da entidade       |
| `entidade_pais`      | `VARCHAR(100)` | País da entidade     |

---

#### C4 · `silver.camara_deputado_orgao`

**Fonte:** `orgaosDeputados-L{leg}.csv` / `GET /deputados/{id}/orgaos`  
**Chave natural:** `(camara_deputado_id, id_orgao, data_inicio)`

| Coluna               | Tipo           | Descrição             |
| -------------------- | -------------- | --------------------- |
| `camara_deputado_id` | `VARCHAR(20)`  | ID do deputado        |
| `id_orgao`           | `INTEGER`      | ID do órgão           |
| `sigla_orgao`        | `VARCHAR(30)`  | Sigla do órgão        |
| `nome_orgao`         | `VARCHAR(300)` | Nome do órgão         |
| `nome_publicacao`    | `VARCHAR(300)` | Nome de publicação    |
| `titulo`             | `VARCHAR(200)` | Cargo/título no órgão |
| `cod_titulo`         | `VARCHAR(20)`  | Código do título      |
| `data_inicio`        | `VARCHAR(30)`  | Data de início        |
| `data_fim`           | `VARCHAR(30)`  | Data de fim           |
| `uri_orgao`          | `VARCHAR(500)` | URI do órgão          |

---

#### C5 · `silver.camara_deputado_frente`

**Fonte:** `frentesDeputados.csv` / `GET /deputados/{id}/frentes`  
**Chave natural:** `(camara_deputado_id, id_frente)`

| Coluna               | Tipo           | Descrição                |
| -------------------- | -------------- | ------------------------ |
| `camara_deputado_id` | `VARCHAR(20)`  | ID do deputado           |
| `id_frente`          | `INTEGER`      | ID da frente parlamentar |
| `id_legislatura`     | `INTEGER`      | Legislatura da frente    |
| `titulo`             | `VARCHAR(500)` | Título da frente         |
| `uri`                | `VARCHAR(500)` | URI da frente            |

---

#### C6 · `silver.camara_deputado_presenca_evento`

**Fonte:** `eventosPresencaDeputados-{ano}.csv`  
**Chave natural:** `(camara_deputado_id, id_evento)`

| Coluna               | Tipo           | Descrição                     |
| -------------------- | -------------- | ----------------------------- |
| `camara_deputado_id` | `VARCHAR(20)`  | ID do deputado                |
| `id_evento`          | `INTEGER`      | ID do evento                  |
| `data_hora_inicio`   | `VARCHAR(30)`  | Data/hora de início do evento |
| `data_hora_fim`      | `VARCHAR(30)`  | Data/hora de fim do evento    |
| `descricao`          | `TEXT`         | Descrição do evento           |
| `descricao_tipo`     | `VARCHAR(100)` | Tipo do evento                |
| `situacao`           | `VARCHAR(50)`  | Situação do evento            |
| `uri_evento`         | `VARCHAR(500)` | URI do evento                 |

> **Nota:** Este CSV traz a junção evento + presença. Os campos do evento vêm do CSV; a existência do registro indica presença.

---

#### C7 · `silver.camara_despesa`

**Fonte:** `Ano-{ano}.csv` (Cota Parlamentar — CEAP)  
**Chave natural:** `(camara_deputado_id, cod_documento, num_documento, parcela)`

| Coluna                | Tipo           | Descrição                        |
| --------------------- | -------------- | -------------------------------- |
| `camara_deputado_id`  | `VARCHAR(20)`  | ID do deputado                   |
| `ano`                 | `INTEGER`      | Ano da despesa                   |
| `mes`                 | `INTEGER`      | Mês da despesa                   |
| `tipo_despesa`        | `VARCHAR(300)` | Categoria da despesa             |
| `cod_documento`       | `INTEGER`      | Código do documento              |
| `tipo_documento`      | `VARCHAR(100)` | Tipo do documento fiscal         |
| `cod_tipo_documento`  | `INTEGER`      | Código do tipo do documento      |
| `data_documento`      | `VARCHAR(30)`  | Data do documento                |
| `num_documento`       | `VARCHAR(50)`  | Número do documento              |
| `parcela`             | `INTEGER`      | Parcela                          |
| `valor_documento`     | `VARCHAR(30)`  | Valor do documento (texto fonte) |
| `valor_glosa`         | `VARCHAR(30)`  | Valor da glosa                   |
| `valor_liquido`       | `VARCHAR(30)`  | Valor líquido                    |
| `nome_fornecedor`     | `VARCHAR(300)` | Nome do fornecedor               |
| `cnpj_cpf_fornecedor` | `VARCHAR(20)`  | CNPJ/CPF do fornecedor           |
| `num_ressarcimento`   | `VARCHAR(50)`  | Número do ressarcimento          |
| `url_documento`       | `VARCHAR(500)` | URL do documento                 |
| `cod_lote`            | `INTEGER`      | Código do lote                   |

> **Nota sobre valores:** Os campos `valor_*` são armazenados como `VARCHAR` (Silver = sem transformação). Conversão para NUMERIC ocorre na camada Gold.

---

#### C8 · `silver.camara_mesa_diretora`

**Fonte:** `legislaturasMesas.csv`  
**Chave natural:** `(camara_deputado_id, id_legislatura, titulo, data_inicio)`

| Coluna               | Tipo           | Descrição              |
| -------------------- | -------------- | ---------------------- |
| `camara_deputado_id` | `VARCHAR(20)`  | ID do deputado         |
| `id_legislatura`     | `INTEGER`      | Legislatura            |
| `titulo`             | `VARCHAR(200)` | Cargo na mesa diretora |
| `data_inicio`        | `VARCHAR(30)`  | Data de início         |
| `data_fim`           | `VARCHAR(30)`  | Data de fim            |

---

#### C9 · `silver.camara_grupo_membro`

**Fonte:** `gruposMembros.csv`  
**Chave natural:** `(camara_deputado_id, id_grupo, data_inicio)`

| Coluna               | Tipo           | Descrição                      |
| -------------------- | -------------- | ------------------------------ |
| `camara_deputado_id` | `VARCHAR(20)`  | ID do deputado                 |
| `id_grupo`           | `INTEGER`      | ID do grupo parlamentar        |
| `nome_parlamentar`   | `VARCHAR(300)` | Nome do parlamentar            |
| `uri`                | `VARCHAR(500)` | URI do deputado                |
| `titulo`             | `VARCHAR(200)` | Cargo na mesa do grupo         |
| `data_inicio`        | `VARCHAR(30)`  | Data de início da participação |
| `data_fim`           | `VARCHAR(30)`  | Data de fim da participação    |

---

### 3.2. Câmara — Tabelas API-Only (sem CSV equivalente)

#### A2 · `silver.camara_deputado_discurso`

**Fonte:** `GET /deputados/{id}/discursos`  
**Chave natural:** `(camara_deputado_id, data_hora_inicio, tipo_discurso)`

| Coluna                         | Tipo           | Descrição                |
| ------------------------------ | -------------- | ------------------------ |
| `camara_deputado_id`           | `VARCHAR(20)`  | ID do deputado           |
| `data_hora_inicio`             | `VARCHAR(30)`  | Data/hora de início      |
| `data_hora_fim`                | `VARCHAR(30)`  | Data/hora de fim         |
| `tipo_discurso`                | `VARCHAR(100)` | Tipo do discurso         |
| `sumario`                      | `TEXT`         | Sumário                  |
| `transcricao`                  | `TEXT`         | Transcrição completa     |
| `keywords`                     | `TEXT`         | Palavras-chave           |
| `url_texto`                    | `VARCHAR(500)` | URL do texto integral    |
| `url_audio`                    | `VARCHAR(500)` | URL do áudio             |
| `url_video`                    | `VARCHAR(500)` | URL do vídeo             |
| `uri_evento`                   | `VARCHAR(500)` | URI do evento associado  |
| `fase_evento_titulo`           | `VARCHAR(300)` | Título da fase do evento |
| `fase_evento_data_hora_inicio` | `VARCHAR(30)`  | Data/hora início da fase |
| `fase_evento_data_hora_fim`    | `VARCHAR(30)`  | Data/hora fim da fase    |

---

#### A3 · `silver.camara_deputado_evento`

**Fonte:** `GET /deputados/{id}/eventos`  
**Chave natural:** `(camara_deputado_id, id_evento)`

| Coluna                | Tipo           | Descrição                                    |
| --------------------- | -------------- | -------------------------------------------- |
| `camara_deputado_id`  | `VARCHAR(20)`  | ID do deputado                               |
| `id_evento`           | `INTEGER`      | ID do evento                                 |
| `data_hora_inicio`    | `VARCHAR(30)`  | Data/hora de início                          |
| `data_hora_fim`       | `VARCHAR(30)`  | Data/hora de fim                             |
| `descricao`           | `TEXT`         | Descrição do evento                          |
| `descricao_tipo`      | `VARCHAR(100)` | Tipo do evento                               |
| `situacao`            | `VARCHAR(50)`  | Situação                                     |
| `local_externo`       | `VARCHAR(300)` | Local externo                                |
| `uri`                 | `VARCHAR(500)` | URI do evento                                |
| `url_registro`        | `VARCHAR(500)` | URL de registro                              |
| `local_camara_nome`   | `VARCHAR(200)` | Nome do local na Câmara                      |
| `local_camara_predio` | `VARCHAR(100)` | Prédio                                       |
| `local_camara_sala`   | `VARCHAR(50)`  | Sala                                         |
| `local_camara_andar`  | `VARCHAR(20)`  | Andar                                        |
| `orgaos`              | `TEXT`         | Órgãos realizadores (JSON array serializado) |

> **Nota sobre C6 vs A3:** O CSV `eventosPresencaDeputados` traz presença **confirmada** por ano. A API `GET /deputados/{id}/eventos` traz **todos** os eventos com maior riqueza de dados (local, órgãos, situação). Os dois são complementares; C6 é a fonte autoritativa para presença, A3 para metadados de eventos. Se apenas um for implementado inicialmente, preferir C6 (presença).

---

#### A4 · `silver.camara_deputado_historico`

**Fonte:** `GET /deputados/{id}/historico`  
**Chave natural:** `(camara_deputado_id, data_hora, id_legislatura)`

| Coluna               | Tipo           | Descrição                        |
| -------------------- | -------------- | -------------------------------- |
| `camara_deputado_id` | `VARCHAR(20)`  | ID do deputado                   |
| `camara_id_registro` | `VARCHAR(20)`  | ID do registro de status         |
| `id_legislatura`     | `INTEGER`      | Legislatura do registro          |
| `nome`               | `VARCHAR(300)` | Nome parlamentar naquele período |
| `nome_eleitoral`     | `VARCHAR(300)` | Nome eleitoral naquele período   |
| `email`              | `VARCHAR(200)` | E-mail                           |
| `sigla_partido`      | `VARCHAR(20)`  | Partido                          |
| `sigla_uf`           | `VARCHAR(5)`   | UF de representação              |
| `situacao`           | `VARCHAR(50)`  | Situação                         |
| `condicao_eleitoral` | `VARCHAR(50)`  | Condição eleitoral               |
| `descricao_status`   | `VARCHAR(500)` | Descrição do status              |
| `data_hora`          | `VARCHAR(30)`  | Data/hora do registro            |
| `uri_partido`        | `VARCHAR(500)` | URI do partido                   |
| `url_foto`           | `VARCHAR(500)` | URL da foto                      |

---

#### A5 · `silver.camara_deputado_mandato_externo`

**Fonte:** `GET /deputados/{id}/mandatosExternos`  
**Chave natural:** `(camara_deputado_id, cargo, sigla_uf, ano_inicio)`

| Coluna                  | Tipo           | Descrição          |
| ----------------------- | -------------- | ------------------ |
| `camara_deputado_id`    | `VARCHAR(20)`  | ID do deputado     |
| `ano_inicio`            | `VARCHAR(10)`  | Ano de início      |
| `ano_fim`               | `VARCHAR(10)`  | Ano de fim         |
| `cargo`                 | `VARCHAR(200)` | Cargo exercido     |
| `sigla_uf`              | `VARCHAR(5)`   | UF                 |
| `municipio`             | `VARCHAR(200)` | Município          |
| `sigla_partido_eleicao` | `VARCHAR(20)`  | Partido na eleição |
| `uri_partido_eleicao`   | `VARCHAR(500)` | URI do partido     |

---

### 3.3. Senado — Tabelas de Listagem

#### S1 · `silver.senado_senador`

**Fonte:** `GET /senador/lista/atual` + `GET /senador/lista/legislatura/{leg}`  
**Chave natural:** `codigo_senador`  
**Complemento API:** `GET /senador/{codigo}` (campos `det_*`)

Colunas da listagem:

| Coluna                      | Tipo           | Descrição                                         |
| --------------------------- | -------------- | ------------------------------------------------- |
| `codigo_senador`            | `VARCHAR(20)`  | Código do senador                                 |
| `nome_parlamentar`          | `VARCHAR(300)` | Nome parlamentar                                  |
| `nome_civil`                | `VARCHAR(300)` | Nome civil                                        |
| `sexo`                      | `VARCHAR(5)`   | Sexo (M/F)                                        |
| `uf_parlamentar`            | `VARCHAR(5)`   | UF                                                |
| `participacao`              | `VARCHAR(5)`   | T=Titular / S=Suplente                            |
| `partido_parlamentar`       | `VARCHAR(100)` | Nome do partido                                   |
| `sigla_partido_parlamentar` | `VARCHAR(20)`  | Sigla do partido                                  |
| `data_designacao`           | `VARCHAR(30)`  | Data de designação                                |
| `codigo_legislatura`        | `VARCHAR(10)`  | Legislatura (preenchida na carga por legislatura) |

Colunas de complemento API (`GET /senador/{codigo}`):

| Coluna                       | Tipo           | Descrição                           |
| ---------------------------- | -------------- | ----------------------------------- |
| `det_nome_completo`          | `VARCHAR(300)` | Nome completo                       |
| `det_data_nascimento`        | `VARCHAR(30)`  | Data de nascimento                  |
| `det_local_nascimento`       | `VARCHAR(200)` | Local de nascimento                 |
| `det_estado_civil`           | `VARCHAR(50)`  | Estado civil                        |
| `det_escolaridade`           | `VARCHAR(100)` | Escolaridade                        |
| `det_contato_email`          | `VARCHAR(200)` | E-mail de contato                   |
| `det_url_foto`               | `VARCHAR(500)` | URL da foto                         |
| `det_url_pagina_parlamentar` | `VARCHAR(500)` | URL da página parlamentar           |
| `det_pagina`                 | `VARCHAR(500)` | Página pessoal                      |
| `det_facebook`               | `VARCHAR(500)` | Facebook                            |
| `det_twitter`                | `VARCHAR(500)` | Twitter                             |
| `det_profissoes`             | `TEXT`         | Profissões (JSON array serializado) |

---

#### S4 · `silver.senado_senador_afastado`

**Fonte:** `GET /senador/afastados`  
**Chave natural:** `(codigo_senador, data_afastamento)`

| Coluna                     | Tipo           | Descrição                  |
| -------------------------- | -------------- | -------------------------- |
| `codigo_senador`           | `VARCHAR(20)`  | Código do senador          |
| `nome_parlamentar`         | `VARCHAR(300)` | Nome parlamentar           |
| `uf_mandato`               | `VARCHAR(5)`   | UF do mandato              |
| `motivo_afastamento`       | `VARCHAR(300)` | Motivo do afastamento      |
| `data_afastamento`         | `VARCHAR(30)`  | Data do afastamento        |
| `data_termino_afastamento` | `VARCHAR(30)`  | Data de término (nullable) |

---

#### S5 · `silver.senado_partido`

**Fonte:** `GET /senador/partidos`  
**Chave natural:** `codigo_partido`

| Coluna             | Tipo           | Descrição                      |
| ------------------ | -------------- | ------------------------------ |
| `codigo_partido`   | `VARCHAR(20)`  | Código do partido              |
| `sigla_partido`    | `VARCHAR(20)`  | Sigla                          |
| `nome_partido`     | `VARCHAR(200)` | Nome completo                  |
| `data_ativacao`    | `VARCHAR(30)`  | Data de ativação               |
| `data_desativacao` | `VARCHAR(30)`  | Data de desativação (nullable) |

---

#### S6 · `silver.senado_tipo_uso_palavra`

**Fonte:** `GET /senador/lista/tiposUsoPalavra`  
**Chave natural:** `codigo_tipo`

| Coluna           | Tipo           | Descrição      |
| ---------------- | -------------- | -------------- |
| `codigo_tipo`    | `VARCHAR(20)`  | Código do tipo |
| `descricao_tipo` | `VARCHAR(200)` | Descrição      |
| `abreviatura`    | `VARCHAR(20)`  | Abreviatura    |

---

### 3.4. Senado — Tabelas por Senador (sub-recursos)

#### S8 · `silver.senado_senador_profissao`

**Fonte:** `GET /senador/{codigo}/profissao`  
**Chave natural:** `(codigo_senador, codigo_profissao)`

| Coluna                | Tipo           | Descrição           |
| --------------------- | -------------- | ------------------- |
| `codigo_senador`      | `VARCHAR(20)`  | Código do senador   |
| `codigo_profissao`    | `VARCHAR(20)`  | Código da profissão |
| `descricao_profissao` | `VARCHAR(300)` | Descrição           |
| `data_registro`       | `VARCHAR(30)`  | Data de registro    |

---

#### S9 · `silver.senado_senador_mandato`

**Fonte:** `GET /senador/{codigo}/mandatos`  
**Chave natural:** `(codigo_senador, codigo_mandato)`

| Coluna             | Tipo           | Descrição                    |
| ------------------ | -------------- | ---------------------------- |
| `codigo_senador`   | `VARCHAR(20)`  | Código do senador            |
| `codigo_mandato`   | `VARCHAR(20)`  | Código do mandato            |
| `descricao`        | `VARCHAR(300)` | Descrição                    |
| `uf_mandato`       | `VARCHAR(5)`   | UF                           |
| `participacao`     | `VARCHAR(5)`   | T=Titular / S=Suplente       |
| `data_inicio`      | `VARCHAR(30)`  | Data de início               |
| `data_fim`         | `VARCHAR(30)`  | Data de fim                  |
| `data_designacao`  | `VARCHAR(30)`  | Data de designação           |
| `data_termino`     | `VARCHAR(30)`  | Data de término              |
| `entrou_exercicio` | `VARCHAR(5)`   | S/N                          |
| `data_exercicio`   | `VARCHAR(30)`  | Data de exercício (nullable) |

---

#### S10 · `silver.senado_senador_licenca`

**Fonte:** `GET /senador/{codigo}/licencas`  
**Chave natural:** `(codigo_senador, codigo_licenca)`

| Coluna             | Tipo           | Descrição           |
| ------------------ | -------------- | ------------------- |
| `codigo_senador`   | `VARCHAR(20)`  | Código do senador   |
| `codigo_licenca`   | `VARCHAR(20)`  | Código da licença   |
| `data_inicio`      | `VARCHAR(30)`  | Data de início      |
| `data_fim`         | `VARCHAR(30)`  | Data de fim         |
| `motivo`           | `VARCHAR(100)` | Motivo (código)     |
| `descricao_motivo` | `VARCHAR(300)` | Descrição do motivo |

---

#### S11 · `silver.senado_senador_historico_academico`

**Fonte:** `GET /senador/{codigo}/historicoAcademico`  
**Chave natural:** `(codigo_senador, codigo_curso)`

| Coluna                  | Tipo           | Descrição                                    |
| ----------------------- | -------------- | -------------------------------------------- |
| `codigo_senador`        | `VARCHAR(20)`  | Código do senador                            |
| `codigo_curso`          | `VARCHAR(20)`  | Código do curso                              |
| `nome_curso`            | `VARCHAR(300)` | Nome do curso                                |
| `instituicao`           | `VARCHAR(300)` | Código/nome da instituição                   |
| `descricao_instituicao` | `VARCHAR(300)` | Descrição da instituição                     |
| `nivel_formacao`        | `VARCHAR(100)` | Nível (Graduação, Mestrado, Doutorado, etc.) |
| `data_inicio_formacao`  | `VARCHAR(30)`  | Data de início (nullable)                    |
| `data_termino_formacao` | `VARCHAR(30)`  | Data de término (nullable)                   |
| `concluido`             | `VARCHAR(5)`   | S/N                                          |

---

#### S12 · `silver.senado_senador_filiacao`

**Fonte:** `GET /senador/{codigo}/filiacoes`  
**Chave natural:** `(codigo_senador, codigo_filiacao)`

| Coluna                  | Tipo           | Descrição                  |
| ----------------------- | -------------- | -------------------------- |
| `codigo_senador`        | `VARCHAR(20)`  | Código do senador          |
| `codigo_filiacao`       | `VARCHAR(20)`  | Código da filiação         |
| `codigo_partido`        | `VARCHAR(20)`  | Código do partido          |
| `sigla_partido`         | `VARCHAR(20)`  | Sigla                      |
| `nome_partido`          | `VARCHAR(200)` | Nome completo              |
| `data_inicio_filiacao`  | `VARCHAR(30)`  | Data de início             |
| `data_termino_filiacao` | `VARCHAR(30)`  | Data de término (nullable) |

---

#### S13 · `silver.senado_senador_discurso`

**Fonte:** `GET /senador/{codigo}/discursos`  
**Chave natural:** `(codigo_senador, codigo_discurso)`

| Coluna                | Tipo           | Descrição                |
| --------------------- | -------------- | ------------------------ |
| `codigo_senador`      | `VARCHAR(20)`  | Código do senador        |
| `codigo_discurso`     | `VARCHAR(20)`  | Código do discurso       |
| `codigo_sessao`       | `VARCHAR(20)`  | Código da sessão         |
| `data_pronunciamento` | `VARCHAR(30)`  | Data do pronunciamento   |
| `casa`                | `VARCHAR(10)`  | Casa (SF/CD/CN/PR/CR/AC) |
| `tipo_sessao`         | `VARCHAR(100)` | Tipo da sessão           |
| `numero_sessao`       | `VARCHAR(20)`  | Número da sessão         |
| `tipo_pronunciamento` | `VARCHAR(100)` | Tipo do pronunciamento   |
| `texto_discurso`      | `TEXT`         | Texto integral           |
| `duracao_aparte`      | `VARCHAR(20)`  | Duração do aparte        |
| `url_video`           | `VARCHAR(500)` | URL do vídeo             |
| `url_audio`           | `VARCHAR(500)` | URL do áudio             |

> **Restrição da API:** Intervalo máximo de consulta = 1 ano; padrão 30 dias. A ingestão deve iterar por períodos de 365 dias.

---

#### S14 · `silver.senado_senador_comissao`

**Fonte:** `GET /senador/{codigo}/comissoes`  
**Chave natural:** `(codigo_senador, codigo_comissao, data_inicio_participacao)`

| Coluna                      | Tipo           | Descrição                  |
| --------------------------- | -------------- | -------------------------- |
| `codigo_senador`            | `VARCHAR(20)`  | Código do senador          |
| `codigo_comissao`           | `VARCHAR(20)`  | Código da comissão         |
| `sigla_comissao`            | `VARCHAR(30)`  | Sigla da comissão          |
| `nome_comissao`             | `VARCHAR(300)` | Nome da comissão           |
| `cargo`                     | `VARCHAR(100)` | Cargo na comissão          |
| `data_inicio_participacao`  | `VARCHAR(30)`  | Data de início             |
| `data_termino_participacao` | `VARCHAR(30)`  | Data de término (nullable) |
| `ativo`                     | `VARCHAR(5)`   | S/N                        |

---

#### S15 · `silver.senado_senador_cargo`

**Fonte:** `GET /senador/{codigo}/cargos`  
**Chave natural:** `(codigo_senador, codigo_cargo, data_inicio)`

| Coluna              | Tipo           | Descrição              |
| ------------------- | -------------- | ---------------------- |
| `codigo_senador`    | `VARCHAR(20)`  | Código do senador      |
| `codigo_cargo`      | `VARCHAR(20)`  | Código do cargo        |
| `descricao_cargo`   | `VARCHAR(300)` | Descrição do cargo     |
| `tipo_cargo`        | `VARCHAR(100)` | Tipo do cargo          |
| `comissao_ou_orgao` | `VARCHAR(300)` | Comissão ou órgão      |
| `data_inicio`       | `VARCHAR(30)`  | Data de início         |
| `data_fim`          | `VARCHAR(30)`  | Data de fim (nullable) |

---

#### S16 · `silver.senado_senador_aparte`

**Fonte:** `GET /senador/{codigo}/apartes`  
**Chave natural:** `(codigo_senador, codigo_aparte)`

| Coluna                      | Tipo           | Descrição                       |
| --------------------------- | -------------- | ------------------------------- |
| `codigo_senador`            | `VARCHAR(20)`  | Código do senador               |
| `codigo_aparte`             | `VARCHAR(20)`  | Código do aparte                |
| `codigo_discurso_principal` | `VARCHAR(20)`  | Código do discurso interrompido |
| `codigo_sessao`             | `VARCHAR(20)`  | Código da sessão                |
| `data_pronunciamento`       | `VARCHAR(30)`  | Data do pronunciamento          |
| `casa`                      | `VARCHAR(10)`  | Casa (SF/CD/CN)                 |
| `texto_aparte`              | `TEXT`         | Texto do aparte                 |
| `url_video`                 | `VARCHAR(500)` | URL do vídeo                    |

> **Restrição da API:** Mesmo limite de 1 ano dos discursos.

---

## 4. Flyway Migrations (V30+)

Todas as migrations seguem o padrão existente. Abaixo, exemplo para as primeiras tabelas.

### V30 — Tabelas principais Câmara (deputados + despesas)

```sql
-- V30__create_silver_camara_deputado.sql

CREATE TABLE IF NOT EXISTS silver.camara_deputado (
    id                          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id                  UUID,
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMP,
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado           BOOLEAN NOT NULL DEFAULT FALSE,

    -- CSV
    camara_id                   VARCHAR(20),
    uri                         VARCHAR(500),
    nome_civil                  VARCHAR(300),
    nome_parlamentar            VARCHAR(300),
    nome_eleitoral              VARCHAR(300),
    sexo                        VARCHAR(10),
    data_nascimento             VARCHAR(30),
    data_falecimento            VARCHAR(30),
    uf_nascimento               VARCHAR(5),
    municipio_nascimento        VARCHAR(200),
    cpf                         VARCHAR(20),
    escolaridade                VARCHAR(100),
    url_website                 VARCHAR(500),
    url_foto                    VARCHAR(500),
    primeira_legislatura        VARCHAR(10),
    ultima_legislatura          VARCHAR(10),

    -- Complemento API (GET /deputados/{id})
    det_rede_social             TEXT,
    det_status_id               VARCHAR(20),
    det_status_id_legislatura   VARCHAR(10),
    det_status_nome             VARCHAR(300),
    det_status_nome_eleitoral   VARCHAR(300),
    det_status_sigla_partido    VARCHAR(20),
    det_status_sigla_uf         VARCHAR(5),
    det_status_email            VARCHAR(200),
    det_status_situacao         VARCHAR(50),
    det_status_condicao_eleitoral VARCHAR(50),
    det_status_descricao        VARCHAR(500),
    det_status_data             VARCHAR(30),
    det_status_uri_partido      VARCHAR(500),
    det_status_url_foto         VARCHAR(500),
    det_gabinete_nome           VARCHAR(100),
    det_gabinete_predio         VARCHAR(100),
    det_gabinete_sala           VARCHAR(20),
    det_gabinete_andar          VARCHAR(20),
    det_gabinete_telefone       VARCHAR(30),
    det_gabinete_email          VARCHAR(200),

    CONSTRAINT uq_silver_camara_deputado_camara_id UNIQUE (camara_id)
);

CREATE INDEX idx_silver_camara_deputado_gold ON silver.camara_deputado (gold_sincronizado) WHERE NOT gold_sincronizado;
```

### V31 — Sub-tabelas Câmara (profissão, ocupação, órgão, frente)

```sql
-- V31__create_silver_camara_deputado_subtabelas.sql

CREATE TABLE IF NOT EXISTS silver.camara_deputado_profissao (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id  VARCHAR(20),
    titulo              VARCHAR(300),
    cod_tipo_profissao  INTEGER,
    data_hora           VARCHAR(30),
    CONSTRAINT uq_silver_camara_dep_prof UNIQUE (camara_deputado_id, titulo, cod_tipo_profissao)
);

CREATE TABLE IF NOT EXISTS silver.camara_deputado_ocupacao (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id  VARCHAR(20),
    titulo              VARCHAR(300),
    ano_inicio          INTEGER,
    ano_fim             INTEGER,
    entidade            VARCHAR(300),
    entidade_uf         VARCHAR(5),
    entidade_pais       VARCHAR(100),
    CONSTRAINT uq_silver_camara_dep_ocup UNIQUE (camara_deputado_id, titulo, ano_inicio)
);

CREATE TABLE IF NOT EXISTS silver.camara_deputado_orgao (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id  VARCHAR(20),
    id_orgao            INTEGER,
    sigla_orgao         VARCHAR(30),
    nome_orgao          VARCHAR(300),
    nome_publicacao     VARCHAR(300),
    titulo              VARCHAR(200),
    cod_titulo          VARCHAR(20),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    uri_orgao           VARCHAR(500),
    CONSTRAINT uq_silver_camara_dep_orgao UNIQUE (camara_deputado_id, id_orgao, data_inicio)
);

CREATE TABLE IF NOT EXISTS silver.camara_deputado_frente (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id  VARCHAR(20),
    id_frente           INTEGER,
    id_legislatura      INTEGER,
    titulo              VARCHAR(500),
    uri                 VARCHAR(500),
    CONSTRAINT uq_silver_camara_dep_frente UNIQUE (camara_deputado_id, id_frente)
);
```

### V32 — Presença em eventos + Despesas

```sql
-- V32__create_silver_camara_presenca_despesa.sql

CREATE TABLE IF NOT EXISTS silver.camara_deputado_presenca_evento (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id  VARCHAR(20),
    id_evento           INTEGER,
    data_hora_inicio    VARCHAR(30),
    data_hora_fim       VARCHAR(30),
    descricao           TEXT,
    descricao_tipo      VARCHAR(100),
    situacao            VARCHAR(50),
    uri_evento          VARCHAR(500),
    CONSTRAINT uq_silver_camara_dep_presenca UNIQUE (camara_deputado_id, id_evento)
);

CREATE TABLE IF NOT EXISTS silver.camara_despesa (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id      VARCHAR(20),
    ano                     INTEGER,
    mes                     INTEGER,
    tipo_despesa            VARCHAR(300),
    cod_documento           INTEGER,
    tipo_documento          VARCHAR(100),
    cod_tipo_documento      INTEGER,
    data_documento          VARCHAR(30),
    num_documento           VARCHAR(50),
    parcela                 INTEGER,
    valor_documento         VARCHAR(30),
    valor_glosa             VARCHAR(30),
    valor_liquido           VARCHAR(30),
    nome_fornecedor         VARCHAR(300),
    cnpj_cpf_fornecedor     VARCHAR(20),
    num_ressarcimento       VARCHAR(50),
    url_documento           VARCHAR(500),
    cod_lote                INTEGER,
    CONSTRAINT uq_silver_camara_despesa UNIQUE (camara_deputado_id, cod_documento, num_documento, parcela)
);

CREATE INDEX idx_silver_camara_despesa_ano ON silver.camara_despesa (ano);
CREATE INDEX idx_silver_camara_despesa_gold ON silver.camara_despesa (gold_sincronizado) WHERE NOT gold_sincronizado;
```

### V33 — Mesa diretora + Grupos + Tabelas API-only Câmara

```sql
-- V33__create_silver_camara_mesa_grupo_api.sql

CREATE TABLE IF NOT EXISTS silver.camara_mesa_diretora (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id  VARCHAR(20),
    id_legislatura      INTEGER,
    titulo              VARCHAR(200),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    CONSTRAINT uq_silver_camara_mesa UNIQUE (camara_deputado_id, id_legislatura, titulo, data_inicio)
);

CREATE TABLE IF NOT EXISTS silver.camara_grupo_membro (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'CSV',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id  VARCHAR(20),
    id_grupo            INTEGER,
    nome_parlamentar    VARCHAR(300),
    uri                 VARCHAR(500),
    titulo              VARCHAR(200),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    CONSTRAINT uq_silver_camara_grupo_membro UNIQUE (camara_deputado_id, id_grupo, data_inicio)
);

CREATE TABLE IF NOT EXISTS silver.camara_deputado_discurso (
    id                              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id                      UUID,
    ingerido_em                     TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em                   TIMESTAMP,
    content_hash                    VARCHAR(64),
    origem_carga                    VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado               BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id              VARCHAR(20),
    data_hora_inicio                VARCHAR(30),
    data_hora_fim                   VARCHAR(30),
    tipo_discurso                   VARCHAR(100),
    sumario                         TEXT,
    transcricao                     TEXT,
    keywords                        TEXT,
    url_texto                       VARCHAR(500),
    url_audio                       VARCHAR(500),
    url_video                       VARCHAR(500),
    uri_evento                      VARCHAR(500),
    fase_evento_titulo              VARCHAR(300),
    fase_evento_data_hora_inicio    VARCHAR(30),
    fase_evento_data_hora_fim       VARCHAR(30),
    CONSTRAINT uq_silver_camara_dep_discurso UNIQUE (camara_deputado_id, data_hora_inicio, tipo_discurso)
);

CREATE TABLE IF NOT EXISTS silver.camara_deputado_evento (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id      VARCHAR(20),
    id_evento               INTEGER,
    data_hora_inicio        VARCHAR(30),
    data_hora_fim           VARCHAR(30),
    descricao               TEXT,
    descricao_tipo          VARCHAR(100),
    situacao                VARCHAR(50),
    local_externo           VARCHAR(300),
    uri                     VARCHAR(500),
    url_registro            VARCHAR(500),
    local_camara_nome       VARCHAR(200),
    local_camara_predio     VARCHAR(100),
    local_camara_sala       VARCHAR(50),
    local_camara_andar      VARCHAR(20),
    orgaos                  TEXT,
    CONSTRAINT uq_silver_camara_dep_evento UNIQUE (camara_deputado_id, id_evento)
);

CREATE TABLE IF NOT EXISTS silver.camara_deputado_historico (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id  VARCHAR(20),
    camara_id_registro  VARCHAR(20),
    id_legislatura      INTEGER,
    nome                VARCHAR(300),
    nome_eleitoral      VARCHAR(300),
    email               VARCHAR(200),
    sigla_partido       VARCHAR(20),
    sigla_uf            VARCHAR(5),
    situacao            VARCHAR(50),
    condicao_eleitoral  VARCHAR(50),
    descricao_status    VARCHAR(500),
    data_hora           VARCHAR(30),
    uri_partido         VARCHAR(500),
    url_foto            VARCHAR(500),
    CONSTRAINT uq_silver_camara_dep_historico UNIQUE (camara_deputado_id, data_hora, id_legislatura)
);

CREATE TABLE IF NOT EXISTS silver.camara_deputado_mandato_externo (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    camara_deputado_id      VARCHAR(20),
    ano_inicio              VARCHAR(10),
    ano_fim                 VARCHAR(10),
    cargo                   VARCHAR(200),
    sigla_uf                VARCHAR(5),
    municipio               VARCHAR(200),
    sigla_partido_eleicao   VARCHAR(20),
    uri_partido_eleicao     VARCHAR(500),
    CONSTRAINT uq_silver_camara_dep_mandato_ext UNIQUE (camara_deputado_id, cargo, sigla_uf, ano_inicio)
);
```

### V34 — Tabela principal Senado + referências

```sql
-- V34__create_silver_senado_senador.sql

CREATE TABLE IF NOT EXISTS silver.senado_senador (
    id                          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id                  UUID,
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMP,
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado           BOOLEAN NOT NULL DEFAULT FALSE,

    -- Listagem
    codigo_senador              VARCHAR(20),
    nome_parlamentar            VARCHAR(300),
    nome_civil                  VARCHAR(300),
    sexo                        VARCHAR(5),
    uf_parlamentar              VARCHAR(5),
    participacao                VARCHAR(5),
    partido_parlamentar         VARCHAR(100),
    sigla_partido_parlamentar   VARCHAR(20),
    data_designacao             VARCHAR(30),
    codigo_legislatura          VARCHAR(10),

    -- Complemento (GET /senador/{codigo})
    det_nome_completo           VARCHAR(300),
    det_data_nascimento         VARCHAR(30),
    det_local_nascimento        VARCHAR(200),
    det_estado_civil            VARCHAR(50),
    det_escolaridade            VARCHAR(100),
    det_contato_email           VARCHAR(200),
    det_url_foto                VARCHAR(500),
    det_url_pagina_parlamentar  VARCHAR(500),
    det_pagina                  VARCHAR(500),
    det_facebook                VARCHAR(500),
    det_twitter                 VARCHAR(500),
    det_profissoes              TEXT,

    CONSTRAINT uq_silver_senado_senador_codigo UNIQUE (codigo_senador)
);

CREATE INDEX idx_silver_senado_senador_gold ON silver.senado_senador (gold_sincronizado) WHERE NOT gold_sincronizado;

CREATE TABLE IF NOT EXISTS silver.senado_senador_afastado (
    id                          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id                  UUID,
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMP,
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado           BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador              VARCHAR(20),
    nome_parlamentar            VARCHAR(300),
    uf_mandato                  VARCHAR(5),
    motivo_afastamento          VARCHAR(300),
    data_afastamento            VARCHAR(30),
    data_termino_afastamento    VARCHAR(30),
    CONSTRAINT uq_silver_senado_afastado UNIQUE (codigo_senador, data_afastamento)
);

CREATE TABLE IF NOT EXISTS silver.senado_partido (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_partido      VARCHAR(20),
    sigla_partido       VARCHAR(20),
    nome_partido        VARCHAR(200),
    data_ativacao       VARCHAR(30),
    data_desativacao    VARCHAR(30),
    CONSTRAINT uq_silver_senado_partido UNIQUE (codigo_partido)
);

CREATE TABLE IF NOT EXISTS silver.senado_tipo_uso_palavra (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_tipo         VARCHAR(20),
    descricao_tipo      VARCHAR(200),
    abreviatura         VARCHAR(20),
    CONSTRAINT uq_silver_senado_tipo_uso_palavra UNIQUE (codigo_tipo)
);
```

### V35 — Sub-tabelas Senado por senador

```sql
-- V35__create_silver_senado_senador_subtabelas.sql

CREATE TABLE IF NOT EXISTS silver.senado_senador_profissao (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador      VARCHAR(20),
    codigo_profissao    VARCHAR(20),
    descricao_profissao VARCHAR(300),
    data_registro       VARCHAR(30),
    CONSTRAINT uq_silver_senado_sen_prof UNIQUE (codigo_senador, codigo_profissao)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_mandato (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador      VARCHAR(20),
    codigo_mandato      VARCHAR(20),
    descricao           VARCHAR(300),
    uf_mandato          VARCHAR(5),
    participacao        VARCHAR(5),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    data_designacao     VARCHAR(30),
    data_termino        VARCHAR(30),
    entrou_exercicio    VARCHAR(5),
    data_exercicio      VARCHAR(30),
    CONSTRAINT uq_silver_senado_sen_mandato UNIQUE (codigo_senador, codigo_mandato)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_licenca (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador      VARCHAR(20),
    codigo_licenca      VARCHAR(20),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    motivo              VARCHAR(100),
    descricao_motivo    VARCHAR(300),
    CONSTRAINT uq_silver_senado_sen_licenca UNIQUE (codigo_senador, codigo_licenca)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_historico_academico (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador          VARCHAR(20),
    codigo_curso            VARCHAR(20),
    nome_curso              VARCHAR(300),
    instituicao             VARCHAR(300),
    descricao_instituicao   VARCHAR(300),
    nivel_formacao          VARCHAR(100),
    data_inicio_formacao    VARCHAR(30),
    data_termino_formacao   VARCHAR(30),
    concluido               VARCHAR(5),
    CONSTRAINT uq_silver_senado_sen_hist_acad UNIQUE (codigo_senador, codigo_curso)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_filiacao (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador          VARCHAR(20),
    codigo_filiacao         VARCHAR(20),
    codigo_partido          VARCHAR(20),
    sigla_partido           VARCHAR(20),
    nome_partido            VARCHAR(200),
    data_inicio_filiacao    VARCHAR(30),
    data_termino_filiacao   VARCHAR(30),
    CONSTRAINT uq_silver_senado_sen_filiacao UNIQUE (codigo_senador, codigo_filiacao)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_discurso (
    id                      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id              UUID,
    ingerido_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMP,
    content_hash            VARCHAR(64),
    origem_carga            VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado       BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador          VARCHAR(20),
    codigo_discurso         VARCHAR(20),
    codigo_sessao           VARCHAR(20),
    data_pronunciamento     VARCHAR(30),
    casa                    VARCHAR(10),
    tipo_sessao             VARCHAR(100),
    numero_sessao           VARCHAR(20),
    tipo_pronunciamento     VARCHAR(100),
    texto_discurso          TEXT,
    duracao_aparte          VARCHAR(20),
    url_video               VARCHAR(500),
    url_audio               VARCHAR(500),
    CONSTRAINT uq_silver_senado_sen_discurso UNIQUE (codigo_senador, codigo_discurso)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_comissao (
    id                          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id                  UUID,
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMP,
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado           BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador              VARCHAR(20),
    codigo_comissao             VARCHAR(20),
    sigla_comissao              VARCHAR(30),
    nome_comissao               VARCHAR(300),
    cargo                       VARCHAR(100),
    data_inicio_participacao    VARCHAR(30),
    data_termino_participacao   VARCHAR(30),
    ativo                       VARCHAR(5),
    CONSTRAINT uq_silver_senado_sen_comissao UNIQUE (codigo_senador, codigo_comissao, data_inicio_participacao)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_cargo (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id          UUID,
    ingerido_em         TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMP,
    content_hash        VARCHAR(64),
    origem_carga        VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado   BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador      VARCHAR(20),
    codigo_cargo        VARCHAR(20),
    descricao_cargo     VARCHAR(300),
    tipo_cargo          VARCHAR(100),
    comissao_ou_orgao   VARCHAR(300),
    data_inicio         VARCHAR(30),
    data_fim            VARCHAR(30),
    CONSTRAINT uq_silver_senado_sen_cargo UNIQUE (codigo_senador, codigo_cargo, data_inicio)
);

CREATE TABLE IF NOT EXISTS silver.senado_senador_aparte (
    id                          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    etl_job_id                  UUID,
    ingerido_em                 TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em               TIMESTAMP,
    content_hash                VARCHAR(64),
    origem_carga                VARCHAR(20) NOT NULL DEFAULT 'API',
    gold_sincronizado           BOOLEAN NOT NULL DEFAULT FALSE,
    codigo_senador              VARCHAR(20),
    codigo_aparte               VARCHAR(20),
    codigo_discurso_principal   VARCHAR(20),
    codigo_sessao               VARCHAR(20),
    data_pronunciamento         VARCHAR(30),
    casa                        VARCHAR(10),
    texto_aparte                TEXT,
    url_video                   VARCHAR(500),
    CONSTRAINT uq_silver_senado_sen_aparte UNIQUE (codigo_senador, codigo_aparte)
);
```

---

## 5. Componentes Java (Plano de Implementação)

### 5.1. Domínio — Entidades JPA

Uma entidade por tabela, seguindo o mesmo padrão de `SilverCamaraProposicao`:

| Entidade                                | Tabela                                      | Pacote          |
| --------------------------------------- | ------------------------------------------- | --------------- |
| `SilverCamaraDeputado`                  | `silver.camara_deputado`                    | `domain.silver` |
| `SilverCamaraDeputadoProfissao`         | `silver.camara_deputado_profissao`          | `domain.silver` |
| `SilverCamaraDeputadoOcupacao`          | `silver.camara_deputado_ocupacao`           | `domain.silver` |
| `SilverCamaraDeputadoOrgao`             | `silver.camara_deputado_orgao`              | `domain.silver` |
| `SilverCamaraDeputadoFrente`            | `silver.camara_deputado_frente`             | `domain.silver` |
| `SilverCamaraDeputadoPresencaEvento`    | `silver.camara_deputado_presenca_evento`    | `domain.silver` |
| `SilverCamaraDespesa`                   | `silver.camara_despesa`                     | `domain.silver` |
| `SilverCamaraMesaDiretora`              | `silver.camara_mesa_diretora`               | `domain.silver` |
| `SilverCamaraGrupoMembro`               | `silver.camara_grupo_membro`                | `domain.silver` |
| `SilverCamaraDeputadoDiscurso`          | `silver.camara_deputado_discurso`           | `domain.silver` |
| `SilverCamaraDeputadoEvento`            | `silver.camara_deputado_evento`             | `domain.silver` |
| `SilverCamaraDeputadoHistorico`         | `silver.camara_deputado_historico`          | `domain.silver` |
| `SilverCamaraDeputadoMandatoExterno`    | `silver.camara_deputado_mandato_externo`    | `domain.silver` |
| `SilverSenadoSenador`                   | `silver.senado_senador`                     | `domain.silver` |
| `SilverSenadoSenadorAfastado`           | `silver.senado_senador_afastado`            | `domain.silver` |
| `SilverSenadoPartido`                   | `silver.senado_partido`                     | `domain.silver` |
| `SilverSenadoTipoUsoPalavra`            | `silver.senado_tipo_uso_palavra`            | `domain.silver` |
| `SilverSenadoSenadorProfissao`          | `silver.senado_senador_profissao`           | `domain.silver` |
| `SilverSenadoSenadorMandato`            | `silver.senado_senador_mandato`             | `domain.silver` |
| `SilverSenadoSenadorLicenca`            | `silver.senado_senador_licenca`             | `domain.silver` |
| `SilverSenadoSenadorHistoricoAcademico` | `silver.senado_senador_historico_academico` | `domain.silver` |
| `SilverSenadoSenadorFiliacao`           | `silver.senado_senador_filiacao`            | `domain.silver` |
| `SilverSenadoSenadorDiscurso`           | `silver.senado_senador_discurso`            | `domain.silver` |
| `SilverSenadoSenadorComissao`           | `silver.senado_senador_comissao`            | `domain.silver` |
| `SilverSenadoSenadorCargo`              | `silver.senado_senador_cargo`               | `domain.silver` |
| `SilverSenadoSenadorAparte`             | `silver.senado_senador_aparte`              | `domain.silver` |

### 5.2. Repositórios

Um `JpaRepository<Entity, UUID>` por entidade, no pacote `repository.silver`.

Métodos auxiliares típicos (espelhar padrão existente):

- `findByXxxId(String id)` — busca por chave natural
- `findAllByXxxIdIn(List<String> ids)` — batch lookup para upsert
- `findByGoldSincronizadoFalse()` — pendentes promoção Gold
- `@Modifying marcarGoldSincronizado(UUID id)` — marcar promovido

### 5.3. Extractors

| Classe                       | Responsabilidade                                                                                    |
| ---------------------------- | --------------------------------------------------------------------------------------------------- |
| `CamaraDeputadoCSVExtractor` | Extrai `deputados.csv` e sub-CSVs usando `CamaraCSVExtractor` existente (OpenCSV, `;`, UTF-8 BOM)   |
| `CamaraDespesaCSVExtractor`  | Extrai `Ano-{ano}.csv.zip` — requer descompressão ZIP antes do parse                                |
| `CamaraDeputadoApiExtractor` | Extrai endpoints API-only (`/discursos`, `/eventos`, `/historico`, `/mandatosExternos`, `/detalhe`) |
| `SenadoSenadorApiExtractor`  | Extrai todos os 16 endpoints ativos do Senado. Spring WebClient + Resilience4j (8 req/s)            |

### 5.4. Loaders

| Classe                                | Padrão                      | Descrição                                               |
| ------------------------------------- | --------------------------- | ------------------------------------------------------- |
| `SilverCamaraDeputadoLoader`          | Upsert por `camara_id`      | Carga principal CSV + API-complement                    |
| `SilverCamaraDeputadoSubtabelaLoader` | Insert-if-not-exists        | Profissão, Ocupação, Órgão, Frente                      |
| `SilverCamaraDespesaLoader`           | Upsert por chave composta   | Despesas CEAP                                           |
| `SilverCamaraDeputadoApiLoader`       | Insert-if-not-exists        | Discurso, Evento, Histórico, Mandato Externo            |
| `SilverSenadoSenadorLoader`           | Upsert por `codigo_senador` | Carga principal lista + detalhe                         |
| `SilverSenadoSenadorEnriquecedor`     | Insert-if-not-exists        | Sub-recursos (profissão, mandato, licença, ..., aparte) |

### 5.5. Enrichment Service

Novo `SilverDeputadoEnrichmentService` e `SilverSenadorEnrichmentService`, seguindo o padrão de `SilverEnrichmentService`:

**Câmara:**

1. Carregar `deputados.csv` → `silver.camara_deputado`
2. Carregar CSVs secundários (profissões, ocupações, órgãos, frentes, presenças, despesas, mesa, grupos)
3. Enriquecer `det_*` via `GET /deputados/{id}` para registros com `det_status_id IS NULL`
4. Carregar endpoints API-only (discursos, eventos, histórico, mandatos externos) iterando deputados já na Silver

**Senado:**

1. Carregar `GET /senador/lista/atual` + `/lista/legislatura/{leg}` → `silver.senado_senador`
2. Enriquecer `det_*` via `GET /senador/{codigo}` para registros com `det_nome_completo IS NULL`
3. Carregar sub-recursos iterando senadores já na Silver (profissão, mandatos, licenças, histórico acadêmico, filiações, discursos, comissões, cargos, apartes)

---

## 6. Estratégia de Implementação por Fases

### Fase 1 — Deputados CSV (base)

**Escopo:** C1 (deputados.csv) com enriquecimento A1 (detalhe API)

- Flyway V30
- Entidade `SilverCamaraDeputado`
- `CamaraDeputadoCSVExtractor`
- `SilverCamaraDeputadoLoader`
- Enriquecimento `det_*` via API
- Admin controller `/admin/etl/parlamentares/camara`
- **Resultado:** Base de deputados na Silver com dados biográficos completos

### Fase 2 — Deputados sub-tabelas CSV

**Escopo:** C2-C6, C8-C9 (profissões, ocupações, órgãos, frentes, presenças, mesa, grupos)

- Flyway V31, V32 (parcial), V33 (parcial)
- 7 entidades + repositórios
- `SilverCamaraDeputadoSubtabelaLoader`

### Fase 3 — Despesas CEAP

**Escopo:** C7 (`Ano-{ano}.csv`)

- Flyway V32 (parcial)
- `CamaraDespesaCSVExtractor` (requer handler de ZIP)
- `SilverCamaraDespesaLoader`
- **Volume alto:** ~300k registros/ano. Processamento por chunks obrigatório.

### Fase 4 — Deputados endpoints API-only

**Escopo:** A2-A5 (discursos, eventos, histórico, mandatos externos)

- Flyway V33 (parcial)
- `CamaraDeputadoApiExtractor`
- `SilverCamaraDeputadoApiLoader`
- **Dependência:** Fase 1 concluída (precisa dos IDs dos deputados)

### Fase 5 — Senadores base

**Escopo:** S1-S7 (lista, afastados, partidos, tipos, detalhe)

- Flyway V34
- `SenadoSenadorApiExtractor`
- `SilverSenadoSenadorLoader`
- **Volume:** ~600 senadores (81 em exercício + histórico)

### Fase 6 — Senadores sub-recursos

**Escopo:** S8-S16 (profissão, mandatos, licenças, histórico acadêmico, filiações, discursos, comissões, cargos, apartes)

- Flyway V35
- `SilverSenadoSenadorEnriquecedor`
- **Restrição:** Discursos e apartes requerem iteração por janelas de 365 dias
- **Volume potencial:** Discursos podem ser volumosos (~2000+/senador ao longo de mandatos)

---

## 7. Resumo Quantitativo

| Dimensão                       | Câmara         | Senado  | Total  |
| ------------------------------ | -------------- | ------- | ------ |
| Tabelas Silver                 | 13             | 13      | **26** |
| Entidades JPA                  | 13             | 13      | **26** |
| Repositórios                   | 13             | 13      | **26** |
| CSVs                           | 9              | 0       | **9**  |
| Endpoints API                  | 5 (complement) | 16      | **21** |
| Endpoints deprecados ignorados | 0              | 4       | **4**  |
| Flyway migrations              | V30-V33        | V34-V35 | **6**  |
| Fases de implementação         | 4              | 2       | **6**  |

---

## 8. Riscos e Mitigações

| Risco                                             | Impacto                         | Mitigação                                                                                                                                         |
| ------------------------------------------------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| Volume de despesas CEAP (~300k/ano desde 2008)    | Alta carga no BD                | Chunks de 10.000 registros; particionamento futuro por ano                                                                                        |
| Rate limit Câmara (retry-after: 30s)              | Lentidão na ingestão API        | Respeitar `retry-after`, cache de respostas, preferir CSV                                                                                         |
| Senado: Discursos limitados a 1 ano por consulta  | Complexidade de orquestração    | Loop por intervalos de 365 dias desde o primeiro mandato                                                                                          |
| 4 endpoints Senado deprecados (sunset 01/02/2026) | Dados indisponíveis após sunset | Implementar via endpoints substitutos (votação, processo, composição) em módulos futuros                                                          |
| Colunas CSV podem divergir dos nomes da API       | Mapeamento incorreto            | Validar com amostra real antes do mapeamento; os CSVs reais podem ter nomes diferentes dos publicados na API — ajustar entidade na primeira carga |
| ZIP das despesas (CEAP) requer handler especial   | Falha de extração               | Implementar `CamaraZipFileDownloader` dedicado ou estender `CamaraFileDownloader`                                                                 |

---

## 9. Decisões de Projeto

1. **CSV-first para Câmara:** Sempre preferir CSV como fonte principal. A API serve para complemento (`det_*`) e endpoints sem CSV equivalente.
2. **Sem FK entre Silver tables:** As tabelas Silver usam `camara_deputado_id` / `codigo_senador` como `VARCHAR`, sem FK física. A integridade será validada na promoção Gold.
3. **Valores como texto:** Campos numéricos e datas da fonte são armazenados como `VARCHAR` na Silver (zero transformação). Conversão para tipos nativos ocorre na Gold.
4. **Endpoints deprecados excluídos:** Os 4 endpoints Senado deprecados não serão implementados. Dados equivalentes virão dos endpoints substitutos em módulos dedicados (votação, processo, composição).
5. **Presença vs Eventos:** `eventosPresencaDeputados` (C6) é a fonte autoritativa para presença. `GET /deputados/{id}/eventos` (A3) é complementar com metadados ricos. Ambos são ingeridos em tabelas separadas.
6. **Despesas separadas:** `silver.camara_despesa` fica em tabela própria (não sub-tabela de deputado) pelo volume e importância analítica.
