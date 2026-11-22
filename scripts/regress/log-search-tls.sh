#!/usr/bin/env bash
# 日志与检索面 TLS 回归：Elasticsearch、Kafka、Logstash 与 RocketMQ 加密边界。
#
# 前置：
#   - Elasticsearch / Kafka / Logstash 已按 TLS 配置启动；
#   - docker/certs 已由 scripts/tls-init.sh 生成；
#   - 需要 elasticsearch / kafka / logstash 容器（可覆盖名称）。
#
# 环境变量：
#   REG_CACERT              本地 CA，默认 docker/certs/ca/rootCA.pem
#   LOG_ES_URL              ES 地址，默认 https://localhost:9200
#   LOG_ES_USER / LOG_ES_PASSWORD  ES 凭据
#   LOG_KAFKA_CONTAINER     默认 iboot-kafka
#   KAFKA_LOG_PASSWORD      iboot_log 的 SASL 口令
#   KAFKA_TOOLS_PASSWORD    iboot_tools 的 SASL 口令
#   LOG_LOGSTASH_CONTAINER  默认 iboot-logstash
#   LOG_ROCKETMQ_PORT       默认 18081；为空时跳过

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
cd "$ROOT"

# shellcheck source=lib.sh
. "$HERE/lib.sh"

ES_URL="${LOG_ES_URL:-https://localhost:9200}"
ES_USER="${LOG_ES_USER:-elastic}"
ES_PASSWORD="${LOG_ES_PASSWORD:-${ELASTIC_PASSWORD:-iboot-dev-elastic}}"
KAFKA_CONTAINER="${LOG_KAFKA_CONTAINER:-iboot-kafka}"
KAFKA_LOG_PASSWORD="${KAFKA_LOG_PASSWORD:-}"
KAFKA_TOOLS_PASSWORD="${KAFKA_TOOLS_PASSWORD:-}"
if [ -f "$ROOT/.env" ]; then
    [ -n "$KAFKA_LOG_PASSWORD" ] || KAFKA_LOG_PASSWORD="$(sed -n 's/^KAFKA_LOG_PASSWORD=//p' "$ROOT/.env" | tail -1)"
    [ -n "$KAFKA_TOOLS_PASSWORD" ] || KAFKA_TOOLS_PASSWORD="$(sed -n 's/^KAFKA_TOOLS_PASSWORD=//p' "$ROOT/.env" | tail -1)"
fi
LOGSTASH_CONTAINER="${LOG_LOGSTASH_CONTAINER:-iboot-logstash}"
ROCKETMQ_PORT="${LOG_ROCKETMQ_PORT:-18081}"
CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
EMPTY_CA="$(mktemp)"
cleanup() { rm -f "$EMPTY_CA"; }
trap cleanup EXIT

# 等待 ES 返回指定状态码。
# @param $1 状态码
# @param $2 超时秒数
wait_es_status() {
    local expected="$1" timeout="${2:-30}" i code
    for i in $(seq 1 "$timeout"); do
        code="$(curl -sS -o /dev/null -w '%{http_code}' --cacert "$CA" -u "$ES_USER:$ES_PASSWORD" "$ES_URL/_cluster/health" 2>/dev/null || true)"
        [ "$code" = "$expected" ] && return 0
        sleep 1
    done
    return 1
}

reg_log "== 1. 本地 CA =="
if [ -f "$CA" ]; then
    reg_pass "本地 CA 存在：$CA"
else
    reg_fail "本地 CA 不存在：$CA（执行 scripts/tls-init.sh）"
    reg_summary
    exit 1
fi

reg_log "== 2. Elasticsearch =="
if wait_es_status 200 30; then
    reg_pass "ES HTTPS + 凭据返回 200"
else
    reg_fail "ES HTTPS + 凭据未返回 200"
fi
plain="$(curl -sS -o /dev/null -w '%{http_code}' "http://localhost:9200/_cluster/health" 2>/dev/null || true)"
if [ "$plain" = "200" ]; then
    reg_fail "ES 仍接受明文 HTTP"
else
    reg_pass "ES 拒绝明文 HTTP（${plain:-无响应}）"
fi
no_ca="$(curl -sS -o /dev/null -w '%{http_code}' "https://localhost:9200/_cluster/health" 2>/dev/null || true)"
if [ "$no_ca" = "200" ]; then
    reg_fail "ES 不带 CA 仍可访问"
else
    reg_pass "ES 不带 CA 时证书校验失败（${no_ca:-无响应}）"
fi
wrong="$(curl -sS -o /dev/null -w '%{http_code}' --cacert "$CA" -u "$ES_USER:wrong-password" "$ES_URL/_cluster/health" 2>/dev/null || true)"
if [ "$wrong" = "401" ]; then
    reg_pass "ES 错误凭据返回 401"
else
    reg_fail "ES 错误凭据未返回 401（HTTP ${wrong:-无响应}）"
fi
if echo | timeout 10 openssl s_client -connect localhost:9200 -tls1_2 -CAfile "$CA" 2>&1 | grep -q "Verify return code: 0"; then
    reg_pass "ES 9200 接受 TLS 1.2"
else
    reg_fail "ES 9200 TLS 1.2 握手失败"
fi

