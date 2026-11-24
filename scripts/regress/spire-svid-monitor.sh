#!/usr/bin/env bash
# SPIRE/SVID 监控回归：规则加载、SPIRE 健康探针、SVID 指标与故障注入、Alertmanager 路由。
#
# 环境变量：REG_PROMETHEUS（默认 https://127.0.0.1:9090）、
#           REG_ALERTMANAGER（默认 https://127.0.0.1:9093）、REG_CACERT

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

PROM="${REG_PROMETHEUS:-https://127.0.0.1:9090}"
AM="${REG_ALERTMANAGER:-https://127.0.0.1:9093}"
CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
METRIC_DIR="${IBOOT_METRIC_DIR:-${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}/runtime}"
METRIC_FILE="$METRIC_DIR/svid-refresh.prom"
PROM_IMAGE="${PROM_IMAGE:-prom/prometheus:v3.16.0}"
AM_IMAGE="${AM_IMAGE:-prom/alertmanager:v0.35.0}"

if ! curl -sf --cacert "$CA" "$PROM/-/ready" >/dev/null 2>&1; then
    reg_log "  [SKIP] Prometheus 未运行（observability profile 未启用）"
    reg_summary
    exit "$REG_FAILED"
fi

query() {
    curl -sS --cacert "$CA" --get "$PROM/api/v1/query" --data-urlencode "query=$1" 2>/dev/null
}

alerts_json() {
    curl -sS --cacert "$CA" "$PROM/api/v1/alerts" 2>/dev/null
}

reg_log "== 规则与配置校验 =="
if docker run --rm --entrypoint /bin/promtool \
        -v "$ROOT/docker/scripts/prometheus/alerts:/rules:ro" \
        "$PROM_IMAGE" check rules /rules/tls-alerts.yml >/tmp/svid-monitor-promtool.log 2>&1; then
    reg_pass "Prometheus 告警规则语法有效"
else
    reg_fail "Prometheus 告警规则语法无效：$(tail -3 /tmp/svid-monitor-promtool.log)"
fi

if docker run --rm --entrypoint /bin/amtool \
        -v "$ROOT/docker/scripts/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro" \
        "$AM_IMAGE" check-config /etc/alertmanager/alertmanager.yml >/tmp/svid-monitor-amtool.log 2>&1; then
    reg_pass "Alertmanager 配置语法有效"
else
    reg_fail "Alertmanager 配置语法无效：$(tail -3 /tmp/svid-monitor-amtool.log)"
fi

if curl -sS --cacert "$CA" "$PROM/api/v1/rules?type=alert" 2>/dev/null \
        | jq -e '.data.groups[] | select(.name == "svid-alerts")' >/dev/null; then
    reg_pass "svid-alerts 告警规则已加载"
else
    reg_fail "svid-alerts 告警规则未加载"
fi

reg_log "== SPIRE 健康探针 =="
if docker ps --format '{{.Names}}' | grep -qx iboot-spire-server \
        && docker ps --format '{{.Names}}' | grep -qx iboot-spire-agent; then
    probe="$(query 'probe_success{job="spire-health"}')"
    server_ok="$(printf '%s' "$probe" | jq -r '[.data.result[] | select(.metric.instance | test("spire-server")) | .value[1]] | first // empty')"
    agent_ok="$(printf '%s' "$probe" | jq -r '[.data.result[] | select(.metric.instance | test("spire-agent")) | .value[1]] | first // empty')"
    [ "$server_ok" = "1" ] && reg_pass "SPIRE Server 健康探针成功" \
        || reg_fail "SPIRE Server 健康探针失败（$server_ok）"
    [ "$agent_ok" = "1" ] && reg_pass "SPIRE Agent 健康探针成功" \
        || reg_fail "SPIRE Agent 健康探针失败（$agent_ok）"
else
    reg_log "  [SKIP] SPIRE 容器未运行"
fi

reg_log "== SVID 刷新指标 =="
# 阈值 1 天：SVID 正常时只检查并写指标，不会触发刷新/重启。
"$ROOT/scripts/svid-refresh-schedule.sh" --run-now --target=host --threshold-days=1 \
    >/tmp/svid-monitor-refresh.log 2>&1 || true
