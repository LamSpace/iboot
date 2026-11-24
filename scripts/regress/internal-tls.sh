#!/usr/bin/env bash
# 内部 TLS 与双向 TLS 回归：服务端口、网关上游、JWKS 与 MySQL/Redis/Silo/Neo4j。
#
# 前置：服务、网关与数据面已以内部 TLS 启动；证书已生成，服务证书含 clientAuth。
# 环境变量：SVC_CACERT（默认 REG_CACERT）、SVC_CLIENT_CERT/SVC_CLIENT_KEY
# （默认门户证书）、REG_BASE、REG_USER、REG_PASS。
# 数据面检查使用 iboot-mysql / iboot-redis / iboot-silo 容器；Neo4j 仅在运行时检查。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

SVC_CACERT="${SVC_CACERT:-${REG_CACERT:-}}"
CERT_DIR="${IBOOT_TLS_CERT_DIR:-$(cd "$HERE/../.." && pwd)/docker/certs}"
# 服务端口要求客户端证书；回归脚本以门户这一合法调用方的身份访问。
SVC_CLIENT_CERT="${SVC_CLIENT_CERT:-$CERT_DIR/portal.pem}"
SVC_CLIENT_KEY="${SVC_CLIENT_KEY:-$CERT_DIR/portal-key.pem}"
: "${MYSQL_ROOT_PASSWORD:?缺少 MYSQL_ROOT_PASSWORD（请加载根目录 .env）}"
: "${REDIS_EXPORTER_PASSWORD:?缺少 REDIS_EXPORTER_PASSWORD（请加载根目录 .env）}"
PORTS=(8080 8081 8082 8083 8084 8085 8086 8087 8088)
if [ "${IBOOT_OPS_ENABLED:-true}" = "true" ]; then
    PORTS+=(8089)
else
    reg_log "  iboot-ops 已关闭，跳过 8089"
fi
EMPTY_CA="$(mktemp)"
cleanup() { rm -f "$EMPTY_CA"; }
trap cleanup EXIT

svc_curl() {
    local -a args=(-sS)
    if [ -n "$SVC_CACERT" ]; then
        args+=(--cacert "$SVC_CACERT")
        if [ -f "$SVC_CLIENT_CERT" ] && [ -f "$SVC_CLIENT_KEY" ]; then
            args+=(--cert "$SVC_CLIENT_CERT" --key "$SVC_CLIENT_KEY")
        fi
    fi
    curl "${args[@]}" "$@"
}

# 只带 CA、不带客户端证书：用于 mTLS 负向用例。
no_cert_curl() {
    curl -sS --cacert "$SVC_CACERT" "$@"
}

openssl_client() {
    local port="$1" cert="${2:-}" key="${3:-}"
    local -a args=(-connect "localhost:$port" -tls1_2 -CAfile "$SVC_CACERT")
    if [ -n "$cert" ] && [ -n "$key" ]; then
        args+=(-cert "$cert" -key "$key")
    fi
    echo | timeout 10 openssl s_client "${args[@]}" 2>&1
}

reg_log "== 1. 内部 CA =="
if [ -n "$SVC_CACERT" ] && [ -f "$SVC_CACERT" ]; then
    reg_pass "内部 CA 存在：$SVC_CACERT"
else
    reg_fail "内部 CA 不存在：${SVC_CACERT:-<空>}（执行 scripts/tls-init.sh）"
fi