reg_log "== 3. Kafka =="
if docker inspect "$KAFKA_CONTAINER" >/dev/null 2>&1; then
    if echo | timeout 10 openssl s_client -connect localhost:29092 -tls1_2 -CAfile "$CA" 2>&1 | grep -q "Verify return code: 0"; then
        reg_pass "Kafka 宿主机监听接受 TLS 1.2"
    else
        reg_fail "Kafka 宿主机监听 TLS 握手失败"
    fi
    kafka_client_config() {
        local user="$1" password="$2"
        {
            printf 'security.protocol=SASL_SSL\n'
            printf 'sasl.mechanism=PLAIN\n'
            printf 'sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="%s" password="%s";\n' "$user" "$password"
            printf 'ssl.truststore.location=/etc/kafka/secrets/truststore.p12\n'
            printf 'ssl.truststore.password=changeit\n'
            printf 'ssl.truststore.type=PKCS12\n'
            printf 'ssl.endpoint.identification.algorithm=HTTPS\n'
        } | docker exec -i "$KAFKA_CONTAINER" sh -c 'cat > /tmp/regress-kafka-client.properties'
    }

    kafka_client_config iboot_tools "$KAFKA_TOOLS_PASSWORD"
    if docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-broker-api-versions.sh \
        --bootstrap-server localhost:9092 \
        --command-config /tmp/regress-kafka-client.properties >/dev/null 2>&1; then
        reg_pass "Kafka SASL_SSL 客户端可读取 broker API 版本"
    else
        reg_fail "Kafka SASL_SSL 客户端连接失败"
    fi
    if docker exec "$KAFKA_CONTAINER" bash -lc 'timeout 10 /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/dev/null 2>&1'; then
        reg_fail "Kafka 仍接受 PLAINTEXT 客户端"
    else
        reg_pass "Kafka 拒绝 PLAINTEXT 客户端"
    fi
    if docker exec "$KAFKA_CONTAINER" bash -lc 'printf "security.protocol=SASL_SSL\nsasl.mechanism=PLAIN\n" > /tmp/regress-kafka-no-trust.properties; timeout 10 /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 --command-config /tmp/regress-kafka-no-trust.properties >/dev/null 2>&1'; then
        reg_fail "Kafka SASL 客户端无 truststore 仍成功"
    else
        reg_pass "Kafka SASL 客户端无 truststore 失败"
    fi
else
    reg_fail "找不到 Kafka 容器：$KAFKA_CONTAINER"
fi

reg_log "== 4. Logstash 全链路 =="
if docker inspect "$LOGSTASH_CONTAINER" >/dev/null 2>&1; then
    trace_id="regress-log-tls-$(date +%s)-$RANDOM"
    printf '%s\n' "{\"@timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%S.000Z)\",\"app\":\"iboot-probe\",\"traceId\":\"$trace_id\",\"message\":\"log-search tls regress\"}" \
        | docker exec -i "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-console-producer.sh \
            --bootstrap-server localhost:9092 --topic iboot-logs \
            --command-property security.protocol=SASL_SSL \
            --command-property sasl.mechanism=PLAIN \
            --command-property "sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"iboot_log\" password=\"${KAFKA_LOG_PASSWORD}\";" \
            --command-property ssl.truststore.location=/etc/kafka/secrets/truststore.p12 \
            --command-property ssl.truststore.password=changeit \
            --command-property ssl.truststore.type=PKCS12 >/dev/null 2>&1
    hits=0
    for _ in $(seq 1 30); do
        hits="$(curl -sS --cacert "$CA" -u "$ES_USER:$ES_PASSWORD" \
            "$ES_URL/iboot-logs-*/_count?q=traceId:$trace_id" 2>/dev/null \
            | python3 -c 'import json,sys; print(json.load(sys.stdin)["count"])' 2>/dev/null || printf 0)"
        [ "$hits" -gt 0 ] && break
        sleep 2
    done
    if [ "$hits" -gt 0 ]; then
        reg_pass "探针经 Kafka → Logstash → ES HTTPS 命中"
    else
        reg_fail "探针未在 ES 中命中（traceId=$trace_id）"
    fi
else
    reg_fail "找不到 Logstash 容器：$LOGSTASH_CONTAINER"
fi

reg_log "== 5. RocketMQ 加密边界 =="
if [ -n "$ROCKETMQ_PORT" ] && (echo > "/dev/tcp/localhost/$ROCKETMQ_PORT") >/dev/null 2>&1; then
    if echo | timeout 10 openssl s_client -connect "localhost:$ROCKETMQ_PORT" -tls1_2 -CAfile "$CA" 2>&1 | grep -q "Verify return code: 0"; then
        reg_pass "RocketMQ Proxy 接受 TLS 1.2 且证书可被本地 CA 验证"
    else
        reg_fail "RocketMQ Proxy TLS 握手失败（端口 $ROCKETMQ_PORT）"
    fi
    reg_log "  [INFO] 同一端口仍接受明文：这是已登记的加密边界，不计为 TLS 通过"
    reg_log "  [INFO] 客户端不校验服务端证书：RocketMQ 只作为加密通道，不提供身份认证"
else
    reg_log "  跳过：RocketMQ Proxy 端口不可达（$ROCKETMQ_PORT）"
fi

reg_summary
