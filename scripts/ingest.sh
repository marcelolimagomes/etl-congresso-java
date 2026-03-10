#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_NAME="host"
MODE="status"

ANO_INICIO=""
ANO_FIM=""
DATA_INICIO=""
DATA_FIM=""
ANO=""
DATA=""

APP_PID=""
APP_STARTED_BY_SCRIPT="false"
RUNTIME_LOG_DIR=""

resolve_runtime_log_dir() {
  local preferred="$ROOT_DIR/data/logs"
  local fallback="/tmp/etl-congresso-java-logs"

  if mkdir -p "$preferred" >/dev/null 2>&1 && [[ -w "$preferred" ]]; then
    RUNTIME_LOG_DIR="$preferred"
    return 0
  fi

  mkdir -p "$fallback"
  [[ -w "$fallback" ]] || die "Nenhum diretório de log gravável disponível (tentado: $preferred e $fallback)"
  RUNTIME_LOG_DIR="$fallback"
  info "Sem permissão em $preferred; usando logs em $RUNTIME_LOG_DIR"
}

is_read_only_mode() {
  [[ "$MODE" == "status" || "$MODE" == "jobs" ]]
}

usage() {
  cat <<EOF
Uso:
  $(basename "$0") --mode <modo> [opções]

Modos:
  status               GET  /admin/etl/status
  jobs                 GET  /admin/etl/jobs
  silver-full          POST /admin/etl/{camara,senado}/full-load (ambas as casas)
  camara-full          POST /admin/etl/camara/full-load
  camara-incremental   POST /admin/etl/camara/incremental
  camara-reprocess     POST /admin/etl/camara/reprocess
  senado-full          POST /admin/etl/senado/full-load
  senado-incremental   POST /admin/etl/senado/incremental
  senado-reprocess     POST /admin/etl/senado/reprocess
  pages-generate       POST /admin/etl/pages/generate (proposições + matérias)
  pages-generate-ano   POST /admin/etl/pages/generate?ano=YYYY (proposições + matérias de um ano)
  pages-generate-pm    Alias de pages-generate (pm = proposições/matérias)
  pages-generate-pm-ano Alias de pages-generate-ano
  pages-status         GET  /admin/etl/pages/status
  parlamentares-camara     POST /admin/etl/parlamentares/camara (CSVs + API Câmara)
  parlamentares-camara-csv POST /admin/etl/parlamentares/camara/csv (etapas 1-3: só CSVs, sem sub-recursos API)
  sub-recursos-deputados   POST /admin/etl/parlamentares/camara/sub-recursos (etapa 4: discursos, eventos, histórico, mandatos)
  parlamentares-senado     POST /admin/etl/parlamentares/senado (API Senado)
  parlamentares-all        Ambas as casas parlamentares (câmara + senado)
  pages-generate-parl      POST /admin/etl/pages/parlamentares/generate
  pages-status-parl        GET  /admin/etl/pages/parlamentares/status
  enriquece-deputados      POST /admin/etl/parlamentares/camara/enriquece (det_* via API)

Opções:
  --env <host|container>   Ambiente de execução (padrão: host)
  --ano-inicio <YYYY>      Para modo *-full
  --ano-fim <YYYY>         Para modo *-full
  --data-inicio <YYYY-MM-DD> Para modo *-incremental
  --data-fim <YYYY-MM-DD>  Para modo *-incremental
  --ano <YYYY>             Para modo camara-reprocess e pages-generate-ano
  --data <YYYY-MM-DD>      Para modo senado-reprocess
  -h, --help               Exibe esta ajuda

Exemplos:
  $(basename "$0") --env host --mode silver-full --ano-inicio 2024 --ano-fim 2024
  $(basename "$0") --env host --mode camara-full --ano-inicio 2024 --ano-fim 2024
  $(basename "$0") --env host --mode senado-incremental --data-inicio 2026-03-01 --data-fim 2026-03-05
  $(basename "$0") --env container --mode jobs
  $(basename "$0") --env host --mode pages-generate
  $(basename "$0") --env host --mode pages-generate-ano --ano 2024
  $(basename "$0") --env host --mode parlamentares-camara --ano-inicio 1988 --ano-fim 2026
  $(basename "$0") --env host --mode parlamentares-camara-csv --ano-inicio 2025 --ano-fim 2026
  $(basename "$0") --env host --mode sub-recursos-deputados
  $(basename "$0") --env host --mode parlamentares-senado
  $(basename "$0") --env host --mode parlamentares-all --ano-inicio 1988 --ano-fim 2026
  $(basename "$0") --env host --mode pages-generate-parl
EOF
}

