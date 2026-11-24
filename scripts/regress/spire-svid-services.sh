#!/usr/bin/env bash
# SPIRE SVID 服务切换回归：十个服务证书的 SAN/有效期、信任 bundle、服务进程加载的证书路径，
# 以及 SVID 客户端到 SVID 服务端的 mTLS 健康检查。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

CERT_DIR="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
SVID_DIR="${IBOOT_SVID_DIR:-$CERT_DIR/svid}"
CA_BUNDLE="$CERT_DIR/ca/rootCA.pem"
SERVICES=(iboot-system iboot-gateway iboot-auth iboot-file iboot-notify iboot-job iboot-workflow iboot-monitor iboot-ai iboot-ops)

if [ ! -d "$SVID_DIR" ]; then
    reg_log "  [SKIP] SVID 目录不存在（未启用 IBOOT_SVID_ENABLED）"
    reg_summary
    exit "$REG_FAILED"
fi

for svc in "${SERVICES[@]}"; do
    cert="$SVID_DIR/$svc/svid.0.pem"
    key="$SVID_DIR/$svc/svid.0.key"
    if [ ! -f "$cert" ] || [ ! -f "$key" ]; then
        reg_fail "$svc 缺少 SVID"
        continue
    fi
    sans="$(openssl x509 -in "$cert" -noout -ext subjectAltName 2>/dev/null | tail -1)"
    printf '%s' "$sans" | grep -q "URI:spiffe://iboot.local/service/$svc" \
        && reg_pass "$svc SVID URI SAN 正确" \
        || reg_fail "$svc SVID URI SAN 异常：$sans"
    printf '%s' "$sans" | grep -q 'DNS:localhost' \
        && reg_pass "$svc SVID 包含 localhost DNS SAN" \
        || reg_fail "$svc SVID 缺少 localhost DNS SAN"
    printf '%s' "$sans" | grep -q 'DNS:host.docker.internal' \
        && reg_pass "$svc SVID 包含 host.docker.internal DNS SAN" \
        || reg_fail "$svc SVID 缺少 host.docker.internal DNS SAN"
    openssl x509 -in "$cert" -checkend 86400 -noout >/dev/null 2>&1 \
        && reg_pass "$svc SVID 剩余有效期超过 1 天" \
        || reg_fail "$svc SVID 即将过期"
    openssl verify -CAfile "$CA_BUNDLE" -untrusted "$cert" "$cert" >/dev/null 2>&1 \
        && reg_pass "$svc SVID 可由信任 bundle 验证" \
        || reg_fail "$svc SVID 无法由信任 bundle 验证"
done

service_uses_svid() {
    local svc="$1" pid
    if docker ps --format '{{.Names}}' | grep -qx "$svc"; then
        docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$svc" \
            | grep -q "^IBOOT_TLS_CERT_FILE=.*/svid/$svc/svid.0.pem"
        return
    fi
    pid="$(pgrep -f "$svc-1\.0\.0\.jar" | head -1 || true)"
    [ -n "$pid" ] && tr '\0' '\n' < "/proc/$pid/environ" 2>/dev/null \
        | grep -q "^IBOOT_TLS_CERT_FILE=.*/svid/$svc/svid.0.pem"
}

for svc in "${SERVICES[@]}"; do
    if [ "$svc" = "iboot-ops" ] && [ "${IBOOT_OPS_ENABLED:-true}" != "true" ]; then
        reg_log "  iboot-ops 已关闭（IBOOT_OPS_ENABLED=false），跳过进程检查"
        continue
    fi
    if service_uses_svid "$svc"; then
        reg_pass "$svc 进程加载 SVID"
    else
        reg_fail "$svc 进程未加载 SVID"
    fi
done

# 使用 ops 的 SVID 访问 system 的 actuator（规则允许 iboot-ops）。
ops_cert="$SVID_DIR/iboot-ops/svid.0.pem"
ops_key="$SVID_DIR/iboot-ops/svid.0.key"
if [ -f "$ops_cert" ] && [ -f "$ops_key" ]; then
    body="$(curl -sS --max-time 5 --cacert "$CA_BUNDLE" --cert "$ops_cert" --key "$ops_key" \
        https://localhost:8080/actuator/health 2>/dev/null || true)"
    if printf '%s' "$body" | grep -q '"status":"UP"'; then
        reg_pass "SVID 客户端到 SVID 服务端 mTLS 健康检查通过"
    else
        reg_fail "SVID mTLS 健康检查失败：$(printf '%s' "$body" | head -c 200)"
    fi
else
    reg_fail "缺少 iboot-ops SVID，无法验证 mTLS"
fi

reg_summary
exit "$REG_FAILED"
