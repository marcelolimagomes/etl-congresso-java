.PHONY: help up down dev build test logs clean db-only ingest ingest-doc

# ─────────────────────────────────────────────────────────────────────────────
#  ETL Congresso — Comandos principais
# ─────────────────────────────────────────────────────────────────────────────

help: ## Exibe esta ajuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Guia de ingestão: docs/operacao-ingestao.md"

env: ## Cria .env a partir do .env.example (se não existir)
	@test -f .env || (cp .env.example .env && echo ".env criado — ajuste as senhas antes de continuar")

db-only: env ## Sobe apenas o PostgreSQL (para desenvolvimento local)
	docker compose up -d postgres adminer

dev: env db-only ## Ambiente de desenvolvimento (BD + Adminer)
	@echo "BD disponível em: localhost:5432"
	@echo "Adminer em: http://localhost:8081"
	@echo "Para rodar a app: mvn spring-boot:run -Dspring-boot.run.profiles=local"

up: env ## Sobe toda a stack (build incluso)
	docker compose up -d --build

monitoring: env ## Exibe métricas via Actuator (Prometheus/Grafana removidos do escopo)
	@echo "Prometheus e Grafana foram removidos do escopo."
	@echo "Use: curl http://localhost:8080/actuator/metrics"
	@echo "     curl http://localhost:8080/actuator/health"

tools: env ## Sobe com ferramentas extras (Adminer)
	docker compose --profile tools up -d

down: ## Para e remove containers (preserva volumes)
	docker compose down

clean: ## Para containers e remove volumes
	docker compose down -v
	@echo "Volumes removidos."

build: ## Compila a aplicação (requer Maven)
	@MVN=$$(which mvn 2>/dev/null || echo "/tmp/apache-maven-3.9.9/bin/mvn"); \
	JAVA_HOME=$$(dirname $$(dirname $$(readlink -f $$(which java)))); \
	JAVA_HOME=$$JAVA_HOME $$MVN -B package -DskipTests

test: ## Executa testes unitários (sem banco de dados)
	@MVN=$$(which mvn 2>/dev/null || echo "/tmp/apache-maven-3.9.9/bin/mvn"); \
	JAVA_HOME=$$(dirname $$(dirname $$(readlink -f $$(which java)))); \
	JAVA_HOME=$$JAVA_HOME $$MVN -B test -Dspring.profiles.active=test

test-all: db-only ## Executa todos os testes (unitários + integração — requer postgres)
	@sleep 3
	@MVN=$$(which mvn 2>/dev/null || echo "/tmp/apache-maven-3.9.9/bin/mvn"); \
	JAVA_HOME=$$(dirname $$(dirname $$(readlink -f $$(which java)))); \
	JAVA_HOME=$$JAVA_HOME $$MVN verify -Dspring.profiles.active=test

logs: ## Exibe logs da aplicação em tempo real
	docker compose logs -f etl-app

db-logs: ## Exibe logs do PostgreSQL
	docker compose logs -f postgres

health: ## Verifica saúde da aplicação
	@curl -sf http://localhost:8080/actuator/health | python3 -m json.tool || echo "App não disponível"

# ── ETL Admin ─────────────────────────────────────────────────────────────────

etl-full-camara: ## Inicia carga completa Câmara (ano atual)
	@read -p "Ano início [2001]: " ANO_INI; ANO_INI=$${ANO_INI:-2001}; \
	 read -p "Ano fim [2026]: " ANO_FIM; ANO_FIM=$${ANO_FIM:-2026}; \
	 curl -sf -X POST -u admin:changeme \
	   "http://localhost:8080/admin/etl/camara/full-load?anoInicio=$$ANO_INI&anoFim=$$ANO_FIM" \
	   | python3 -m json.tool

etl-full-senado: ## Inicia carga completa Senado (ano atual)
	@read -p "Ano início [1988]: " ANO_INI; ANO_INI=$${ANO_INI:-1988}; \
	 read -p "Ano fim [2026]: " ANO_FIM; ANO_FIM=$${ANO_FIM:-2026}; \
	 curl -sf -X POST -u admin:changeme \
	   "http://localhost:8080/admin/etl/senado/full-load?anoInicio=$$ANO_INI&anoFim=$$ANO_FIM" \
	   | python3 -m json.tool

etl-status: ## Verifica status dos jobs ETL
	@curl -sf -u admin:changeme http://localhost:8080/admin/etl/status | python3 -m json.tool

etl-jobs: ## Lista últimas execuções ETL
	@curl -sf -u admin:changeme http://localhost:8080/admin/etl/jobs | python3 -m json.tool

ingest: ## Dispara ingestão via script (ex: make ingest MODE=camara-full ENV=host ANO_INI=2024 ANO_FIM=2024)
	@./scripts/ingest.sh --env $${ENV:-host} --mode $${MODE:-status} \
		$$( [ -n "$${ANO_INI:-}" ] && echo --ano-inicio "$${ANO_INI}" ) \
		$$( [ -n "$${ANO_FIM:-}" ] && echo --ano-fim "$${ANO_FIM}" ) \
		$$( [ -n "$${DATA_INI:-}" ] && echo --data-inicio "$${DATA_INI}" ) \
		$$( [ -n "$${DATA_FIM:-}" ] && echo --data-fim "$${DATA_FIM}" ) \
		$$( [ -n "$${ANO:-}" ] && echo --ano "$${ANO}" ) \
		$$( [ -n "$${DATA:-}" ] && echo --data "$${DATA}" )

ingest-doc: ## Exibe o caminho do guia de operação da ingestão
	@echo "Veja: docs/operacao-ingestao.md"