die() {
  echo "[ERRO] $*" >&2
  exit 1
}

info() {
  echo "[INFO] $*"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env) ENV_NAME="${2:-}"; shift 2 ;;
    --mode) MODE="${2:-}"; shift 2 ;;
    --ano-inicio) ANO_INICIO="${2:-}"; shift 2 ;;
    --ano-fim) ANO_FIM="${2:-}"; shift 2 ;;
    --data-inicio) DATA_INICIO="${2:-}"; shift 2 ;;
    --data-fim) DATA_FIM="${2:-}"; shift 2 ;;
    --ano) ANO="${2:-}"; shift 2 ;;
    --data) DATA="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "Argumento desconhecido: $1" ;;
  esac
done

ENV_FILE="$ROOT_DIR/config/env/ingest-${ENV_NAME}.env"
[[ -f "$ENV_FILE" ]] || die "Arquivo de ambiente não encontrado: $ENV_FILE"

# shellcheck source=/dev/null
source "$ENV_FILE"

APP_BASE_URL="${APP_BASE_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-changeme}"
DB_MODE="${DB_MODE:-container}"
START_JAVA_APP="${START_JAVA_APP:-false}"
KEEP_APP_RUNNING="${KEEP_APP_RUNNING:-false}"
SPRING_PROFILE="${SPRING_PROFILE:-local}"

cleanup() {
  if [[ "$APP_STARTED_BY_SCRIPT" == "true" && "$KEEP_APP_RUNNING" != "true" && -n "$APP_PID" ]]; then
    if is_read_only_mode; then
      info "Parando processo Java iniciado por este script (PID=$APP_PID)."
      kill "$APP_PID" >/dev/null 2>&1 || true
    else
      info "Aplicação local mantida ativa (job assíncrono em execução)."
      info "Para parar depois: kill $APP_PID"
    fi
  fi
}
trap cleanup EXIT

ensure_postgres_container() {
  if [[ "$DB_MODE" == "container" ]]; then
    info "Garantindo PostgreSQL em container..."
    (cd "$ROOT_DIR" && docker compose up -d postgres >/dev/null)
  fi
}

wait_for_app() {
  local retries=45
  local sleep_seconds=2
  for ((i=1; i<=retries; i++)); do
    if curl -sf "$APP_BASE_URL/actuator/health" >/dev/null; then
      return 0
    fi
    sleep "$sleep_seconds"
  done
  return 1
}

start_java_app_if_needed() {
  if [[ "$START_JAVA_APP" != "true" ]]; then
    return 0
  fi

  if curl -sf "$APP_BASE_URL/actuator/health" >/dev/null 2>&1; then
    info "Aplicação já está ativa em $APP_BASE_URL."
    return 0
  fi

  local mvn_cmd
  mvn_cmd="$(command -v mvn || true)"
  if [[ -z "$mvn_cmd" ]]; then
    mvn_cmd="/tmp/apache-maven-3.9.9/bin/mvn"
  fi
  [[ -x "$mvn_cmd" ]] || die "Maven não encontrado. Configure mvn ou /tmp/apache-maven-3.9.9/bin/mvn"

  local java_home
  java_home="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
  [[ -d "$java_home" ]] || die "Não foi possível detectar JAVA_HOME"

  resolve_runtime_log_dir
  local app_log_file="$RUNTIME_LOG_DIR/ingest-host.log"
  local app_pid_file="$RUNTIME_LOG_DIR/ingest-host.pid"

  info "Iniciando Java no host com profile '$SPRING_PROFILE'..."
  (
    cd "$ROOT_DIR"
    JAVA_HOME="$java_home" \
    SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/etl_congresso}" \
    SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-etl_user}" \
    SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-etl_pass}" \
    ADMIN_USERNAME="$ADMIN_USERNAME" \
    ADMIN_PASSWORD="$ADMIN_PASSWORD" \
    ETL_CAMARA_BASE_URL="${ETL_CAMARA_BASE_URL:-https://dadosabertos.camara.leg.br}" \
    ETL_CAMARA_CSV_BASE_URL="${ETL_CAMARA_CSV_BASE_URL:-https://dadosabertos.camara.leg.br/arquivos}" \
    ETL_SENADO_BASE_URL="${ETL_SENADO_BASE_URL:-https://legis.senado.leg.br}" \
    "$mvn_cmd" -q spring-boot:run -Dspring-boot.run.profiles="$SPRING_PROFILE" \
      > "$app_log_file" 2>&1 &
    echo $! > "$app_pid_file"
  )
  APP_PID="$(cat "$app_pid_file")"
  APP_STARTED_BY_SCRIPT="true"

  info "Aguardando aplicação subir em $APP_BASE_URL ..."
  wait_for_app || die "Aplicação não respondeu em $APP_BASE_URL/actuator/health"
  info "Aplicação disponível."
}

