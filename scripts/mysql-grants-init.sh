#!/usr/bin/env bash
# 在业务表创建后应用 MySQL 表级写权限矩阵。
#
# 由 compose 的一次性服务 mysql-grants-init 执行；必须在 db-init 之后、业务服务之前运行。
# 账号本身由 mysql-users-init 创建；本脚本先撤销旧授权，再按 scripts/mysql-table-grants.tsv 授予。
#
# 环境变量：
#   MYSQL_HOST / MYSQL_PORT / MYSQL_ADMIN_USER / MYSQL_ADMIN_PASSWORD / MYSQL_DATABASE
#   MYSQL_GRANTS_FILE（默认 /scripts/mysql-table-grants.tsv）

set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_ADMIN_USER="${MYSQL_ADMIN_USER:-iboot_provisioner}"
MYSQL_ADMIN_PASSWORD="${MYSQL_ADMIN_PASSWORD:?MYSQL_ADMIN_PASSWORD 必须在 .env 中设置}"
MYSQL_DATABASE="${MYSQL_DATABASE:-iboot}"
GRANTS_FILE="${MYSQL_GRANTS_FILE:-/scripts/mysql-table-grants.tsv}"

log() { printf '[mysql-grants-init] %s\n' "$*"; }
fail() { printf '[mysql-grants-init] ERROR: %s\n' "$*" >&2; exit 1; }

[ -f "$GRANTS_FILE" ] || fail "授权矩阵不存在：$GRANTS_FILE"

sql_quote() {
    printf '%s' "$1" | sed "s/'/''/g"
}

mysql_exec() {
    MYSQL_PWD="$MYSQL_ADMIN_PASSWORD" mysql \
        --protocol=TCP \
        --connect-timeout=5 \
        --ssl-mode=VERIFY_IDENTITY \
        --ssl-ca=/certs/rootCA.pem \
        -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_ADMIN_USER" "$@"
}

for _ in $(seq 1 60); do
    if mysql_exec -e "SELECT 1" >/dev/null 2>&1; then
        break
    fi
    sleep 2
done
mysql_exec -e "SELECT 1" >/dev/null 2>&1 || fail 'MySQL 未就绪'

mapfile -t lines < <(grep -vE '^\s*(#|$)' "$GRANTS_FILE")
[ "${#lines[@]}" -gt 0 ] || fail "授权矩阵为空：$GRANTS_FILE"

declare -A services=()
for line in "${lines[@]}"; do
    IFS=$'\t' read -r service table _ <<< "$line"
    [ -n "$service" ] && [ -n "$table" ] || fail "授权矩阵格式错误：$line"
    services["$service"]=1
done

sql=""
for service in "${!services[@]}"; do
    case "$service" in
        iboot_system|iboot_workflow|iboot_file|iboot_notify|iboot_job|iboot_monitor|iboot_auth|iboot_ai) ;;
        *) fail "未知服务账号：$service" ;;
    esac
    sql+="REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${service}'@'%';
GRANT SELECT ON \`${MYSQL_DATABASE}\`.* TO '${service}'@'%';
"
done

for line in "${lines[@]}"; do
    IFS=$'\t' read -r service table privileges <<< "$line"
    [ -n "$privileges" ] || fail "缺少权限：$line"
    case "$privileges" in
        *[!A-Z,]*|'') fail "非法权限：$privileges" ;;
    esac
    if [[ "$table" == *'*' ]]; then
        prefix="${table%\*}"
        case "$prefix" in
            *[!A-Za-z0-9_]*) fail "非法表前缀：$prefix" ;;
        esac
        mapfile -t matched < <(mysql_exec -N -B -e \
            "SELECT table_name FROM information_schema.tables WHERE table_schema='${MYSQL_DATABASE}' AND table_name LIKE '${prefix}%'")
        for name in "${matched[@]}"; do
            [ -n "$name" ] || continue
            [[ "$name" =~ ^[A-Za-z0-9_]+$ ]] || fail "非法表名：$name"
            sql+="GRANT ${privileges} ON \`${MYSQL_DATABASE}\`.\`${name}\` TO '${service}'@'%';
"
        done
    else
        [[ "$table" =~ ^[A-Za-z0-9_]+$ ]] || fail "非法表名：$table"
        sql+="GRANT ${privileges} ON \`${MYSQL_DATABASE}\`.\`${table}\` TO '${service}'@'%';
"
    fi
done

sql+="FLUSH PRIVILEGES;"
printf '%s' "$sql" | mysql_exec

log "表级授权已应用：${#services[@]} 个服务账号，${#lines[@]} 条规则"
