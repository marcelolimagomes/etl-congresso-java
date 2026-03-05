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

Opções:
  --env <host|container>   Ambiente de execução (padrão: host)
  --ano-inicio <YYYY>      Para modo *-full
  --ano-fim <YYYY>         Para modo *-full
  --data-inicio <YYYY-MM-DD> Para modo *-incremental
  --data-fim <YYYY-MM-DD>  Para modo *-incremental
  --ano <YYYY>             Para modo camara-reprocess
  --data <YYYY-MM-DD>      Para modo senado-reprocess
  -h, --help               Exibe esta ajuda

Exemplos:
  $(basename "$0") --env host --mode silver-full --ano-inicio 2024 --ano-fim 2024
  $(basename "$0") --env host --mode camara-full --ano-inicio 2024 --ano-fim 2024
  $(basename "$0") --env host --mode senado-incremental --data-inicio 2026-03-01 --data-fim 2026-03-05
  $(basename "$0") --env container --mode jobs
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
      > "$ROOT_DIR/data/logs/ingest-host.log" 2>&1 &
    echo $! > "$ROOT_DIR/data/logs/ingest-host.pid"
  )
  APP_PID="$(cat "$ROOT_DIR/data/logs/ingest-host.pid")"
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
    info "Disparando full load Silver (Câmara): $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/camara/full-load?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    info "Aguardando Câmara terminar (máx 30s)..."
    sleep 3
    info "Disparando full load Silver (Senado): $ANO_INICIO a $ANO_FIM"
    call_api POST "/admin/etl/senado/full-load?anoInicio=$ANO_INICIO&anoFim=$ANO_FIM" | python3 -m json.tool
    info "Jobs disparados com sucesso. Ambas as casas em processamento."
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

  *)
    die "Modo inválido: $MODE"
    ;;
esac
