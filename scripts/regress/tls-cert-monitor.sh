#!/usr/bin/env bash
# 证书到期监控回归：探针在跑、指标可见、告警规则已加载。
#
# 环境变量：REG_PROMETHEUS（默认 https://127.0.0.1:9090）、REG_CACERT

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$HERE/lib.sh"

PROM="${REG_PROMETHEUS:-https://127.0.0.1:9090}"
CA="${REG_CACERT:-$(cd "$HERE/../.." && pwd)/docker/certs/ca/rootCA.pem}"

if ! curl -sf --cacert "$CA" "$PROM/-/ready" >/dev/null 2>&1; then
    reg_log "  [SKIP] Prometheus 未运行（observability profile 未启用）"
    reg_summary
    exit "$REG_FAILED"
fi

reg_log "== 证书探针 =="
if docker inspect -f '{{.State.Running}}' iboot-blackbox-exporter 2>/dev/null | grep -q true; then
    reg_pass "blackbox-exporter 正在运行"
else
    reg_fail "blackbox-exporter 未运行"
fi

# 单次抓取失败不代表探针不可用：Prometheus 15s 抓一次，blackbox 经宿主 relay
# 访问服务时偶发瞬时失败。用 5 分钟窗口判断「最近是否成功过」，持续失败仍由
# tls-alerts 的 probe_success == 0 告警覆盖。
probe_query='max_over_time(probe_success{job="tls-cert"}[5m])'
probe_ok=""
for attempt in 1 2 3; do
    probe_ok="$(curl -sS --cacert "$CA" --get "$PROM/api/v1/query" \
        --data-urlencode "query=$probe_query" 2>/dev/null \
        | jq -r '[.data.result[].value[1]] | any(. == "1")')"
    [ "$probe_ok" = "true" ] && break
    [ "$attempt" -lt 3 ] && sleep 5
done
if [ "$probe_ok" = "true" ]; then
    reg_pass "证书探针握手成功（最近 5 分钟）"
else
    up="$(curl -sS --cacert "$CA" --get "$PROM/api/v1/query" \
        --data-urlencode 'query=up{job="tls-cert"}' 2>/dev/null \
        | jq -r '[.data.result[].value[1]] | join(",")')"
    raw="$(curl -sS --cacert "$CA" --get "$PROM/api/v1/query" \
        --data-urlencode 'query=probe_success{job="tls-cert"}' 2>/dev/null \
        | jq -r '[.data.result[].value[1]] | join(",")')"
    reg_fail "证书探针握手失败（最近 5 分钟无成功样本；up=${up:-无}，probe_success=${raw:-无}）"
fi

expiry="$(curl -sS --cacert "$CA" --get "$PROM/api/v1/query" \
    --data-urlencode 'query=max(probe_ssl_earliest_cert_expiry{job="tls-cert"})' 2>/dev/null \
    | jq -r '.data.result[0].value[1] // empty')"
if printf '%s' "$expiry" | grep -Eq '^[0-9]+(\.[0-9]+)?$' && [ "${expiry%.*}" -gt 0 ]; then
    days=$(( ( ${expiry%.*} - $(date +%s) ) / 86400 ))
    reg_pass "证书到期指标可见（剩余约 ${days} 天）"
else
    reg_fail "证书到期指标缺失"
fi

if curl -sS --cacert "$CA" "$PROM/api/v1/rules?type=alert" 2>/dev/null \
    | jq -e '.data.groups[] | select(.name == "tls-alerts")' >/dev/null; then
    reg_pass "tls-alerts 告警规则已加载"
else
    reg_fail "tls-alerts 告警规则未加载"
fi

reg_summary
