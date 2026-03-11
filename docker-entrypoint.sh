#!/bin/sh
# docker-entrypoint.sh
# Executa como root para garantir que os diretórios necessários existam
# com as permissões corretas mesmo quando volumes bind-mounted são montados
# sobre /tmp/etl. Em seguida, rebaixa para o usuário 'etl' com su-exec.
set -e

# Garante subdiretórios de trabalho (CSV da Câmara)
mkdir -p /tmp/etl/camara /app/logs

# Ajusta dono apenas se ainda for root (pode falhar em ambientes read-only)
chown -R etl:etl /tmp/etl /app/logs 2>/dev/null || true

# Rebaixa para usuário não-root e inicia a JVM
exec su-exec etl java $JAVA_OPTS -jar /app/app.jar "$@"