reg_log "== 2. 服务端口 TLS =="
for port in "${PORTS[@]}"; do
    if openssl_client "$port" "$SVC_CLIENT_CERT" "$SVC_CLIENT_KEY" | grep -q "Verify return code: 0"; then
        reg_pass "端口 $port 接受带客户端证书的 TLS 1.2"
    else
        reg_fail "端口 $port 带客户端证书的 TLS 1.2 握手失败"
    fi
    status="$(svc_curl -o /dev/null -w '%{http_code}' "https://localhost:$port/actuator/health" 2>/dev/null || true)"
    if [ "$status" = "200" ]; then
        reg_pass "端口 $port 的 HTTPS 健康检查通过"
    else
        reg_fail "端口 $port 的 HTTPS 健康检查失败（HTTP ${status:-无响应}）"
    fi
    no_cert="$(no_cert_curl -o /dev/null -w '%{http_code}' "https://localhost:$port/actuator/health" 2>/dev/null || true)"
    if [ "$no_cert" = "000" ] || [ -z "$no_cert" ]; then
        reg_pass "端口 $port 拒绝无客户端证书的连接"
    else
        reg_fail "端口 $port 仍接受无客户端证书的连接（HTTP $no_cert）"
    fi
    plain="$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:$port/actuator/health" 2>/dev/null || true)"
    if [ "$plain" = "200" ]; then
        reg_fail "端口 $port 仍接受明文 HTTP"
    else
        reg_pass "端口 $port 不接受明文（HTTP ${plain:-无响应}）"
    fi
done

reg_log "== 2b. 服务证书身份 =="
SERVICE_PORTS=(
    "iboot-system:8080" "iboot-ai:8081" "iboot-file:8082" "iboot-auth:8083"
    "iboot-notify:8084" "iboot-job:8085" "iboot-workflow:8086"
    "iboot-monitor:8087" "iboot-gateway:8088"
)
if [ "${IBOOT_OPS_ENABLED:-true}" = "true" ]; then
    SERVICE_PORTS+=("iboot-ops:8089")
fi
FINGERPRINTS="$(mktemp)"
service_cert_fingerprint() {
    local port="$1" svc="$2"
    echo | timeout 10 openssl s_client -connect "localhost:$port" -servername "$svc" \
        -CAfile "$SVC_CACERT" -cert "$SVC_CLIENT_CERT" -key "$SVC_CLIENT_KEY" 2>/dev/null \
        | openssl x509 2>/dev/null
}
for entry in "${SERVICE_PORTS[@]}"; do
    svc="${entry%%:*}"
    port="${entry##*:}"
    cert="$(service_cert_fingerprint "$port" "$svc")"
    if [ -z "$cert" ]; then
        reg_fail "$svc 未提供证书"
        continue
    fi
    if printf '%s' "$cert" | openssl x509 -noout -checkhost "$svc" >/dev/null 2>&1; then
        reg_pass "$svc 证书 SAN 覆盖服务名 $svc"
    else
        reg_fail "$svc 证书 SAN 不包含服务名 $svc"
    fi
    if printf '%s' "$cert" | openssl x509 -noout -checkhost localhost >/dev/null 2>&1; then
        reg_pass "$svc 证书 SAN 覆盖 localhost"
    else
        reg_fail "$svc 证书 SAN 不包含 localhost"
    fi
    if printf '%s' "$cert" | openssl x509 -noout -checkhost host.docker.internal >/dev/null 2>&1; then
        reg_pass "$svc 证书 SAN 覆盖 host.docker.internal"
    else
        reg_fail "$svc 证书 SAN 不包含 host.docker.internal"
    fi
    if printf '%s' "$cert" | openssl x509 -noout -purpose 2>/dev/null | grep -q 'SSL server[[:space:]]*: Yes'; then
        reg_pass "$svc 证书可用于服务端身份"
    else
        reg_fail "$svc 证书缺少 serverAuth 用途"
    fi
    if printf '%s' "$cert" | openssl x509 -noout -purpose 2>/dev/null | grep -q 'SSL client[[:space:]]*: Yes'; then
        reg_pass "$svc 证书可用于客户端身份"
    else
        reg_fail "$svc 证书缺少 clientAuth 用途"
    fi
    printf '%s' "$cert" | openssl x509 -noout -fingerprint -sha256 \
        | sed 's/^.*=//' >> "$FINGERPRINTS"
done
unique_count="$(sort -u "$FINGERPRINTS" | wc -l | tr -d ' ')"
if [ "$unique_count" -eq "${#SERVICE_PORTS[@]}" ]; then
    reg_pass "十张服务证书指纹互不相同"
