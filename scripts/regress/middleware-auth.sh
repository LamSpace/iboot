#!/usr/bin/env bash
# 中间件账号鉴权回归：MySQL 最小权限、Redis 认证、Silo 受管凭据、Nacos 鉴权。
#
# 前置：中间件已启动，根目录 .env 或环境变量提供受管凭据。
# 环境变量：MYSQL_ROOT_PASSWORD、MYSQL_APP_PASSWORD、MYSQL_EXPORTER_PASSWORD、
#          REDIS_PASSWORD、SILO_ROOT_USER、SILO_ROOT_PASSWORD、
#          NACOS_USERNAME、NACOS_PASSWORD、REG_CACERT。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$HERE/lib.sh"

ROOT="$(cd "$HERE/../.." && pwd)"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
MYSQL_APP_USER="${MYSQL_USERNAME:-iboot_app}"
MYSQL_APP_PASSWORD="${MYSQL_APP_PASSWORD:-${MYSQL_PASSWORD:-}}"
MYSQL_EXPORTER_USER="${MYSQL_EXPORTER_USER:-iboot_exporter}"
MYSQL_EXPORTER_PASSWORD="${MYSQL_EXPORTER_PASSWORD:-}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
SILO_ROOT_USER="${SILO_ROOT_USER:-}"
SILO_ROOT_PASSWORD="${SILO_ROOT_PASSWORD:-}"
NACOS_USERNAME="${NACOS_USERNAME:-}"
NACOS_PASSWORD="${NACOS_PASSWORD:-}"

mysql_exec() {
    local user="$1" password="$2" sql="$3"
    docker exec -e MYSQL_PWD="$password" iboot-mysql mysql \
        --protocol=TCP --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem \
        -h127.0.0.1 -u"$user" -e "$sql" 2>/dev/null
}

reg_log "== 1. MySQL 最小权限 =="
if mysql_exec "$MYSQL_APP_USER" "$MYSQL_APP_PASSWORD" "SELECT 1" >/dev/null; then
    reg_pass "业务账号 $MYSQL_APP_USER 可以连接并查询"
else
    reg_fail "业务账号 $MYSQL_APP_USER 无法连接"
fi
if mysql_exec "$MYSQL_APP_USER" "$MYSQL_APP_PASSWORD" \
    "CREATE TABLE iboot.__auth_probe__ (id INT)" >/dev/null; then
    reg_fail "业务账号可以执行 DDL"
    mysql_exec "$MYSQL_APP_USER" "$MYSQL_APP_PASSWORD" "DROP TABLE IF EXISTS iboot.__auth_probe__" >/dev/null || true
else
    reg_pass "业务账号执行 DDL 被拒绝"
fi
if mysql_exec "$MYSQL_EXPORTER_USER" "$MYSQL_EXPORTER_PASSWORD" \
    "CREATE TABLE iboot.__exporter_probe__ (id INT)" >/dev/null; then
    reg_fail "exporter 账号可以写入业务库"
    mysql_exec "$MYSQL_EXPORTER_USER" "$MYSQL_EXPORTER_PASSWORD" "DROP TABLE IF EXISTS iboot.__exporter_probe__" >/dev/null || true
else
    reg_pass "exporter 账号不能写入业务库"
fi
if mysql_exec "$MYSQL_APP_USER" "wrong-password" "SELECT 1" >/dev/null; then
    reg_fail "MySQL 错误口令仍可连接"
else
    reg_pass "MySQL 错误口令被拒绝"
fi

reg_log "== 2. Redis 认证 =="
if docker exec -e REDISCLI_AUTH="$REDIS_PASSWORD" iboot-redis \
    redis-cli --tls --cacert /certs/rootCA.pem -p 6379 ping 2>/dev/null | grep -q PONG; then
    reg_pass "Redis 正确口令 PING 成功"
else
    reg_fail "Redis 正确口令 PING 失败"
fi
if docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem -p 6379 ping 2>/dev/null | grep -q PONG; then
    reg_fail "Redis 未认证仍可执行命令"
else
    reg_pass "Redis 未认证被拒绝"
fi
if docker exec -e REDISCLI_AUTH="wrong-password" iboot-redis \
    redis-cli --tls --cacert /certs/rootCA.pem -p 6379 ping 2>/dev/null | grep -q PONG; then
    reg_fail "Redis 错误口令仍可执行命令"
else
    reg_pass "Redis 错误口令被拒绝"
fi

reg_log "== 3. Silo 凭据 =="
if docker exec -e SILO_USER="$SILO_ROOT_USER" -e SILO_PASS="$SILO_ROOT_PASSWORD" iboot-silo \
    sh -c 'mcli alias set good https://127.0.0.1:9000 "$SILO_USER" "$SILO_PASS" >/dev/null && mcli ls good >/dev/null' 2>/dev/null; then
    reg_pass "Silo 受管凭据可列举 bucket"
else
    reg_fail "Silo 受管凭据无法访问"
fi
if docker exec iboot-silo sh -c \
    'mcli alias set bad https://127.0.0.1:9000 minioadmin minioadmin >/dev/null 2>&1 && mcli ls bad >/dev/null 2>&1'; then
    reg_fail "Silo 默认凭据 minioadmin 仍可访问"
else
    reg_pass "Silo 默认凭据被拒绝"
fi

reg_log "== 4. Nacos 鉴权 =="
login_resp="$(curl -sS --cacert "$CA" -X POST \
    "https://localhost:8848/nacos/v3/auth/user/login" \
    --data-urlencode "username=$NACOS_USERNAME" \
    --data-urlencode "password=$NACOS_PASSWORD" 2>/dev/null || true)"
if printf '%s' "$login_resp" | grep -q accessToken; then
    reg_pass "Nacos 服务账号登录成功"
else
    reg_fail "Nacos 服务账号登录失败"
fi
anon_status="$(curl -sS --cacert "$CA" -o /dev/null -w '%{http_code}' -X POST \
    "https://localhost:8848/nacos/v3/admin/cs/config" \
    --data-urlencode "dataId=__auth_probe__" \
    --data-urlencode "groupName=DEFAULT_GROUP" \
    --data-urlencode "namespaceId=public" \
    --data-urlencode "content=x" --data-urlencode "type=json" 2>/dev/null || true)"
if [ "$anon_status" = "401" ] || [ "$anon_status" = "403" ]; then
    reg_pass "Nacos 匿名管理请求被拒绝（HTTP $anon_status）"
else
    reg_fail "Nacos 匿名管理请求未被拒绝（HTTP ${anon_status:-无响应}）"
fi
if env -u NACOS_USERNAME -u NACOS_PASSWORD "$ROOT/scripts/sentinel-init.sh" >/dev/null 2>&1; then
    reg_fail "sentinel-init 缺少凭据仍成功"
else
    reg_pass "sentinel-init 缺少凭据时失败"
fi

reg_summary
