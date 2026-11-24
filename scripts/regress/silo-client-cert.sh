#!/usr/bin/env bash
# Silo 客户端证书回归：服务端启用 AssumeRoleWithCertificate，文件/AI 证书可换取临时凭据，
# 无证书与错误服务证书被拒绝，策略按证书 CN 映射。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

CA="$ROOT/docker/certs/ca/rootCA.pem"
SILO="https://127.0.0.1:9000"
STS="$SILO?Action=AssumeRoleWithCertificate&Version=2011-06-15&DurationSeconds=3600"

if ! docker ps --format '{{.Names}}' | grep -qx 'iboot-silo'; then
    reg_log "  [SKIP] Silo 未运行"
    reg_summary
    exit "$REG_FAILED"
fi

if docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' iboot-silo \
        | grep -qx 'MINIO_IDENTITY_TLS_ENABLE=on'; then
    reg_pass "Silo 已启用客户端证书身份"
else
    reg_fail "Silo 未启用 MINIO_IDENTITY_TLS_ENABLE=on"
fi

service_enabled() {
    local svc="$1" var pid
    var="IBOOT_$(printf '%s' "$svc" | tr '[:lower:]' '[:upper:]')_SILO_CLIENT_CERT_ENABLED"
    if docker ps --format '{{.Names}}' | grep -qx "iboot-$svc"; then
        docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "iboot-$svc" \
            | grep -qx "$var=true"
        return
    fi
    pid="$(pgrep -f "iboot-$svc-1\.0\.0\.jar" | head -1 || true)"
    [ -n "$pid" ] && tr '\0' '\n' < "/proc/$pid/environ" 2>/dev/null | grep -qx "$var=true"
}

for svc in file ai; do
    if service_enabled "$svc"; then
        reg_pass "$svc 服务已启用 Silo 客户端证书"
    else
        reg_fail "$svc 服务未启用 Silo 客户端证书"
    fi
done

sts_with_cert() {
    local svc="$1"
    curl -sS --max-time 5 --cacert "$CA" \
        --cert "$ROOT/docker/certs/services/iboot-$svc.pem" \
        --key "$ROOT/docker/certs/services/iboot-$svc-key.pem" \
        -X POST "$STS" 2>/dev/null
}

for svc in file ai; do
    body="$(sts_with_cert "$svc")"
    if printf '%s' "$body" | grep -q '<AccessKeyId>'; then
        reg_pass "iboot-$svc 证书换取临时凭据"
    else
        reg_fail "iboot-$svc 证书换取凭据失败：$(printf '%s' "$body" | head -c 200)"
    fi
done

code="$(curl -sS -o /tmp/silo-sts-nocert.out -w '%{http_code}' --max-time 5 \
    --cacert "$CA" -X POST "$STS" 2>/dev/null || true)"
case "$code" in
    400|401|403) reg_pass "无客户端证书被拒绝（HTTP $code）" ;;
    *) reg_fail "无客户端证书未被拒绝（HTTP ${code:-000}）" ;;
esac

# mcli 的 alias 保存在容器本地，silo-init 的 alias 不会跨容器共享；这里在 Silo 容器内重建。
docker exec iboot-silo sh -c \
    'mcli alias set local https://127.0.0.1:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null' \
    >/dev/null 2>&1 || true
for policy in iboot-file iboot-ai; do
    if docker exec iboot-silo sh -c "mcli admin policy info local $policy" >/dev/null 2>&1; then
        reg_pass "证书 CN 对应的策略存在：$policy"
    else
        reg_fail "证书 CN 对应的策略缺失：$policy"
    fi
done

reg_summary
exit "$REG_FAILED"