else
    reg_fail "服务证书存在重复指纹（唯一 $unique_count / ${#SERVICE_PORTS[@]}）"
fi
shared_fp="$(openssl x509 -in "$CERT_DIR/portal.pem" -noout -fingerprint -sha256 2>/dev/null | sed 's/^.*=//')"
if [ -n "$shared_fp" ] && grep -qxF "$shared_fp" "$FINGERPRINTS"; then
    reg_fail "业务服务仍在共享门户证书"
else
    reg_pass "业务服务未使用共享门户证书"
fi
rm -f "$FINGERPRINTS"

if openssl x509 -in "$CERT_DIR/portal.pem" -noout -purpose 2>/dev/null | grep -q 'SSL client[[:space:]]*: Yes'; then
    reg_pass "门户证书可用于访问服务端口"
else
    reg_fail "门户证书缺少 clientAuth 用途"
fi

reg_log "== 2c. 每服务证书可作为客户端 =="
for entry in "${SERVICE_PORTS[@]}"; do
    svc="${entry%%:*}"
    service_cert="$CERT_DIR/services/$svc.pem"
    service_key="$CERT_DIR/services/$svc-key.pem"
    if [ ! -f "$service_cert" ] || [ ! -f "$service_key" ]; then
        reg_fail "$svc 客户端证书文件缺失"
        continue
    fi
    if openssl_client 8080 "$service_cert" "$service_key" | grep -q "Verify return code: 0"; then
        reg_pass "$svc 证书可作为客户端访问系统服务端口"
    else
        reg_fail "$svc 证书无法作为客户端完成 mTLS 握手"
    fi
done

reg_log "== 3. 无 CA 必须失败 =="
status="$(curl -sS -o /dev/null -w '%{http_code}' --cacert "$EMPTY_CA" \
    --cert "$SVC_CLIENT_CERT" --key "$SVC_CLIENT_KEY" \
    "https://localhost:8080/actuator/health" 2>/dev/null || true)"
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
    cipher="$(docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" iboot-mysql mysql -h127.0.0.1 -uroot \
        --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem -N \
        -e "SHOW STATUS LIKE 'Ssl_cipher'" 2>/dev/null | awk '{print $2}')"
    if [ -n "$cipher" ] && [ "$cipher" != "NULL" ]; then
        reg_pass "MySQL 加密连接 cipher=$cipher"
    else
        reg_fail "MySQL 未使用 TLS"
    fi
    if docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" iboot-mysql mysql -h127.0.0.1 -uroot --ssl-mode=DISABLED \
        -e "SELECT 1" >/dev/null 2>&1; then
        reg_fail "MySQL 仍接受明文连接"
    else
        reg_pass "MySQL 拒绝明文连接"
    fi
    if docker exec -e MYSQL_PWD="${MYSQL_SYSTEM_PASSWORD:-}" iboot-mysql mysql -h127.0.0.1 -uiboot_system \
        --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem -e "SELECT 1" >/dev/null 2>&1; then
        reg_fail "MySQL 业务账号不带客户端证书仍可连接"
    else
        reg_pass "MySQL 业务账号不带客户端证书被拒绝"
    fi
    if docker exec -e MYSQL_PWD="${MYSQL_SYSTEM_PASSWORD:-}" iboot-mysql mysql -h127.0.0.1 -uiboot_system \
        --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem \
        --ssl-cert=/certs/services/iboot-system.pem --ssl-key=/certs/services/iboot-system-key.pem \
        -e "SELECT 1" >/dev/null 2>&1; then
        reg_pass "MySQL 业务账号带自己的客户端证书可连接"
    else
        reg_fail "MySQL 业务账号带自己的客户端证书仍被拒绝"
    fi
    if docker exec -e MYSQL_PWD="${MYSQL_SYSTEM_PASSWORD:-}" iboot-mysql mysql -h127.0.0.1 -uiboot_system \
        --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem \
        --ssl-cert=/certs/services/iboot-file.pem --ssl-key=/certs/services/iboot-file-key.pem \
        -e "SELECT 1" >/dev/null 2>&1; then
        reg_fail "MySQL 接受其它服务的客户端证书"
    else
        reg_pass "MySQL 拒绝其它服务的客户端证书"
    fi
