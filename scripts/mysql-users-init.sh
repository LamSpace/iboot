#!/usr/bin/env bash
# 幂等创建/更新 MySQL 最小权限账号。
#
# 由 compose 的一次性服务 mysql-users-init 执行，也可在宿主机开发时通过
# `docker compose run --rm mysql-users-init` 复用同一份脚本。
#
# 环境变量：
#   MYSQL_HOST / MYSQL_PORT / MYSQL_ROOT_PASSWORD
#   MYSQL_APP_USER / MYSQL_APP_PASSWORD
#   MYSQL_EXPORTER_USER / MYSQL_EXPORTER_PASSWORD

set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD 必须在 .env 中设置}"
MYSQL_APP_USER="${MYSQL_APP_USER:-iboot_app}"
MYSQL_APP_PASSWORD="${MYSQL_APP_PASSWORD:?MYSQL_APP_PASSWORD 必须在 .env 中设置}"
MYSQL_EXPORTER_USER="${MYSQL_EXPORTER_USER:-iboot_exporter}"
MYSQL_EXPORTER_PASSWORD="${MYSQL_EXPORTER_PASSWORD:?MYSQL_EXPORTER_PASSWORD 必须在 .env 中设置}"

sql_escape() {
    printf '%s' "$1" | sed "s/'/''/g"
}

APP_USER_SQL="$(sql_escape "$MYSQL_APP_USER")"
APP_PASSWORD_SQL="$(sql_escape "$MYSQL_APP_PASSWORD")"
EXPORTER_USER_SQL="$(sql_escape "$MYSQL_EXPORTER_USER")"
EXPORTER_PASSWORD_SQL="$(sql_escape "$MYSQL_EXPORTER_PASSWORD")"

SQL="
CREATE USER IF NOT EXISTS '${APP_USER_SQL}'@'%' IDENTIFIED BY '${APP_PASSWORD_SQL}';
ALTER USER '${APP_USER_SQL}'@'%' IDENTIFIED BY '${APP_PASSWORD_SQL}';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`iboot\`.* TO '${APP_USER_SQL}'@'%';

CREATE USER IF NOT EXISTS '${EXPORTER_USER_SQL}'@'%' IDENTIFIED BY '${EXPORTER_PASSWORD_SQL}';
ALTER USER '${EXPORTER_USER_SQL}'@'%' IDENTIFIED BY '${EXPORTER_PASSWORD_SQL}';
GRANT PROCESS, REPLICATION CLIENT ON *.* TO '${EXPORTER_USER_SQL}'@'%';
GRANT SELECT ON \`performance_schema\`.* TO '${EXPORTER_USER_SQL}'@'%';

FLUSH PRIVILEGES;
"

for _ in $(seq 1 60); do
    if MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
        --protocol=TCP \
        --connect-timeout=5 \
        --ssl-mode=VERIFY_IDENTITY \
        --ssl-ca=/certs/rootCA.pem \
        -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root \
        -e "SELECT 1" >/dev/null 2>&1; then
        break
    fi
    sleep 2
done

MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
    --protocol=TCP \
    --connect-timeout=5 \
    --ssl-mode=VERIFY_IDENTITY \
    --ssl-ca=/certs/rootCA.pem \
    -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root \
    -e "$SQL"

printf '[mysql-users-init] 账号已就绪：%s、%s\n' "$MYSQL_APP_USER" "$MYSQL_EXPORTER_USER"
