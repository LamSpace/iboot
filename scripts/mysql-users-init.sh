#!/usr/bin/env bash
# 幂等创建/更新 MySQL 每服务账号与 exporter 账号。
#
# 由 compose 的一次性服务 mysql-users-init 执行，也可在宿主机开发时通过
# `docker compose run --rm mysql-users-init` 复用同一份脚本。
#
# 每个数据服务使用自己的账号 iboot_<service>；账号在此只创建与更新，具体表级写权限由
# db-init 建表后执行的 mysql-grants-init.sh 授予。
#
# 环境变量：
#   MYSQL_HOST / MYSQL_PORT / MYSQL_ADMIN_USER / MYSQL_ADMIN_PASSWORD
#   MYSQL_SYSTEM_PASSWORD ... MYSQL_AI_PASSWORD
#   MYSQL_EXPORTER_USER / MYSQL_EXPORTER_PASSWORD

set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_ADMIN_USER="${MYSQL_ADMIN_USER:-iboot_provisioner}"
MYSQL_ADMIN_PASSWORD="${MYSQL_ADMIN_PASSWORD:?MYSQL_ADMIN_PASSWORD 必须在 .env 中设置}"
MYSQL_EXPORTER_USER="${MYSQL_EXPORTER_USER:-iboot_exporter}"
MYSQL_EXPORTER_PASSWORD="${MYSQL_EXPORTER_PASSWORD:?MYSQL_EXPORTER_PASSWORD 必须在 .env 中设置}"

# 只有真正连接 MySQL 的服务在此登记；网关与运维控制台不建库账号。
SERVICES=(system workflow file notify job monitor auth ai)

sql_escape() {
    printf '%s' "$1" | sed "s/'/''/g"
}

EXPORTER_USER_SQL="$(sql_escape "$MYSQL_EXPORTER_USER")"
EXPORTER_PASSWORD_SQL="$(sql_escape "$MYSQL_EXPORTER_PASSWORD")"

SQL="DROP USER IF EXISTS 'iboot_app'@'%';
CREATE USER IF NOT EXISTS '${EXPORTER_USER_SQL}'@'%' IDENTIFIED BY '${EXPORTER_PASSWORD_SQL}';
ALTER USER '${EXPORTER_USER_SQL}'@'%' IDENTIFIED BY '${EXPORTER_PASSWORD_SQL}';
GRANT PROCESS, REPLICATION CLIENT ON *.* TO '${EXPORTER_USER_SQL}'@'%';
GRANT SELECT ON \`performance_schema\`.* TO '${EXPORTER_USER_SQL}'@'%';
"

for service in "${SERVICES[@]}"; do
    var="MYSQL_$(printf '%s' "$service" | tr '[:lower:]' '[:upper:]')_PASSWORD"
    password="${!var:-}"
    if [ -z "$password" ]; then
        printf '[mysql-users-init] ERROR: 缺少 %s\n' "$var" >&2
        exit 1
    fi
    user="iboot_${service}"
    user_sql="$(sql_escape "$user")"
    password_sql="$(sql_escape "$password")"
    SQL+="
CREATE USER IF NOT EXISTS '${user_sql}'@'%' IDENTIFIED BY '${password_sql}';
ALTER USER '${user_sql}'@'%' IDENTIFIED BY '${password_sql}';
"
done

SQL+="FLUSH PRIVILEGES;"

for _ in $(seq 1 60); do
    if MYSQL_PWD="$MYSQL_ADMIN_PASSWORD" mysql \
        --protocol=TCP \
        --connect-timeout=5 \
        --ssl-mode=VERIFY_IDENTITY \
        --ssl-ca=/certs/rootCA.pem \
        -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_ADMIN_USER" \
        -e "SELECT 1" >/dev/null 2>&1; then
        break
    fi
    sleep 2
done

MYSQL_PWD="$MYSQL_ADMIN_PASSWORD" mysql \
    --protocol=TCP \
    --connect-timeout=5 \
    --ssl-mode=VERIFY_IDENTITY \
    --ssl-ca=/certs/rootCA.pem \
    -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_ADMIN_USER" \
    -e "$SQL"

printf '[mysql-users-init] 账号已就绪：%s 个服务账号、%s\n' \
    "${#SERVICES[@]}" "$MYSQL_EXPORTER_USER"