else
    reg_fail "找不到 iboot-mysql 容器"
fi

reg_log "== 6. Redis TLS =="
if docker inspect iboot-redis >/dev/null 2>&1; then
    if [ "$(docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem \
        --cert /certs/clients/redis-exporter.pem --key /certs/clients/redis-exporter-key.pem \
        --user iboot_exporter --pass "$REDIS_EXPORTER_PASSWORD" -p 6379 ping 2>/dev/null)" = "PONG" ]; then
        reg_pass "Redis TLS PING 成功"
    else
        reg_fail "Redis TLS PING 失败"
    fi
    if docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem -p 6379 \
        --user iboot_system --pass "${REDIS_SYSTEM_PASSWORD:-}" ping 2>/dev/null | grep -q PONG; then
        reg_fail "Redis 不带客户端证书仍可连接"
    else
        reg_pass "Redis 不带客户端证书被拒绝"
    fi
    if [ "$(docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem \
        --cert /certs/clients/redis-exporter.pem --key /certs/clients/redis-exporter-key.pem \
        --user iboot_system --pass "${REDIS_SYSTEM_PASSWORD:-}" -p 6379 ping 2>/dev/null)" = "PONG" ]; then
        reg_pass "Redis 带客户端证书可连接"
    else
        reg_fail "Redis 带客户端证书仍被拒绝"
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

reg_log "== 8. Nacos TLS =="
nacos_status="$(svc_curl -o /dev/null -w '%{http_code}' "https://localhost:8848/nacos/v1/ns/operator/metrics" 2>/dev/null || true)"
if [ "$nacos_status" = "200" ]; then
    reg_pass "Nacos 8848 HTTPS API 返回 200"
else
    reg_fail "Nacos 8848 HTTPS API 失败（HTTP ${nacos_status:-无响应}）"
fi
nacos_console="$(svc_curl -o /dev/null -w '%{http_code}' "https://localhost:8849/v3/console/health/readiness" 2>/dev/null || true)"
if [ "$nacos_console" = "200" ]; then
    reg_pass "Nacos 控制台 HTTPS 返回 200"
else
    reg_fail "Nacos 控制台 HTTPS 失败（HTTP ${nacos_console:-无响应}）"
fi
nacos_no_ca="$(curl -sS -o /dev/null -w '%{http_code}' "https://localhost:8848/nacos/v1/ns/operator/metrics" 2>/dev/null || true)"
if [ "$nacos_no_ca" = "200" ]; then
    reg_fail "Nacos 8848 不带 CA 仍可访问"
else
    reg_pass "Nacos 8848 不带 CA 时证书校验失败（${nacos_no_ca:-无响应}）"
fi
for port in 8848 8849; do
    plain="$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:$port/" 2>/dev/null || true)"
    if [ "$plain" = "200" ]; then
        reg_fail "Nacos 端口 $port 仍接受明文 HTTP"
    else
        reg_pass "Nacos 端口 $port 拒绝明文（HTTP ${plain:-无响应}）"
    fi
done
if echo | timeout 10 openssl s_client -connect localhost:9848 -tls1_2 -CAfile "$SVC_CACERT" \
    -cert "$CERT_DIR/portal.pem" -key "$CERT_DIR/portal-key.pem" 2>&1 | grep -q "Verify return code: 0"; then
    reg_pass "Nacos gRPC 9848 接受带客户端证书的 TLS 1.2"
else
    reg_fail "Nacos gRPC 9848 双向 TLS 握手失败"
fi
if echo | timeout 10 openssl s_client -connect localhost:9848 -tls1_2 -CAfile "$SVC_CACERT" 2>&1 | grep -q "Verify return code: 0"; then
    reg_fail "Nacos gRPC 9848 未要求客户端证书"
