#!/usr/bin/env bash
# 量化 SkyWalking Java Agent 对启动时间、常驻内存与吞吐的影响。
#
# 用法：scripts/apm/measure.sh [--runs N] [--concurrency N] [--requests N]
# 默认以 iboot-system 为样本，在「挂 Agent」与「不挂 Agent」两组下各重启 N 次。
#
# 注意：脚本会反复停止并重启 iboot-system；运行期间其他服务对系统域的调用可能短暂失败。
# 前置：中间件与其余服务已启动；挂 Agent 组需要辅助栈已运行、Agent 已下载。
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
# shellcheck source=../regress/lib.sh
. "$ROOT/scripts/regress/lib.sh"

SERVICE="iboot-system"
PORT="8080"
VERSION="1.0.0"
JAR="$SERVICE/target/$SERVICE-$VERSION.jar"
AGENT="${IBOOT_SKYWALKING_AGENT:-$ROOT/docker/skywalking/agent/skywalking-agent.jar}"
OAP="${IBOOT_SKYWALKING_OAP:-127.0.0.1:11800}"
GATEWAY="${APM_GATEWAY:-http://localhost:8088}"
RUNS=3
CONCURRENCY=8
REQUESTS=100
WARMUP=10

while [ $# -gt 0 ]; do
    case "$1" in
        --runs) RUNS="${2:-}"; shift 2 ;;
        --concurrency) CONCURRENCY="${2:-}"; shift 2 ;;
        --requests) REQUESTS="${2:-}"; shift 2 ;;
        -h|--help)
            sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) printf '[ERROR] 未知参数：%s\n' "$1" >&2; exit 2 ;;
    esac
done

[ -f "$JAR" ] || { printf '[ERROR] 缺少 %s，先执行 scripts/dev-init.sh --build\n' "$JAR" >&2; exit 2; }
[ -f "$AGENT" ] || { printf '[ERROR] 缺少 Agent：%s，先执行 scripts/apm/fetch-agent.sh\n' "$AGENT" >&2; exit 2; }
command -v jcmd >/dev/null 2>&1 || { printf '[ERROR] 缺少 jcmd（需要 JDK 而非 JRE）\n' >&2; exit 2; }

if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

# 与 dev-init.sh 保持一致的宿主机运行默认值。
export MANAGEMENT_HEALTH_MAIL_ENABLED=false
export SENTINEL_DASHBOARD="${SENTINEL_DASHBOARD:-localhost:8858}"
export IBOOT_ROCKETMQ_ENDPOINTS="${IBOOT_ROCKETMQ_ENDPOINTS:-localhost:18081}"
export IBOOT_SYSTEM_INTERNAL_TOKEN="${IBOOT_SYSTEM_INTERNAL_TOKEN:-iboot-dev-internal-token}"

service_pid() {
    pgrep -f -- "-jar $JAR" 2>/dev/null | head -1
}

stop_service() {
    local pid
    pid="$(service_pid)"
    [ -n "$pid" ] || return 0
    kill "$pid" 2>/dev/null || true
    for _ in $(seq 1 30); do
        kill -0 "$pid" 2>/dev/null || return 0
        sleep 1
    done
    kill -9 "$pid" 2>/dev/null || true
}

wait_health() {
    local deadline=$(( $(date +%s) + 240 ))
    while [ "$(date +%s)" -lt "$deadline" ]; do
        if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORT/actuator/health" 2>/dev/null)" = "200" ]; then
            return 0
        fi
        sleep 0.5
    done
    return 1
}

start_service() {
    local mode="$1" log="$2" pid
    local -a opts=()
    if [ "$mode" = "agent" ]; then
        opts=(
            "-javaagent:$AGENT"
            "-Dskywalking.agent.service_name=$SERVICE"
            "-Dskywalking.collector.backend_service=$OAP"
            "-Dskywalking.agent.sample_n_per_3_secs=-1"
        )
    fi
    nohup java "${opts[@]}" -jar "$JAR" > "$log" 2>&1 &
    pid=$!
    echo "$pid" > "$ROOT/logs/$SERVICE.pid"
    printf '%s' "$pid"
}

run_load() {
    local start_ms end_ms ok total
    export REG_TOKEN GATEWAY REQUESTS
    start_ms="$(date +%s%3N)"
    seq 1 "$CONCURRENCY" | xargs -P "$CONCURRENCY" -I{} bash -c '
        for ((i = 0; i < REQUESTS; i++)); do
            curl -s -o /dev/null -w "%{http_code}\n" \
                -H "Authorization: Bearer $REG_TOKEN" "$GATEWAY/api/user/profile"
        done
    ' > "$WORK/codes"
    end_ms="$(date +%s%3N)"
    total=$(( CONCURRENCY * REQUESTS ))
    ok="$(grep -c '^200$' "$WORK/codes" || true)"
    printf '%s %s %s' "$(( end_ms - start_ms ))" "$ok" "$total"
}

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

reg_login || { printf '[ERROR] 登录失败，无法进行吞吐测量\n' >&2; exit 3; }

printf '样本=%s 轮次=%s 并发=%s 每并发请求=%s\n\n' "$SERVICE" "$RUNS" "$CONCURRENCY" "$REQUESTS"
printf '| 组别 | 轮次 | 启动(ms) | RSS(MB) | 堆使用(MB) | 吞吐(请求/秒) | 成功/总数 |\n'
printf '|---|---|---|---|---|---|---|\n'

for mode in no-agent agent; do
    for run in $(seq 1 "$RUNS"); do
        stop_service
        # 冷启动：先移除页缓存影响不现实，至少保证进程是新的。
        START_MS="$(date +%s%3N)"
        PID="$(start_service "$mode" "$WORK/$mode-$run.log")"
        if ! wait_health; then
            printf '| %s | %s | 启动失败 | - | - | - | - |\n' "$mode" "$run"
            continue
        fi
        STARTUP_MS=$(( $(date +%s%3N) - START_MS ))
        sleep "$WARMUP"
        RSS_KB="$(ps -o rss= -p "$PID" 2>/dev/null | tr -d ' ')"
        HEAP_KB="$(jcmd "$PID" GC.heap_info 2>/dev/null | grep -oE 'used [0-9]+K' | head -1 | grep -oE '[0-9]+' || true)"
        HEAP_MB=$(( ${HEAP_KB:-0} / 1024 ))
        [ -n "$RSS_KB" ] || RSS_KB=0
        read -r ELAPSED_MS OK TOTAL <<< "$(run_load)"
        THROUGHPUT="$(awk -v ok="$OK" -v ms="$ELAPSED_MS" 'BEGIN { if (ms > 0) printf "%.1f", ok * 1000 / ms; else print "-" }')"
        printf '| %s | %s | %s | %s | %s | %s | %s/%s |\n' \
            "$mode" "$run" "$STARTUP_MS" "$(( RSS_KB / 1024 ))" "$HEAP_MB" "$THROUGHPUT" "$OK" "$TOTAL"
    done
done

# 恢复为挂 Agent 的运行状态，与实验默认一致。
stop_service
start_service agent "$WORK/restore.log" >/dev/null
wait_health || printf '[WARN] 恢复挂 Agent 的 %s 失败，请检查 %s\n' "$SERVICE" "$WORK/restore.log" >&2
printf '\n测量完成；%s 已恢复为挂 Agent 的运行状态。\n' "$SERVICE"
