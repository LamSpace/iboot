#!/usr/bin/env bash
# Prometheus ↔ exporter mTLS 回归：四个 exporter 只接受 HTTPS + 受管客户端证书。
#
# 前置：observability profile 已启动，exporter 证书已生成。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$HERE/lib.sh"

ROOT="$(cd "$HERE/../.." && pwd)"
CERT_DIR="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
CA="$CERT_DIR/ca/rootCA.pem"
CLIENT_CERT="$CERT_DIR/clients/prometheus.pem"
CLIENT_KEY="$CERT_DIR/clients/prometheus-key.pem"
IMAGE=apache/rocketmq:5.5.0
EMPTY_CA="$(mktemp)"
cleanup() { rm -f "$EMPTY_CA"; }
trap cleanup EXIT

if ! docker inspect iboot-mysql-exporter >/dev/null 2>&1; then
    reg_log "  [SKIP] observability profile 未启动"
    reg_summary
    exit "$REG_FAILED"
fi

network_of() {
    docker inspect -f '{{range $name, $_ := .NetworkSettings.Networks}}{{$name}}{{"\n"}}{{end}}' "$1" 2>/dev/null | head -1
}

curl_exporter() {
    local container="$1" url="$2"
    shift 2
    docker run --rm --entrypoint curl --network "$(network_of "$container")" \
        -v "$CERT_DIR/runtime:/certs:ro" \
        "$IMAGE" -sS -o /dev/null -w '%{http_code}' "$@" "$url" 2>/dev/null || true
}

CHECKS=(
    "iboot-mysql-exporter|mysql-exporter|https://mysql-exporter:9104/metrics|http://mysql-exporter:9104/metrics"
    "iboot-redis-exporter|redis-exporter|https://redis-exporter:9121/metrics|http://redis-exporter:9121/metrics"
    "iboot-node-exporter|node-exporter|https://node-exporter:9100/metrics|http://node-exporter:9100/metrics"
    "iboot-elasticsearch-exporter|elasticsearch-exporter|https://elasticsearch-exporter:9114/metrics|http://elasticsearch-exporter:9114/metrics"
)

for entry in "${CHECKS[@]}"; do
    IFS='|' read -r container name https_url http_url <<< "$entry"
    if [ ! -f "$CERT_DIR/services/$name.pem" ]; then
        reg_fail "$name 证书不存在"
        continue
    fi
    code="$(curl_exporter "$container" "$https_url" \
        --cacert /certs/ca/rootCA.pem \
        --cert /certs/clients/prometheus.pem --key /certs/clients/prometheus-key.pem)"
    if [ "$code" = "200" ]; then
        reg_pass "$name 接受 Prometheus 客户端证书（200）"
    else
        reg_fail "$name 拒绝 Prometheus 客户端证书（HTTP ${code:-无响应}）"
    fi
    plain="$(curl_exporter "$container" "$http_url")"
    if [ "$plain" = "200" ]; then
        reg_fail "$name 仍接受明文 HTTP"
    else
        reg_pass "$name 拒绝明文（HTTP ${plain:-无响应}）"
    fi
    no_client="$(curl_exporter "$container" "$https_url" --cacert /certs/ca/rootCA.pem)"
    if [ "$no_client" = "200" ]; then
        reg_fail "$name 无客户端证书仍返回指标"
    else
        reg_pass "$name 拒绝无客户端证书（HTTP ${no_client:-无响应}）"
    fi
    wrong_ca="$(curl_exporter "$container" "$https_url" --cacert /certs/clients/prometheus.pem)"
    if [ "$wrong_ca" = "200" ]; then
        reg_fail "$name 错误 CA 仍返回指标"
    else
        reg_pass "$name 错误 CA 被拒绝（HTTP ${wrong_ca:-无响应}）"
    fi
done

reg_log "== Prometheus 抓取健康 =="
PROM="${REG_PROMETHEUS:-https://127.0.0.1:9090}"
if curl -sf --cacert "$CA" "$PROM/-/ready" >/dev/null 2>&1; then
    for name in mysql-exporter redis-exporter node-exporter elasticsearch-exporter; do
        up="$(curl -sS --cacert "$CA" --get "$PROM/api/v1/query" \
            --data-urlencode "query=up{job=\"$name\"}" 2>/dev/null \
            | grep -o '"value":\[[^]]*,"1"\]' | head -1 || true)"
        if [ -n "$up" ]; then
            reg_pass "Prometheus 抓取 $name 成功"
        else
            reg_fail "Prometheus 抓取 $name 失败"
        fi
    done
else
    reg_log "  [SKIP] Prometheus 未运行"
fi

reg_summary