call_api() {
  local method="$1"
  local endpoint="$2"
  local body="${3:-}"

  if [[ -n "$body" ]]; then
    curl -sf -X "$method" -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" \
      -H 'Content-Type: application/json' \
      -d "$body" \
      "$APP_BASE_URL$endpoint"
  else
    curl -sf -X "$method" -u "$ADMIN_USERNAME:$ADMIN_PASSWORD" \
      "$APP_BASE_URL$endpoint"
  fi
}

ensure_postgres_container
start_java_app_if_needed

case "$MODE" in
  status)
    info "Consultando status do ETL..."
    call_api GET "/admin/etl/status" | python3 -m json.tool
    ;;

  jobs)
    info "Listando jobs ETL..."
    call_api GET "/admin/etl/jobs" | python3 -m json.tool
    ;;

  silver-full)
    [[ -n "$ANO_INICIO" ]] || die "Informe --ano-inicio para silver-full"
    [[ -n "$ANO_FIM" ]] || die "Informe --ano-fim para silver-full"
    # Câmara e Senado usam locks independentes (etl:camara / etl:senado) e
    # operam em tabelas separadas — são disparados em paralelo no servidor.
    info "Disparando full load Silver (Câmara): $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/camara/full-load?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    info "Disparando full load Silver (Senado): $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/senado/full-load?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    info "Ambos os jobs iniciados em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode jobs"
    info "  $( basename "$0" ) --env $ENV_NAME --mode status"
    ;;

  camara-full)
    [[ -n "$ANO_INICIO" ]] || die "Informe --ano-inicio para camara-full"
    [[ -n "$ANO_FIM" ]] || die "Informe --ano-fim para camara-full"
    info "Disparando full load Câmara: $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/camara/full-load?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    ;;

  senado-full)
    [[ -n "$ANO_INICIO" ]] || die "Informe --ano-inicio para senado-full"
    [[ -n "$ANO_FIM" ]] || die "Informe --ano-fim para senado-full"
    info "Disparando full load Senado: $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/senado/full-load?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    ;;

  camara-incremental)
    body='{}'
    if [[ -n "$DATA_INICIO" || -n "$DATA_FIM" ]]; then
      [[ -n "$DATA_INICIO" && -n "$DATA_FIM" ]] || die "Para camara-incremental com intervalo, informe --data-inicio e --data-fim"
      body="{\"dataInicio\":\"$DATA_INICIO\",\"dataFim\":\"$DATA_FIM\"}"
    fi
    info "Disparando incremental Câmara..."
    call_api POST "/admin/etl/camara/incremental" "$body" | python3 -m json.tool
    ;;

  senado-incremental)
    body='{}'
    if [[ -n "$DATA_INICIO" || -n "$DATA_FIM" ]]; then
      [[ -n "$DATA_INICIO" && -n "$DATA_FIM" ]] || die "Para senado-incremental com intervalo, informe --data-inicio e --data-fim"
      body="{\"dataInicio\":\"$DATA_INICIO\",\"dataFim\":\"$DATA_FIM\"}"
    fi
    info "Disparando incremental Senado..."
    call_api POST "/admin/etl/senado/incremental" "$body" | python3 -m json.tool
    ;;

  camara-reprocess)
    [[ -n "$ANO" ]] || die "Informe --ano para camara-reprocess"
    info "Disparando reprocessamento Câmara ano=$ANO"
    call_api POST "/admin/etl/camara/reprocess?ano=$ANO" | python3 -m json.tool
    ;;

  senado-reprocess)
    [[ -n "$DATA" ]] || die "Informe --data para senado-reprocess"
    info "Disparando reprocessamento Senado data=$DATA"
    call_api POST "/admin/etl/senado/reprocess?data=$DATA" | python3 -m json.tool
    ;;

  pages-generate)
    info "Disparando geração de páginas estáticas (proposições + matérias)..."
    call_api POST "/admin/etl/pages/generate" | python3 -m json.tool
    info "Geração iniciada em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode pages-status"
    ;;

  pages-generate-ano)
    [[ -n "$ANO" ]] || die "Informe --ano para pages-generate-ano"
    info "Disparando geração de páginas estáticas (proposições + matérias) para ano=$ANO..."
    call_api POST "/admin/etl/pages/generate?ano=$ANO" | python3 -m json.tool
    info "Geração iniciada em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode pages-status"
    ;;

  pages-generate-pm)
    info "Usando alias pages-generate-pm -> pages-generate"
    info "Disparando geração de páginas estáticas (proposições + matérias)..."
    call_api POST "/admin/etl/pages/generate" | python3 -m json.tool
    info "Geração iniciada em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode pages-status"
    ;;

  pages-generate-pm-ano)
    [[ -n "$ANO" ]] || die "Informe --ano para pages-generate-pm-ano"
    info "Usando alias pages-generate-pm-ano -> pages-generate-ano"
    info "Disparando geração de páginas estáticas (proposições + matérias) para ano=$ANO..."
    call_api POST "/admin/etl/pages/generate?ano=$ANO" | python3 -m json.tool
    info "Geração iniciada em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode pages-status"
    ;;

  pages-status)
    info "Consultando status da geração de páginas..."
    call_api GET "/admin/etl/pages/status" | python3 -m json.tool
    ;;

  parlamentares-camara)
    [[ -n "$ANO_INICIO" ]] || die "Informe --ano-inicio para parlamentares-camara"
    [[ -n "$ANO_FIM" ]] || die "Informe --ano-fim para parlamentares-camara"
    info "Disparando ingestão de parlamentares Câmara: $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/parlamentares/camara?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    info "Job iniciado em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode jobs"
    ;;

  parlamentares-senado)
    info "Disparando ingestão de parlamentares Senado..."
    call_api POST "/admin/etl/parlamentares/senado" | python3 -m json.tool
    info "Job iniciado em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode jobs"
    ;;

  parlamentares-all)
    [[ -n "$ANO_INICIO" ]] || die "Informe --ano-inicio para parlamentares-all"
    [[ -n "$ANO_FIM" ]] || die "Informe --ano-fim para parlamentares-all"
    info "Disparando ingestão de parlamentares Câmara: $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/parlamentares/camara?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    info "Disparando ingestão de parlamentares Senado..."
    call_api POST "/admin/etl/parlamentares/senado" | python3 -m json.tool
    info "Ambos os jobs iniciados em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode jobs"
    ;;

  pages-generate-parl)
    info "Disparando geração de páginas estáticas de parlamentares..."
    call_api POST "/admin/etl/pages/parlamentares/generate" | python3 -m json.tool
    info "Geração iniciada em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode pages-status-parl"
    ;;

  pages-status-parl)
    info "Consultando status da geração de páginas de parlamentares..."
    call_api GET "/admin/etl/pages/parlamentares/status" | python3 -m json.tool
    ;;

  enriquece-deputados)
    info "Disparando enriquecimento det_* de deputados Câmara..."
    call_api POST "/admin/etl/parlamentares/camara/enriquece" | python3 -m json.tool
    info "Enriquecimento iniciado em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode jobs"
    ;;

  parlamentares-camara-csv)
    [[ -n "$ANO_INICIO" ]] || die "Informe --ano-inicio para parlamentares-camara-csv"
    [[ -n "$ANO_FIM" ]] || die "Informe --ano-fim para parlamentares-camara-csv"
    info "Disparando carga CSV parlamentares Câmara (etapas 1-3): $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/parlamentares/camara/csv?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    info "Job iniciado em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode jobs"
    ;;

  sub-recursos-deputados)
    info "Disparando carga de sub-recursos API de deputados Câmara (etapa 4)..."
    info "ATENÇÃO: Este job pode levar horas para 7878 deputados (4 endpoints x deputado)."
    call_api POST "/admin/etl/parlamentares/camara/sub-recursos" | python3 -m json.tool
    info "Job iniciado em background. Acompanhe via:"
    info "  $( basename "$0" ) --env $ENV_NAME --mode jobs"
    ;;

  *)
    die "Modo inválido: $MODE"
    ;;
esac
