#!/usr/bin/env bash
# 可观测控制台 TLS 回归：Grafana、Prometheus、Alertmanager、Thanos Query 与 Kibana。
#
# 前置：observability profile 已启动，证书已生成。
# 环境变量：REG_CACERT；GRAFANA_ADMIN_PASSWORD（可选，用于数据源健康检查）。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$HERE/lib.sh"

ROOT="$(cd "$HERE/../.." && pwd)"
CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
EMPTY_CA="$(mktemp)"
cleanup() { rm -f "$EMPTY_CA"; }
trap cleanup EXIT

if [ -n "$CA" ] && [ -f "$CA" ]; then
    reg_pass "本地 CA 存在：$CA"
else
    reg_fail "本地 CA 不存在：${CA:-<空>}"
fi

# 端口、名称、HTTPS 健康路径。
CHECKS=(
    "3000|Grafana|/api/health"
    "9090|Prometheus|/-/ready"
    "9093|Alertmanager|/-/ready"
    "19192|Thanos Query|/-/ready"
    "5601|Kibana|/api/status"
)

for entry in "${CHECKS[@]}"; do
    IFS='|' read -r port name path <<< "$entry"
    if echo | timeout 10 openssl s_client -connect "localhost:$port" -tls1_2 -CAfile "$CA" 2>&1 \
        | grep -q "Verify return code: 0"; then
        reg_pass "$name 端口 $port 接受 TLS 且证书通过本地 CA 校验"
    else
        reg_fail "$name 端口 $port TLS 握手或证书校验失败"
    fi
    code="$(curl -sS --cacert "$CA" -o /dev/null -w '%{http_code}' "https://localhost:$port$path" 2>/dev/null || true)"
    if [ "$code" = "200" ]; then
        reg_pass "$name HTTPS 健康端点返回 200"
    else
        reg_fail "$name HTTPS 健康端点失败（HTTP ${code:-无响应}）"
    fi
    plain="$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:$port$path" 2>/dev/null || true)"
    if [ "$plain" = "200" ]; then
        reg_fail "$name 端口 $port 仍接受明文 HTTP"
    else
        reg_pass "$name 端口 $port 拒绝明文（HTTP ${plain:-无响应}）"
    fi
    no_ca="$(curl -sS --cacert "$EMPTY_CA" -o /dev/null -w '%{http_code}' "https://localhost:$port$path" 2>/dev/null || true)"
    if [ "$no_ca" = "000" ] || [ -z "$no_ca" ]; then
        reg_pass "$name 不带内部 CA 时证书校验失败"
    else
        reg_fail "$name 不带内部 CA 仍可访问：HTTP $no_ca"
    fi
done

if [ -n "${GRAFANA_ADMIN_PASSWORD:-}" ]; then
    ds_json="$(curl -sS --cacert "$CA" -u "admin:$GRAFANA_ADMIN_PASSWORD" \
        "https://localhost:3000/api/datasources" 2>/dev/null || true)"
    for name in Prometheus Thanos; do
        uid="$(printf '%s' "$ds_json" | python3 -c \
            'import json,sys; name=sys.argv[1]; print(next((d["uid"] for d in json.load(sys.stdin) if d.get("name")==name), ""))' \
            "$name" 2>/dev/null || true)"
        if [ -z "$uid" ]; then
            reg_fail "Grafana 数据源 $name 不存在"
            continue
        fi
        health="$(curl -sS --cacert "$CA" -u "admin:$GRAFANA_ADMIN_PASSWORD" \
            "https://localhost:3000/api/datasources/uid/$uid/health" 2>/dev/null \
            | python3 -c 'import json,sys; print(json.load(sys.stdin).get("status",""))' 2>/dev/null || true)"
        if [ "$health" = "OK" ]; then
            reg_pass "Grafana 数据源 $name 健康检查通过"
        else
            reg_fail "Grafana 数据源 $name 健康检查失败"
        fi
    done
else
    reg_log "  [SKIP] 未提供 GRAFANA_ADMIN_PASSWORD，跳过数据源健康检查"
fi

reg_summary
