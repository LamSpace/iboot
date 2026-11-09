#!/usr/bin/env bash
# 日志管道回归：验证「服务 → Kafka → Logstash → Elasticsearch」的链路与降级行为。
#
# 用法：
#   log-pipeline.sh            只做非破坏性断言（可进 CI）
#   log-pipeline.sh --drills   额外执行停 Kafka / 停 Logstash 两种演练
#
# 断言：
#   1. 十个可部署进程（九个业务服务 + iboot-ops）都在 ES 中有日志记录；
#   2. 向 topic 灌入探针后，可按 app 与 traceId 在 ES 中检索到；
#   3. （--drills）停 Kafka 期间业务请求成功率 100%、延迟不显著劣化；恢复后日志自动续上；
#   4. （--drills）停 Logstash 期间日志留在 Kafka，恢复后补齐到 ES。
#
# 说明：第 1 条检查的是「该服务曾经把日志送进过 ES」（不受服务空闲影响），
# 当前链路是否活着由第 2 条的探针证明。
#
# 退出码：0 全部通过；1 存在失败；2 参数错误。

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

ES_URL="${LOG_ES_URL:-http://localhost:9200}"
GATEWAY_URL="${LOG_GATEWAY_URL:-http://localhost:8088}"
KAFKA_CONTAINER="${LOG_KAFKA_CONTAINER:-iboot-kafka}"
LOGSTASH_CONTAINER="${LOG_LOGSTASH_CONTAINER:-iboot-logstash}"
COMPOSE=(docker compose -f docker/docker-compose.yml)
WAIT_SECONDS="${LOG_WAIT_SECONDS:-30}"

# 可部署进程清单：九个业务服务 + 运维控制台。
APPS="iboot-system iboot-ai iboot-file iboot-auth iboot-notify iboot-job iboot-workflow iboot-monitor iboot-gateway iboot-ops"

DRILLS=0
for arg in "$@"; do
    case "$arg" in
        --drills) DRILLS=1 ;;
        -h|--help) sed -n '2,18p' "$0"; exit 0 ;;
        *) printf '[ERROR] 未知参数：%s\n' "$arg" >&2; exit 2 ;;
    esac
done

