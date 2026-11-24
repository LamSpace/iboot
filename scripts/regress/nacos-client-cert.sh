#!/usr/bin/env bash
# Nacos 客户端证书回归：每个服务必须提交自己的服务证书，且不再引用 portal.pem。
#
# 支持 Docker 服务容器与 dev-init.sh 启动的宿主机进程两种模式。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

SERVICES=(system workflow file notify job monitor auth ai gateway ops)
CERT_DIR="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"

for svc in "${SERVICES[@]}"; do
    name="iboot-$svc"
    cert="$CERT_DIR/services/$name.pem"
    key="$CERT_DIR/services/$name-key.pem"
    if [ ! -f "$cert" ] || [ ! -f "$key" ]; then
        reg_fail "$name 证书或私钥不存在"
        continue
    fi
    expected_cert="$(sha256sum "$cert" | awk '{print $1}')"
    expected_key="$(sha256sum "$key" | awk '{print $1}')"

    if docker inspect "$name" >/dev/null 2>&1; then
        actual_cert="$(docker exec "$name" sha256sum /certs/nacos-client.pem 2>/dev/null | awk '{print $1}')"
        actual_key="$(docker exec "$name" sha256sum /certs/nacos-client-key.pem 2>/dev/null | awk '{print $1}')"
        if [ "$actual_cert" = "$expected_cert" ] && [ "$actual_key" = "$expected_key" ]; then
            reg_pass "$name 容器内 Nacos 客户端证书是自己的服务证书"
        else
            reg_fail "$name 容器内 Nacos 客户端证书不是自己的服务证书"
        fi
        env_dump="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$name" 2>/dev/null || true)"
        if printf '%s\n' "$env_dump" | grep -q 'Dnacos.remote.client.rpc.tls.certChainFile=file:/certs/nacos-client.pem'; then
            reg_pass "$name 容器使用稳定 Nacos 客户端证书路径"
        else
            reg_fail "$name 容器未使用稳定 Nacos 客户端证书路径"
        fi
        if printf '%s\n' "$env_dump" | grep -q 'Dnacos.remote.client.rpc.tls.certChainFile=file:/certs/portal.pem'; then
            reg_fail "$name 容器仍引用共享 portal.pem"
        else
            reg_pass "$name 容器不再引用共享 portal.pem"
        fi
        continue
    fi

    pid="$(pgrep -f "$name-1\\.0\\.0\\.jar" | head -1 || true)"
    if [ -z "$pid" ]; then
        reg_log "  [SKIP] $name 未运行（既不参与容器也不参与宿主机证书检查）"
        continue
    fi
    cmdline="$(tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null || true)"
    if printf '%s' "$cmdline" | grep -q "nacos.remote.client.rpc.tls.certChainFile=file:.*/services/$name.pem" \
        && printf '%s' "$cmdline" | grep -q "nacos.remote.client.rpc.tls.certPrivateKey=file:.*/services/$name-key.pem"; then
        reg_pass "$name 宿主机进程使用自己的 Nacos 客户端证书"
    else
        reg_fail "$name 宿主机进程未使用自己的 Nacos 客户端证书"
    fi
    if printf '%s' "$cmdline" | grep -q 'nacos.remote.client.rpc.tls.certChainFile=file:.*/portal.pem'; then
        reg_fail "$name 宿主机进程仍引用共享 portal.pem"
    else
        reg_pass "$name 宿主机进程不再引用共享 portal.pem"
    fi
done

reg_summary
exit "$REG_FAILED"
