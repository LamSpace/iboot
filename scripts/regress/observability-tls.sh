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

reg_log "== Sentinel Dashboard =="
SENTINEL_PORT=8858
if echo | timeout 10 openssl s_client -connect "localhost:$SENTINEL_PORT" -tls1_2 -CAfile "$CA" 2>&1 \
    | grep -q "Verify return code: 0"; then
    reg_pass "Sentinel Dashboard 端口 $SENTINEL_PORT 接受 TLS 且证书通过本地 CA 校验"
else
    reg_fail "Sentinel Dashboard 端口 $SENTINEL_PORT TLS 握手或证书校验失败"
fi
sentinel_code="$(curl -sS --cacert "$CA" -o /dev/null -w '%{http_code}' "https://localhost:$SENTINEL_PORT/" 2>/dev/null || true)"
if [ "$sentinel_code" = "200" ] || [ "$sentinel_code" = "302" ]; then
    reg_pass "Sentinel Dashboard HTTPS 入口可访问（HTTP $sentinel_code）"
else
    reg_fail "Sentinel Dashboard HTTPS 入口失败（HTTP ${sentinel_code:-无响应}）"
fi
sentinel_plain="$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:$SENTINEL_PORT/" 2>/dev/null || true)"
if [ "$sentinel_plain" = "200" ]; then
    reg_fail "Sentinel Dashboard 仍接受明文 HTTP"
else
    reg_pass "Sentinel Dashboard 拒绝明文（HTTP ${sentinel_plain:-无响应}）"
fi
sentinel_no_ca="$(curl -sS --cacert "$EMPTY_CA" -o /dev/null -w '%{http_code}' "https://localhost:$SENTINEL_PORT/" 2>/dev/null || true)"
if [ "$sentinel_no_ca" = "000" ] || [ -z "$sentinel_no_ca" ]; then
    reg_pass "Sentinel Dashboard 不带内部 CA 时证书校验失败"
else
    reg_fail "Sentinel Dashboard 不带内部 CA 仍可访问：HTTP $sentinel_no_ca"
fi
sentinel_anon="$(curl -sS --cacert "$CA" -o /dev/null -w '%{http_code}' "https://localhost:$SENTINEL_PORT/app/briefinfos.json" 2>/dev/null || true)"
if [ "$sentinel_anon" = "200" ]; then
    reg_fail "Sentinel Dashboard 匿名可读取机器列表（登录未生效）"
else
    reg_pass "Sentinel Dashboard 匿名管理接口被拒绝（HTTP ${sentinel_anon:-无响应}）"
fi
sentinel_jar="$(mktemp)"
curl -sS --cacert "$CA" -c "$sentinel_jar" -o /dev/null "https://localhost:$SENTINEL_PORT/" 2>/dev/null || true
curl -sS --cacert "$CA" -b "$sentinel_jar" -c "$sentinel_jar" -o /dev/null \
    -X POST --data-urlencode "username=${SENTINEL_DASHBOARD_USER:-sentinel}" \
    --data-urlencode "password=${SENTINEL_DASHBOARD_PASSWORD:-sentinel123}" \
    "https://localhost:$SENTINEL_PORT/auth/login" 2>/dev/null || true
sentinel_auth="$(curl -sS --cacert "$CA" -b "$sentinel_jar" -o /dev/null -w '%{http_code}' \
    "https://localhost:$SENTINEL_PORT/app/briefinfos.json" 2>/dev/null || true)"
rm -f "$sentinel_jar"
if [ "$sentinel_auth" = "200" ]; then
    reg_pass "Sentinel Dashboard 登录后可读取机器列表"
else
    reg_fail "Sentinel Dashboard 登录后仍不可用（HTTP ${sentinel_auth:-无响应}）"
fi

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