else
    reg_pass "Nacos gRPC 9848 拒绝无客户端证书的连接"
fi

reg_log "== 9. Neo4j（可选） =="
if docker inspect iboot-neo4j >/dev/null 2>&1; then
    if echo | timeout 10 openssl s_client -connect localhost:7687 -tls1_2 -CAfile "$SVC_CACERT" >/dev/null 2>&1; then
        reg_pass "Neo4j Bolt 端口接受 TLS"
    else
        reg_fail "Neo4j Bolt 端口 TLS 握手失败"
    fi
else
    reg_log "  Neo4j 未运行，跳过"
fi

reg_log "== 10. Tempo TLS =="
for port in 3200 4317 4318; do
    if echo | timeout 10 openssl s_client -connect "localhost:$port" -tls1_2 -CAfile "$SVC_CACERT" 2>&1 \
        | grep -q "Verify return code: 0"; then
        reg_pass "Tempo 端口 $port 接受 TLS 且证书通过本地 CA 校验"
    else
        reg_fail "Tempo 端口 $port TLS 握手或证书校验失败"
    fi
done
tempo_status="$(svc_curl -o /dev/null -w '%{http_code}' "https://localhost:3200/status" 2>/dev/null || true)"
if [ "$tempo_status" = "200" ]; then
    reg_pass "Tempo 查询端口通过 HTTPS 提供 /status"
else
    reg_fail "Tempo 查询端口 HTTPS 请求失败（HTTP ${tempo_status:-无响应}）"
fi
tempo_plain="$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:3200/status" 2>/dev/null || true)"
if [ "$tempo_plain" = "200" ]; then
    reg_fail "Tempo 3200 仍接受明文 HTTP"
else
    reg_pass "Tempo 3200 拒绝明文（HTTP ${tempo_plain:-无响应}）"
fi
tempo_no_ca="$(curl -sS -o /dev/null -w '%{http_code}' --cacert "$EMPTY_CA" "https://localhost:3200/status" 2>/dev/null || true)"
if [ "$tempo_no_ca" = "000" ] || [ -z "$tempo_no_ca" ]; then
    reg_pass "Tempo 不带内部 CA 时证书校验失败"
else
    reg_fail "Tempo 不带内部 CA 仍可访问：HTTP $tempo_no_ca"
fi

reg_log "== 11. SPIFFE 身份 =="
SERVICES=(iboot-system iboot-gateway iboot-auth iboot-file iboot-notify iboot-job iboot-workflow iboot-monitor iboot-ai iboot-ops)
CLIENTS=(prometheus blackbox)
EXPORTERS=(mysql-exporter redis-exporter node-exporter elasticsearch-exporter)
for svc in "${SERVICES[@]}"; do
    if openssl x509 -in "$CERT_DIR/services/$svc.pem" -noout -text 2>/dev/null \
        | grep -q "URI:spiffe://iboot.local/service/$svc"; then
        reg_pass "$svc 证书带 SPIFFE 服务身份"
    else
        reg_fail "$svc 证书缺少 SPIFFE 服务身份"
    fi
done
for client in "${CLIENTS[@]}"; do
    if openssl x509 -in "$CERT_DIR/clients/$client.pem" -noout -text 2>/dev/null \
        | grep -q "URI:spiffe://iboot.local/client/$client"; then
        reg_pass "$client 证书带 SPIFFE 客户端身份"
    else
        reg_fail "$client 证书缺少 SPIFFE 客户端身份"
    fi
done
for exporter in "${EXPORTERS[@]}"; do
    if openssl x509 -in "$CERT_DIR/services/$exporter.pem" -noout -text 2>/dev/null \
        | grep -q "URI:spiffe://iboot.local/service/$exporter"; then
        reg_pass "$exporter 证书带 SPIFFE 服务身份"
    else
        reg_fail "$exporter 证书缺少 SPIFFE 服务身份"
    fi
done

reg_summary