PASSED=0
FAILED=0
pass() { PASSED=$((PASSED + 1)); printf '  [PASS] %s\n' "$*"; }
fail() { FAILED=$((FAILED + 1)); printf '  [FAIL] %s\n' "$*"; }
die()  { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

# 查询 ES 并输出命中数。
#
# @param $1 _search 请求体
es_count() {
    curl -sS -X POST "$ES_URL/iboot-logs-*/_search" \
        -H 'Content-Type: application/json' -d "$1" \
        | python3 -c 'import json,sys; print(json.load(sys.stdin)["hits"]["total"]["value"])' 2>/dev/null || printf '0'
}

# 轮询等待某个查询出现至少一条命中。
#
# @param $1 查询体
# @param $2 描述
wait_count() {
    local body="$1" desc="$2" deadline=$((SECONDS + WAIT_SECONDS)) count=0
    while [ "$SECONDS" -lt "$deadline" ]; do
        count="$(es_count "$body")"
        [ "$count" -gt 0 ] && break
        sleep 2
    done
    printf '%s' "$count"
}

# 生成带唯一 traceId 的探针并写入 topic。
#
# @param $1 traceId
produce_probe() {
    local trace_id="$1"
    printf '%s\n' "{\"@timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%S.000)\",\"app\":\"iboot-probe\",\"traceId\":\"$trace_id\",\"message\":\"log-pipeline regress probe\"}" \
        | docker exec -i "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-console-producer.sh \
            --bootstrap-server localhost:9092 --topic iboot-logs >/dev/null 2>&1
}

# 等待 Kafka 容器健康。
wait_kafka() {
    local deadline=$((SECONDS + 90)) status
    while [ "$SECONDS" -lt "$deadline" ]; do
        status="$(docker inspect "$KAFKA_CONTAINER" --format '{{.State.Health.Status}}' 2>/dev/null)"
        [ "$status" = "healthy" ] && return 0
        sleep 3
    done
    return 1
}

# 以固定次数请求网关并输出「成功数/总次数/平均耗时(秒)」。
#
# @param $1 次数
http_probe() {
    local n="$1" ok=0 total=0 code
    for _ in $(seq 1 "$n"); do
        code="$(curl -s -o /dev/null -w '%{http_code} %{time_total}' "$GATEWAY_URL/actuator/health")"
        total="$(printf '%s\n%s' "$total" "${code#* }" | awk '{s+=$1} END {printf "%.6f", s}')"
        [ "${code%% *}" = "200" ] && ok=$((ok + 1))
    done
    printf '%s %s %.6f' "$ok" "$n" "$(awk -v s="$total" -v n="$n" 'BEGIN {print s / n}')"
}

printf '== 日志管道回归 ==\n'
curl -sS -o /dev/null "$ES_URL" || die "Elasticsearch 不可达：$ES_URL"

printf '\n[1] 十个可部署进程的日志覆盖\n'
for app in $APPS; do
    count="$(es_count "{\"query\":{\"term\":{\"app.keyword\":\"$app\"}},\"size\":0}")"
    if [ "$count" -gt 0 ]; then
        pass "$app 在 ES 中有 $count 条日志"
    else
        fail "$app 在 ES 中没有日志"
    fi
done

printf '\n[2] 探针经 Kafka → Logstash 进入 ES\n'
trace_id="regress-$(date +%s)"
produce_probe "$trace_id"
count="$(wait_count "{\"query\":{\"bool\":{\"must\":[{\"term\":{\"app.keyword\":\"iboot-probe\"}},{\"term\":{\"traceId.keyword\":\"$trace_id\"}}]}},\"size\":0}" "探针")"
if [ "$count" -gt 0 ]; then
    pass "按 app 与 traceId 检索到探针 $trace_id"
else
    fail "探针 $trace_id 未在 ${WAIT_SECONDS}s 内进入 ES（检查 Kafka 与 Logstash）"
fi

if [ "$DRILLS" -eq 1 ]; then
    printf '\n[3] 停 Kafka：业务不受影响，恢复后自动续投\n'
    read -r base_ok base_n base_avg <<<"$(http_probe 5)"
    printf '  基线：%s/%s 成功，平均 %.3fs\n' "$base_ok" "$base_n" "$base_avg"

    "${COMPOSE[@]}" stop kafka >/dev/null 2>&1 || die "无法停止 Kafka"
    read -r down_ok down_n down_avg <<<"$(http_probe 20)"
    printf '  停机：%s/%s 成功，平均 %.3fs\n' "$down_ok" "$down_n" "$down_avg"
    if [ "$down_ok" -eq "$down_n" ]; then
        pass "Kafka 停机期间请求全部成功"
    else
        fail "Kafka 停机期间 $((down_n - down_ok))/$down_n 次请求失败"
    fi
    if awk -v a="$down_avg" -v b="$base_avg" 'BEGIN {exit !(a <= b * 3 + 0.2)}'; then
        pass "Kafka 停机期间平均延迟未显著劣化"
    else
        fail "Kafka 停机期间延迟劣化：基线 ${base_avg}s，停机 ${down_avg}s"
    fi

    "${COMPOSE[@]}" start kafka >/dev/null 2>&1 || die "无法启动 Kafka"
    wait_kafka || fail "Kafka 在 90s 内未恢复健康"
    # 打开网关的包级 DEBUG，制造一条可检索的新日志
    curl -sS -o /dev/null -X POST -H 'Content-Type: application/json' \
        -d '{"configuredLevel":"DEBUG"}' "$GATEWAY_URL/actuator/loggers/org.springframework.cloud.gateway" || true
    marker="$(date -u +%Y-%m-%dT%H:%M:%S)"
    sleep 1
    curl -sS -o /dev/null "$GATEWAY_URL/api/system/users" || true
    count="$(wait_count "{\"query\":{\"bool\":{\"must\":[{\"term\":{\"app.keyword\":\"iboot-gateway\"}},{\"range\":{\"@timestamp\":{\"gte\":\"$marker\"}}}]}},\"size\":0}" "恢复后日志")"
    if [ "$count" -gt 0 ]; then
        pass "Kafka 恢复后网关日志自动续投"
    else
        fail "Kafka 恢复后未观察到新的网关日志"
    fi

    printf '\n[4] 停 Logstash：日志留在 Kafka，恢复后回放\n'
    "${COMPOSE[@]}" stop logstash >/dev/null 2>&1 || die "无法停止 Logstash"
    sleep 2
    trace_id="replay-$(date +%s)"
    produce_probe "$trace_id"
    sleep 5
    count="$(es_count "{\"query\":{\"term\":{\"traceId.keyword\":\"$trace_id\"}},\"size\":0}")"
    if [ "$count" -eq 0 ]; then
        pass "Logstash 停机期间日志未进入 ES（留在 Kafka）"
    else
        fail "Logstash 停机期间日志仍进入 ES，停止未生效"
    fi
    "${COMPOSE[@]}" start logstash >/dev/null 2>&1 || die "无法启动 Logstash"
    count="$(wait_count "{\"query\":{\"term\":{\"traceId.keyword\":\"$trace_id\"}},\"size\":0}" "回放")"
    if [ "$count" -gt 0 ]; then
        pass "Logstash 恢复后日志从 Kafka 回放到 ES"
    else
        fail "Logstash 恢复后未回放日志 $trace_id"
    fi
fi

printf '\n== 结果：%s 通过，%s 失败 ==\n' "$PASSED" "$FAILED"
[ "$FAILED" -eq 0 ]