seen=""
for _ in $(seq 1 45); do
    seen="$(query 'iboot_svid_refresh_last_exit_code')"
    if printf '%s' "$seen" | jq -e '.data.result | length > 0' >/dev/null 2>&1; then
        break
    fi
    sleep 2
done
exit_code="$(printf '%s' "$seen" | jq -r '.data.result[0].value[1] // empty')"
[ "$exit_code" = "0" ] && reg_pass "SVID 刷新退出码指标可见且为 0" \
    || reg_fail "SVID 刷新退出码指标缺失或非 0（$exit_code）"
min_remaining="$(query 'iboot_svid_cert_min_remaining_seconds' | jq -r '.data.result[0].value[1] // empty')"
if printf '%s' "$min_remaining" | grep -Eq '^[0-9]+(\.[0-9]+)?$' && [ "${min_remaining%.*}" -gt 0 ]; then
    reg_pass "SVID 最小剩余有效期指标可见"
else
    reg_fail "SVID 最小剩余有效期指标缺失"
fi

reg_log "== 故障注入 =="
backup="$(mktemp)"
[ -f "$METRIC_FILE" ] && cp "$METRIC_FILE" "$backup"
now="$(date +%s)"
mkdir -p "$METRIC_DIR"
cat > "$METRIC_FILE" <<EOF
# HELP iboot_svid_refresh_last_success_timestamp_seconds 最近一次 SVID 刷新成功时间。
# TYPE iboot_svid_refresh_last_success_timestamp_seconds gauge
iboot_svid_refresh_last_success_timestamp_seconds $((now - 4 * 86400))
# HELP iboot_svid_refresh_last_exit_code 最近一次 SVID 检查/刷新退出码。
# TYPE iboot_svid_refresh_last_exit_code gauge
iboot_svid_refresh_last_exit_code 1
# HELP iboot_svid_cert_min_remaining_seconds 十个服务 SVID 的最小剩余有效期。
# TYPE iboot_svid_cert_min_remaining_seconds gauge
iboot_svid_cert_min_remaining_seconds 3600
EOF
fired=""
for _ in $(seq 1 45); do
    fired="$(alerts_json | jq -r '[.data.alerts[]? | select(.labels.alertname == "SvidRefreshFailed")] | length')"
    [ "${fired:-0}" -gt 0 ] && break
    sleep 2
done
[ "${fired:-0}" -gt 0 ] && reg_pass "SVID 刷新失败告警被触发" \
    || reg_fail "SVID 刷新失败告警未触发"
stale="$(alerts_json | jq -r '[.data.alerts[]? | select(.labels.alertname == "SvidRefreshStale")] | length')"
[ "${stale:-0}" -gt 0 ] && reg_pass "SVID 刷新停滞告警被触发" \
    || reg_fail "SVID 刷新停滞告警未触发"

if [ -s "$backup" ]; then
    cp "$backup" "$METRIC_FILE"
else
    rm -f "$METRIC_FILE"
fi
rm -f "$backup"
# 恢复后重新写入成功指标，避免故障注入留在运行环境。
"$ROOT/scripts/svid-refresh-schedule.sh" --run-now --target=host --threshold-days=1 \
    >>/tmp/svid-monitor-refresh.log 2>&1 || true

reg_log "== Alertmanager 路由 =="
if curl -sf --cacert "$CA" "$AM/-/ready" >/dev/null 2>&1; then
    status="$(curl -sS --cacert "$CA" "$AM/api/v2/status" 2>/dev/null)"
    # Alertmanager v2 /status 直接返回配置对象，原始 YAML 位于 config.original。
    config="$(printf '%s' "$status" | jq -r '.config.original // .data.configYAML // empty')"
    if printf '%s' "$config" | grep -q 'security-webhook'; then
        reg_pass "Alertmanager 已加载安全告警 receiver"
    else
        reg_fail "Alertmanager 未加载安全告警 receiver"
    fi
    if printf '%s' "$config" | grep -Eq 'team.*security|security.*team'; then
        reg_pass "Alertmanager 安全告警路由已配置"
    else
        reg_fail "Alertmanager 安全告警路由缺失"
    fi
else
    reg_log "  [SKIP] Alertmanager 未运行"
fi

reg_summary
exit "$REG_FAILED"
