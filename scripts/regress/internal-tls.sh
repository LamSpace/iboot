#!/usr/bin/env bash
# 内部 TLS 回归：服务端口、网关上游、JWKS 与 MySQL/Redis/Silo/Neo4j。
#
# 前置：服务、网关与数据面已以内部 TLS 启动；证书已生成。
# 环境变量：SVC_CACERT（默认 REG_CACERT）、REG_BASE、REG_USER、REG_PASS。
# 数据面检查使用 iboot-mysql / iboot-redis / iboot-silo 容器；Neo4j 仅在运行时检查。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

SVC_CACERT="${SVC_CACERT:-${REG_CACERT:-}}"
PORTS=(8080 8081 8082 8083 8084 8085 8086 8087 8088 8089)
EMPTY_CA="$(mktemp)"
cleanup() { rm -f "$EMPTY_CA"; }
trap cleanup EXIT

svc_curl() {
    local -a args=(-sS)
    [ -n "$SVC_CACERT" ] && args+=(--cacert "$SVC_CACERT")
    curl "${args[@]}" "$@"
}

reg_log "== 1. 内部 CA =="
if [ -n "$SVC_CACERT" ] && [ -f "$SVC_CACERT" ]; then
    reg_pass "内部 CA 存在：$SVC_CACERT"
else
    reg_fail "内部 CA 不存在：${SVC_CACERT:-<空>}（执行 scripts/tls-init.sh）"
fi

reg_log "== 2. 服务端口 TLS =="
for port in "${PORTS[@]}"; do
    if echo | timeout 10 openssl s_client -connect "localhost:$port" -tls1_2 -CAfile "$SVC_CACERT" >/dev/null 2>&1; then
        reg_pass "端口 $port 接受 TLS 1.2"
    else
        reg_fail "端口 $port TLS 1.2 握手失败"
    fi
    status="$(svc_curl -o /dev/null -w '%{http_code}' "https://localhost:$port/actuator/health" 2>/dev/null || true)"
    if [ "$status" = "200" ]; then
        reg_pass "端口 $port 的 HTTPS 健康检查通过"
    else
        reg_fail "端口 $port 的 HTTPS 健康检查失败（HTTP ${status:-无响应}）"
    fi
    plain="$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:$port/actuator/health" 2>/dev/null || true)"
    if [ "$plain" = "200" ]; then
        reg_fail "端口 $port 仍接受明文 HTTP"
    else
        reg_pass "端口 $port 不接受明文（HTTP ${plain:-无响应}）"
    fi
done

reg_log "== 3. 无 CA 必须失败 =="
status="$(curl -sS -o /dev/null -w '%{http_code}' --cacert "$EMPTY_CA" "https://localhost:8080/actuator/health" 2>/dev/null || true)"
if [ "$status" = "000" ] || [ -z "$status" ]; then
    reg_pass "不带内部 CA 时证书校验失败"
else
    reg_fail "不带内部 CA 仍可访问：HTTP $status"
fi

reg_log "== 4. 网关与 JWKS =="
gw_status="$(svc_curl -o /dev/null -w '%{http_code}' "https://localhost:8088/api/user/list" 2>/dev/null || true)"
if [ "$gw_status" = "401" ] || [ "$gw_status" = "403" ]; then
    reg_pass "网关经 HTTPS 转发到业务服务（HTTP $gw_status）"
else
    reg_fail "网关到业务服务的 HTTPS 链路异常（HTTP ${gw_status:-无响应}）"
fi
jwks_status="$(svc_curl -o /dev/null -w '%{http_code}' "https://localhost:8083/oauth2/jwks" 2>/dev/null || true)"
if [ "$jwks_status" = "200" ]; then
    reg_pass "JWKS 通过 HTTPS 提供"
else
    reg_fail "JWKS HTTPS 请求失败（HTTP ${jwks_status:-无响应}）"
fi

reg_log "== 5. MySQL TLS =="
if docker inspect iboot-mysql >/dev/null 2>&1; then
    cipher="$(docker exec iboot-mysql mysql -h127.0.0.1 -uroot -proot \
        --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem -N \
        -e "SHOW STATUS LIKE 'Ssl_cipher'" 2>/dev/null | awk '{print $2}')"
    if [ -n "$cipher" ] && [ "$cipher" != "NULL" ]; then
        reg_pass "MySQL 加密连接 cipher=$cipher"
    else
        reg_fail "MySQL 未使用 TLS"
    fi
    if docker exec iboot-mysql mysql -h127.0.0.1 -uroot -proot --ssl-mode=DISABLED \
        -e "SELECT 1" >/dev/null 2>&1; then
        reg_fail "MySQL 仍接受明文连接"
    else
        reg_pass "MySQL 拒绝明文连接"
    fi
else
    reg_fail "找不到 iboot-mysql 容器"
fi

reg_log "== 6. Redis TLS =="
if docker inspect iboot-redis >/dev/null 2>&1; then
    if [ "$(docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem -p 6379 ping 2>/dev/null)" = "PONG" ]; then
        reg_pass "Redis TLS PING 成功"
    else
        reg_fail "Redis TLS PING 失败"
    fi
    if docker exec iboot-redis redis-cli -p 6379 ping >/dev/null 2>&1; then
        reg_fail "Redis 仍接受明文连接"
    else
        reg_pass "Redis 拒绝明文连接"
    fi
else
    reg_fail "找不到 iboot-redis 容器"
fi

reg_log "== 7. Silo TLS =="
silo_status="$(svc_curl -o /dev/null -w '%{http_code}' "https://localhost:9000/minio/health/live" 2>/dev/null || true)"
if [ "$silo_status" = "200" ]; then
    reg_pass "Silo HTTPS 健康检查通过"
else
    reg_fail "Silo HTTPS 健康检查失败（HTTP ${silo_status:-无响应}）"
fi
silo_plain="$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:9000/minio/health/live" 2>/dev/null || true)"
if [ "$silo_plain" = "200" ]; then
    reg_fail "Silo 仍接受明文 HTTP"
else
    reg_pass "Silo 不接受明文（HTTP ${silo_plain:-无响应}）"
fi

reg_log "== 8. Neo4j（可选） =="
if docker inspect iboot-neo4j >/dev/null 2>&1; then
    if echo | timeout 10 openssl s_client -connect localhost:7687 -tls1_2 -CAfile "$SVC_CACERT" >/dev/null 2>&1; then
        reg_pass "Neo4j Bolt 端口接受 TLS"
    else
        reg_fail "Neo4j Bolt 端口 TLS 握手失败"
    fi
else
    reg_log "  Neo4j 未运行，跳过"
fi

reg_summary
