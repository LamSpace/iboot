#!/usr/bin/env bash
# 一键拉起本地开发环境：中间件 → 构建（按需）→ db-init 建库建表 → 启动后端 → 等待健康。
#
# 用法：
#   dev-init.sh              拉起中间件、初始化数据库并启动九个业务服务与运维控制台
#   dev-init.sh --build      强制重新构建后端
#   dev-init.sh --frontend   同时安装前端依赖并启动开发服务器
#   dev-init.sh --status     查看中间件、后端与前端的状态
#   dev-init.sh --stop       停止后端与前端（中间件保留）
#   dev-init.sh --down       停止服务并移除中间件容器（数据卷保留）
#   dev-init.sh --purge      停止服务并移除中间件容器与数据卷（数据不可恢复）
#
# 密钥与私有配置沿用根目录的 .env（已在 .gitignore 中，模板见 .env.example），
# 与 README 里 `set -a; source .env; set +a` 的既有约定一致；脚本会自动加载。
#
# 说明：
# - 中间件默认启动开发必需的一组（mysql/redis/silo/es + nacos/tempo + seata + rocketmq），
#   并随环境启动集中日志（kafka/logstash）与可观测平台（Prometheus/Grafana/Kibana/
#   Alertmanager/Thanos 及 exporter）；资源紧张时用 IBOOT_OBSERVABILITY_ENABLED=false
#   或 IBOOT_LOGGING_KAFKA_ENABLED=false 关闭；
# - 数据库 iboot 与全部表结构由 iboot-db-init 创建，服务不再自带迁移；
# - 后端日志写入 logs/，进程号写入 logs/<name>.pid；
# - SkyWalking APM 对比实验由 IBOOT_SKYWALKING_ENABLED 控制，默认关闭。
#   打开前先执行 scripts/apm/fetch-agent.sh，并启动 docker compose --profile apm。

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

COMPOSE=(docker compose -f docker/docker-compose.yml)
LOG_DIR="$ROOT/logs"
MAVEN_SETTINGS="${MAVEN_SETTINGS:-$HOME/repo/settings.xml}"
[ -f "$MAVEN_SETTINGS" ] || MAVEN_SETTINGS=""
VERSION="1.0.0"

# 后端服务：名称:端口
# 系统服务必须排在最前（监控服务除外）：它负责 Flyway 建表，其余服务启动时要读共享库（sys_config、sys_user 等）。
# iboot-ops 是旁路的运维控制台，可随 IBOOT_OPS_ENABLED=false 关闭而不影响业务服务。
SERVICES=("iboot-monitor:8087" "iboot-system:8080" "iboot-auth:8083" "iboot-file:8082" "iboot-notify:8084" "iboot-job:8085" "iboot-workflow:8086" "iboot-ai:8081" "iboot-gateway:8088" "iboot-ops:8089")

FORCE_BUILD=0
WITH_FRONTEND=0
ACTION="up"

for arg in "$@"; do
    case "$arg" in
        --build)    FORCE_BUILD=1 ;;
        --frontend) WITH_FRONTEND=1 ;;
        --status)   ACTION="status" ;;
        --stop)     ACTION="stop" ;;
        --down)     ACTION="down" ;;
        --purge)    ACTION="purge" ;;
        -h|--help)  awk 'NR>1 && /^#/ { sub(/^# ?/, ""); print; next } NR>1 { exit }' "$0"; exit 0 ;;
        *) printf '[ERROR] 未知参数：%s\n' "$arg" >&2; exit 2 ;;
    esac
done

log()  { printf '%s\n' "$*"; }
fail() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

# 加载本地环境变量；不存在时跳过。
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

# 中间件账号鉴权：缺少凭据时不启动，避免回退到默认口令。
for required in MYSQL_ROOT_PASSWORD MYSQL_EXPORTER_PASSWORD \
    MIDDLEWARE_SYSTEM_PASSWORD MIDDLEWARE_WORKFLOW_PASSWORD MIDDLEWARE_FILE_PASSWORD \
    MIDDLEWARE_NOTIFY_PASSWORD MIDDLEWARE_JOB_PASSWORD MIDDLEWARE_MONITOR_PASSWORD \
    MIDDLEWARE_AUTH_PASSWORD MIDDLEWARE_AI_PASSWORD MIDDLEWARE_GATEWAY_PASSWORD \
    MIDDLEWARE_OPS_PASSWORD \
    REDIS_EXPORTER_PASSWORD SILO_ROOT_USER SILO_ROOT_PASSWORD \
    SILO_FILE_SECRET_KEY SILO_AI_SECRET_KEY \
    ROCKETMQ_ADMIN_ACCESS_KEY ROCKETMQ_ADMIN_SECRET_KEY \
    ROCKETMQ_WORKFLOW_ACCESS_KEY ROCKETMQ_WORKFLOW_SECRET_KEY \
    ROCKETMQ_NOTIFY_ACCESS_KEY ROCKETMQ_NOTIFY_SECRET_KEY \
    NACOS_USERNAME NACOS_PASSWORD NACOS_ADMIN_PASSWORD NACOS_AUTH_TOKEN \
    NEO4J_PASSWORD ELASTIC_PASSWORD ES_AI_PASSWORD ES_NOTIFY_PASSWORD \
    ES_MONITOR_PASSWORD ES_LOGSTASH_PASSWORD ES_GRAFANA_PASSWORD \
    ES_EXPORTER_PASSWORD GRAFANA_ADMIN_PASSWORD; do
    [ -n "${!required:-}" ] || fail "缺少中间件凭据：$required（请检查根目录 .env）"
done
if [ "${IBOOT_LOGGING_KAFKA_ENABLED:-true}" = "true" ]; then
    for required in KAFKA_BROKER_PASSWORD KAFKA_LOG_PASSWORD KAFKA_LOGSTASH_PASSWORD KAFKA_TOOLS_PASSWORD; do
        [ -n "${!required:-}" ] || fail "缺少 Kafka 凭据：$required（请检查根目录 .env）"
    done
fi
# 每服务账号在 start_backend 中按进程注入；这里不再导出共享身份。
# 图数据库口令与 Neo4j 服务端同源，避免两处维护后漂移。
export IBOOT_AI_GRAPH_PASSWORD="${IBOOT_AI_GRAPH_PASSWORD:-$NEO4J_PASSWORD}"

# 运维控制台：默认开启，可通过 IBOOT_OPS_ENABLED=false 关闭以节省本机内存。
ops_enabled() { [ "${IBOOT_OPS_ENABLED:-true}" = "true" ]; }

# 门户 TLS：默认开启；关闭仅用于排障，回归与验收不得在该模式下判定通过。
tls_enabled() { [ "${IBOOT_TLS_ENABLED:-true}" = "true" ]; }

# 内部服务 TLS：默认开启；关闭仅用于宿主机排障，Docker 不提供该回退。
internal_tls_enabled() { [ "${IBOOT_INTERNAL_TLS_ENABLED:-true}" = "true" ]; }

# 内部服务 scheme：启用内部 TLS 时为 https，否则为 http。
service_scheme() {
    if internal_tls_enabled; then printf 'https'; else printf 'http'; fi
}

# 当前模式的公开来源：宿主机开发默认 Vite；关闭 TLS 时使用 HTTP 排障地址。
public_origin() {
    if tls_enabled; then
        printf '%s' "${IBOOT_PUBLIC_ORIGIN:-https://localhost:5173}"
    else
        printf '%s' "${IBOOT_PUBLIC_ORIGIN_HTTP:-http://localhost:5173}"
    fi
}

# SkyWalking APM 对比实验：默认关闭；打开前需先执行 scripts/apm/fetch-agent.sh
# 并启动 docker compose --profile apm。只影响启动参数，不改业务代码与依赖。
SKYWALKING_AGENT="${IBOOT_SKYWALKING_AGENT:-$ROOT/docker/skywalking/agent/skywalking-agent.jar}"
SKYWALKING_OAP="${IBOOT_SKYWALKING_OAP:-127.0.0.1:11800}"
skywalking_enabled() { [ "${IBOOT_SKYWALKING_ENABLED:-false}" = "true" ]; }

# RocketMQ 是通知链路的中间件；关闭后审批仍可提交，事件保留在 outbox。
rocketmq_enabled() { [ "${IBOOT_ROCKETMQ_ENABLED:-true}" = "true" ]; }

# 集中日志（Kafka → Logstash → ES）：默认开启，运行日志页面依赖该链路。
logging_enabled() { [ "${IBOOT_LOGGING_KAFKA_ENABLED:-true}" = "true" ]; }

# ES 只接受 HTTPS + 认证；宿主机进程与 compose 使用同一组凭据。
ELASTICSEARCH_URIS="${ELASTICSEARCH_URIS:-https://localhost:9200}"
case "$ELASTICSEARCH_URIS" in
    http://*) ELASTICSEARCH_URIS="https://${ELASTICSEARCH_URIS#http://}" ;;
esac
export ELASTICSEARCH_URIS
export ELASTICSEARCH_USERNAME="${ELASTICSEARCH_USERNAME:-elastic}"
export ELASTICSEARCH_PASSWORD="${ELASTICSEARCH_PASSWORD:-${ELASTIC_PASSWORD:-iboot-dev-elastic}}"

# 宿主机进程通过映射端口连容器内的 Kafka，使用本地 CA 信任库。
export IBOOT_LOGGING_KAFKA_SECURITY_PROTOCOL="${IBOOT_LOGGING_KAFKA_SECURITY_PROTOCOL:-SASL_SSL}"
export IBOOT_LOGGING_KAFKA_SASL_MECHANISM="${IBOOT_LOGGING_KAFKA_SASL_MECHANISM:-PLAIN}"
export IBOOT_LOGGING_KAFKA_SASL_USERNAME="${IBOOT_LOGGING_KAFKA_SASL_USERNAME:-iboot_log}"
export IBOOT_LOGGING_KAFKA_SASL_PASSWORD="${IBOOT_LOGGING_KAFKA_SASL_PASSWORD:-${KAFKA_LOG_PASSWORD:-}}"
export IBOOT_LOGGING_KAFKA_TRUSTSTORE_LOCATION="${IBOOT_LOGGING_KAFKA_TRUSTSTORE_LOCATION:-$ROOT/docker/certs/truststore.p12}"
export IBOOT_LOGGING_KAFKA_TRUSTSTORE_PASSWORD="${IBOOT_LOGGING_KAFKA_TRUSTSTORE_PASSWORD:-changeit}"
export IBOOT_LOGGING_KAFKA_TRUSTSTORE_TYPE="${IBOOT_LOGGING_KAFKA_TRUSTSTORE_TYPE:-PKCS12}"

# RocketMQ 使用 TLS-only 入口；客户端不校验服务端证书。
export IBOOT_ROCKETMQ_SSL_ENABLED="${IBOOT_ROCKETMQ_SSL_ENABLED:-true}"

# 服务间授权依赖客户端证书；明文排障模式默认关闭，可显式覆盖。
export IBOOT_SERVICE_AUTHORIZATION_ENABLED="${IBOOT_SERVICE_AUTHORIZATION_ENABLED:-${IBOOT_INTERNAL_TLS_ENABLED:-true}}"

# 可观测平台：默认随开发环境启动；资源紧张时可关闭。
observability_enabled() { [ "${IBOOT_OBSERVABILITY_ENABLED:-true}" = "true" ]; }

# 控制台密码在生产环境必须由密钥管理注入；本地开发使用固定默认值。
export IBOOT_OPS_USER="${IBOOT_OPS_USER:-ops}"
export IBOOT_OPS_PASSWORD="${IBOOT_OPS_PASSWORD:-ops123}"
# 本地开发默认关闭控制台认证，门户 iframe 可直接打开；生产必须保持开启。
export IBOOT_OPS_SECURITY_ENABLED="${IBOOT_OPS_SECURITY_ENABLED:-false}"

# 宿主机开发进程连接容器内的 Sentinel Dashboard（必选治理控制台）。
export SENTINEL_DASHBOARD="${SENTINEL_DASHBOARD:-https://localhost:8858}"

# 宿主机服务通过 Nacos 以回环地址互相发现，证书 SAN 覆盖 127.0.0.1；
# Docker 部署不设置该变量，实例地址回落到服务名。
export IBOOT_NACOS_DISCOVERY_IP="${IBOOT_NACOS_DISCOVERY_IP:-127.0.0.1}"

# 内部 Silo endpoint 使用 HTTPS；即使没有 .env 也避免退回 sys_config 的明文地址
export IBOOT_FILE_SILO_ENDPOINT="${IBOOT_FILE_SILO_ENDPOINT:-https://localhost:9000}"
export IBOOT_FILE_SILO_PUBLIC_ENDPOINT="${IBOOT_FILE_SILO_PUBLIC_ENDPOINT:-https://localhost:9000}"
export IBOOT_AI_SILO_ENDPOINT="${IBOOT_AI_SILO_ENDPOINT:-https://localhost:9000}"

# 宿主机进程连接 RocketMQ TLS 入口的映射端口；容器内由 compose 注入服务名。
export IBOOT_ROCKETMQ_ENDPOINTS="${IBOOT_ROCKETMQ_ENDPOINTS:-localhost:18081}"

# 审批额度内部端点共享密钥；未配置时使用仅限本地的开发默认值，两端保持一致。
export IBOOT_SYSTEM_INTERNAL_TOKEN="${IBOOT_SYSTEM_INTERNAL_TOKEN:-iboot-dev-internal-token}"

mkdir -p "$LOG_DIR"

# 备份能力需要密钥；没有配置时用仅限本地的开发默认值。
export IBOOT_BACKUP_ENABLED="${IBOOT_BACKUP_ENABLED:-true}"
export IBOOT_BACKUP_ENCRYPTION_KEY="${IBOOT_BACKUP_ENCRYPTION_KEY:-MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}"

# 取服务进程号：优先用 pid 文件；文件丢失时按 jar 名匹配。
# logs/ 被清理时会连带丢掉 pid 文件，仅靠 pid 文件会把仍在运行的进程当成"未运行"。
service_pid() {
    local name="$1" pidfile="$LOG_DIR/$1.pid" pid
    if [ -f "$pidfile" ]; then
        pid="$(cat "$pidfile")"
        if kill -0 "$pid" 2>/dev/null; then
            printf '%s' "$pid"
            return 0
        fi
    fi
    # 挂 Agent 时启动命令在 -jar 前多了 -javaagent/-D 参数，不能再用 "java -jar" 作为前缀匹配。
    pid="$(pgrep -f -- "-jar $name/target/$name-$VERSION.jar" 2>/dev/null | head -1)"
    [ -n "$pid" ] && printf '%s' "$pid"
}

stop_service() {
    local name="$1" pidfile="$LOG_DIR/$1.pid" pid
    pid="$(service_pid "$name")"
    if [ -n "$pid" ]; then
        log "  停止 $name（pid $pid）"
        kill "$pid" 2>/dev/null
        for _ in $(seq 1 30); do
            kill -0 "$pid" 2>/dev/null || break
            sleep 1
        done
        kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
    fi
    rm -f "$pidfile"
}

# 前端开发服务器的进程号：优先 pid 文件，失效时按 vite 的命令行匹配。
# `cnpm run dev` 会 fork 出 vite，只杀父进程会留下孤儿，所以按命令行再兜一次。
frontend_pids() {
    local pidfile="$LOG_DIR/portal.pid" pids="" pid
    if [ -f "$pidfile" ]; then
        pid="$(cat "$pidfile")"
        kill -0 "$pid" 2>/dev/null && pids="$pid"
    fi
    pids="$(printf '%s\n%s\n' "$pids" "$(pgrep -f 'iboot-portal/node_modules/.*vite/bin/vite\.js' 2>/dev/null)" \
        | grep -v '^$' | sort -u | tr '\n' ' ')"
    printf '%s' "$pids"
}

stop_frontend() {
    local pidfile="$LOG_DIR/portal.pid" pids pid
    pids="$(frontend_pids)"
    if [ -n "$pids" ]; then
        log "  停止前端（pid $pids）"
        kill $pids 2>/dev/null
        sleep 2
        for pid in $pids; do
            kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
        done
    fi
    rm -f "$pidfile"
}

# 宿主回环 relay：原生 Docker 无法访问宿主机 127.0.0.1 时，把 Docker 网桥网关的同端口转发到回环。
relay_pid() {
    local pidfile="$LOG_DIR/host-loopback-relay.pid" pid
    [ -f "$pidfile" ] || return 1
    pid="$(cat "$pidfile")"
    kill -0 "$pid" 2>/dev/null && printf '%s' "$pid"
}

stop_host_loopback_relay() {
    local pid
    pid="$(relay_pid)" || {
        rm -f "$LOG_DIR/host-loopback-relay.pid"
        return 0
    }
    log "  停止宿主回环 relay（pid $pid）"
    kill "$pid" 2>/dev/null
    for _ in $(seq 1 10); do
        kill -0 "$pid" 2>/dev/null || break
        sleep 1
    done
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
    rm -f "$LOG_DIR/host-loopback-relay.pid"
}

docker_gateways() {
    local net
    docker network ls --format '{{.Name}}' 2>/dev/null | while IFS= read -r net; do
        docker network inspect "$net" -f '{{range .IPAM.Config}}{{.Gateway}}{{"\n"}}{{end}}' 2>/dev/null
    done | grep -E '^[0-9]{1,3}(\.[0-9]{1,3}){3}$' | grep -v '^127\.' | sort -u | paste -sd, -
}

# 用临时回环监听器判断容器能否经 host.docker.internal 访问宿主机回环。
# 返回 0 表示 Docker Desktop 代理可用，不需要 relay；返回 1 表示需要 relay。
host_loopback_reachable_from_docker() {
    local probe_port="${IBOOT_HOST_LOOPBACK_PROBE_PORT:-18099}" pid
    python3 - "$probe_port" <<'PY' >/dev/null 2>&1 &
import socket, sys
s = socket.socket()
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
s.bind(("127.0.0.1", int(sys.argv[1])))
s.listen(1)
s.settimeout(5)
try:
    s.accept()
except OSError:
    pass
PY
    pid=$!
    sleep 1
    if docker exec iboot-prometheus busybox nc -z -w 2 host.docker.internal "$probe_port" >/dev/null 2>&1; then
        kill "$pid" 2>/dev/null || true
        wait "$pid" 2>/dev/null || true
        return 0
    fi
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    return 1
}

start_host_loopback_relay() {
    observability_enabled || {
        log '  可观测平台未启用，跳过宿主回环 relay'
        return 0
    }
    if [ "${IBOOT_HOST_LOOPBACK_RELAY:-auto}" = "false" ]; then
        log '  宿主回环 relay 已禁用（IBOOT_HOST_LOOPBACK_RELAY=false）'
        return 0
    fi
    local running
    running="$(relay_pid)" || true
    if [ -n "$running" ]; then
        log "  宿主回环 relay 已在运行（pid $running）"
        return 0
    fi
    command -v python3 >/dev/null 2>&1 || fail '缺少 python3，无法启动宿主回环 relay'
    if ! docker inspect iboot-prometheus >/dev/null 2>&1; then
        log '  Prometheus 未运行，跳过宿主回环 relay'
        return 0
    fi
    if host_loopback_reachable_from_docker; then
        log '  容器已可经 host.docker.internal 访问宿主机回环，跳过 relay'
        return 0
    fi
    local binds
    binds="$(docker_gateways || true)"
    if [ -z "$binds" ]; then
        log '  未发现 Docker 网桥网关，跳过宿主回环 relay'
        return 0
    fi
    log "  启动宿主回环 relay：$binds -> 127.0.0.1:8080-8089"
    nohup python3 "$ROOT/scripts/host-loopback-relay.py" \
        --bind "$binds" --ports 8080-8089 \
        --pid-file "$LOG_DIR/host-loopback-relay.pid" \
        --log-file "$LOG_DIR/host-loopback-relay.log" >/dev/null 2>&1 &
    local i
    for i in $(seq 1 10); do
        running="$(relay_pid)" || true
        [ -n "$running" ] && break
        sleep 1
    done
    [ -n "$running" ] || fail "宿主回环 relay 未启动，详见 $LOG_DIR/host-loopback-relay.log"
    log "  宿主回环 relay 就绪（pid $running）"
}

stop_all_services() {
    local entry
    stop_host_loopback_relay
    for entry in "${SERVICES[@]}"; do
        stop_service "${entry%%:*}"
    done
    stop_frontend
}

# 准备本地证书与公开来源变量。
#
# 默认启用 TLS：生成/复用证书，导出认证服务与 Vite 所需的环境变量。显式设置
# IBOOT_TLS_ENABLED=false 时进入明文排障模式，只改变公开入口的协议，不改内部服务端口。
prepare_tls() {
    if ! internal_tls_enabled; then
        fail 'B2 不提供运行期内部明文回退（IBOOT_INTERNAL_TLS_ENABLED=false）；请回退到上一版 commit/镜像'
    fi
    local cert_dir="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
    export IBOOT_TLS_CERT_DIR="$cert_dir"
    if tls_enabled; then
        "$ROOT/scripts/tls-init.sh" || fail 'TLS 证书生成失败'
        export NODE_EXTRA_CA_CERTS="$cert_dir/ca/rootCA.pem"
        export IBOOT_AUTH_COOKIE_SECURE="${IBOOT_AUTH_COOKIE_SECURE:-true}"
        export IBOOT_PUBLIC_ORIGIN="${IBOOT_PUBLIC_ORIGIN:-https://localhost:5173}"
        case "$IBOOT_PUBLIC_ORIGIN" in
            https://*) ;;
            *) fail "IBOOT_PUBLIC_ORIGIN 必须是 https 地址（当前：$IBOOT_PUBLIC_ORIGIN）" ;;
        esac
    else
        export IBOOT_PUBLIC_ORIGIN="${IBOOT_PUBLIC_ORIGIN_HTTP:-http://localhost:5173}"
        export IBOOT_AUTH_COOKIE_SECURE=false
        unset NODE_EXTRA_CA_CERTS
        log '[WARN] IBOOT_TLS_ENABLED=false：当前为明文模式，仅用于排障；回归与验收不得判定通过。'
    fi
    export IBOOT_AUTH_ISSUER="${IBOOT_AUTH_ISSUER:-$IBOOT_PUBLIC_ORIGIN}"
    export IBOOT_AUTH_REDIRECT_URI="${IBOOT_AUTH_REDIRECT_URI:-$IBOOT_PUBLIC_ORIGIN/callback}"
    export IBOOT_AUTH_LOGOUT_REDIRECT_URI="${IBOOT_AUTH_LOGOUT_REDIRECT_URI:-$IBOOT_PUBLIC_ORIGIN/login}"
    if tls_enabled; then
        case "$IBOOT_AUTH_ISSUER" in
            https://*) ;;
            *) fail "IBOOT_AUTH_ISSUER 必须是 https 地址（当前：$IBOOT_AUTH_ISSUER）" ;;
        esac
    fi
    log "  公开来源：$IBOOT_PUBLIC_ORIGIN"
}

wait_http() {
    local desc="$1" url="$2" timeout="${3:-180}" i
    local -a curl_opts=(-sf -m 3)
    if [ "${url#https://}" != "$url" ]; then
        local cert_dir="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
        curl_opts+=(--cacert "$cert_dir/ca/rootCA.pem")
        # 服务端口要求客户端证书；门户是这些服务的合法调用方。
        if [ -f "$cert_dir/portal.pem" ] && [ -f "$cert_dir/portal-key.pem" ]; then
            curl_opts+=(--cert "$cert_dir/portal.pem" --key "$cert_dir/portal-key.pem")
        fi
    fi
    for i in $(seq 1 "$timeout"); do
        if curl "${curl_opts[@]}" "$url" > /dev/null 2>&1; then
            log "  $desc 就绪"
            return 0
        fi
        sleep 1
    done
    printf '[ERROR] %s 在 %s 秒内未就绪：%s\n' "$desc" "$timeout" "$url" >&2
    return 1
}

# 等待 ES 通过 HTTPS + 凭据就绪；不回退到明文。
wait_es() {
    local timeout="${1:-180}" i
    for i in $(seq 1 "$timeout"); do
        if curl -sf -m 3 --cacert "${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}/ca/rootCA.pem" \
            -u "$ELASTICSEARCH_USERNAME:$ELASTICSEARCH_PASSWORD" \
            "$ELASTICSEARCH_URIS/_cluster/health" > /dev/null 2>&1; then
            log '  Elasticsearch 就绪'
            return 0
        fi
        sleep 1
    done
    printf '[ERROR] Elasticsearch 在 %s 秒内未就绪：%s\n' "$timeout" "$ELASTICSEARCH_URIS" >&2
    return 1
}

wait_healthy() {
    local desc="$1" name="$2" timeout="${3:-120}" i status
    for i in $(seq 1 "$timeout"); do
        status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
            "$name" 2>/dev/null || true)"
        if [ "$status" = "healthy" ]; then
            log "  $desc 就绪"
            return 0
        fi
        sleep 1
    done
    printf '[ERROR] %s 在 %s 秒内未就绪：%s\n' "$desc" "$timeout" "$name" >&2
    return 1
}

start_middleware() {
    log '=== 1. 中间件 ==='
    # Prometheus 在宿主机开发模式抓取宿主进程；Docker 全量部署的默认配置按服务名抓取。
    export IBOOT_PROMETHEUS_CONFIG="${IBOOT_PROMETHEUS_CONFIG:-prometheus.yml}"
    local infra=(mysql redis silo elasticsearch elasticsearch-init seata-server)
    if rocketmq_enabled; then
        infra+=(rocketmq-namesrv rocketmq-store-init rocketmq-broker rocketmq-proxy rocketmq-tls rocketmq-init)
    fi
    if logging_enabled; then
        infra+=(kafka kafka-init logstash)
    fi
    "${COMPOSE[@]}" up -d "${infra[@]}" > /dev/null || fail '中间件启动失败'
    "${COMPOSE[@]}" --profile platform up -d nacos tempo sentinel-dashboard > /dev/null \
        || fail 'nacos/tempo/sentinel-dashboard 启动失败'
    if observability_enabled; then
        # 显式列出 observability profile 中的服务，避免 compose 一并拉起应用容器与宿主机端口冲突。
        local observability=(prometheus grafana alertmanager thanos-sidecar thanos-store \
            thanos-query thanos-compact mysql-exporter redis-exporter node-exporter \
            elasticsearch-exporter kibana blackbox-exporter)
        "${COMPOSE[@]}" --profile observability up -d "${observability[@]}" > /dev/null \
            || fail '可观测平台启动失败'
    fi

    local i
    for i in $(seq 1 90); do
        docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" iboot-mysql mysqladmin ping -h localhost -uroot --silent > /dev/null 2>&1 && break
        sleep 2
    done
    docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" iboot-mysql mysqladmin ping -h localhost -uroot --silent > /dev/null 2>&1 \
        || fail 'MySQL 未在预期时间内就绪'

    # 建库与建表统一由 iboot-db-init 完成，服务不再自带迁移。

    wait_es 180 || fail 'Elasticsearch 未就绪'
    # 业务服务与日志链路只拿专用账号；超级用户只留在初始化任务里。
    "${COMPOSE[@]}" run --rm --no-deps es-users-init >/dev/null \
        || fail 'ES 账号初始化失败（es-users-init）'
    wait_http 'Silo' "$(service_scheme)://localhost:9000/minio/health/live" 120 || fail 'Silo 未就绪'
    # 文件、备份与 AI 使用各自的桶级账号，必须先完成账号与桶初始化。
    "${COMPOSE[@]}" run --rm --no-deps silo-init >/dev/null \
        || fail 'Silo 账号初始化失败（silo-init）'
    # 注册中心没就绪就启动后端，会以 ErrCode:-401 Client not connected 直接启动失败。
    # Nacos 3.x 的 v1 健康接口已下线，改用控制台的 readiness 端点（宿主机 8849）。
    wait_http 'Nacos' 'https://localhost:8849/v3/console/health/readiness' 180 || fail 'Nacos 未就绪'
    # 先初始化管理员与服务账号，再注册服务与写入 Sentinel 基线规则。
    "${COMPOSE[@]}" run --rm --no-deps nacos-init >/dev/null 2>&1 \
        || fail 'Nacos 账号初始化失败（nacos-init）'
    wait_http 'Sentinel Dashboard' 'https://localhost:8858/' 120 || fail 'Sentinel Dashboard 未就绪'
    # 审批通过依赖全局事务协调器；必须在 workflow/system 启动前就绪
    wait_healthy 'Seata Server' iboot-seata-server 60 || fail 'Seata Server 未就绪'
    "$ROOT/scripts/sentinel-init.sh" || fail 'Sentinel 基线规则初始化失败'
    if rocketmq_enabled; then
        wait_healthy 'RocketMQ NameServer' iboot-rocketmq-namesrv 90 || fail 'RocketMQ NameServer 未就绪'
        wait_healthy 'RocketMQ Broker' iboot-rocketmq-broker 120 || fail 'RocketMQ Broker 未就绪'
        wait_healthy 'RocketMQ Proxy' iboot-rocketmq-proxy 60 || fail 'RocketMQ Proxy 未就绪'
        wait_healthy 'RocketMQ TLS' iboot-rocketmq-tls 60 || fail 'RocketMQ TLS 入口未就绪'
        "${COMPOSE[@]}" run --rm --no-deps rocketmq-acl-init >/dev/null \
            || fail 'RocketMQ ACL 初始化失败（rocketmq-acl-init）'
    else
        log '  RocketMQ 已关闭（IBOOT_ROCKETMQ_ENABLED=false）'
    fi
    if logging_enabled; then
        wait_healthy 'Kafka' iboot-kafka 120 || fail 'Kafka 未就绪'
        # 日志链路是辅助能力，未就绪时只提示，不阻塞业务服务启动。
        wait_healthy 'Logstash' iboot-logstash 240 || log '  Logstash 未在预期时间内就绪（运行日志可能暂时为空）'
    fi
    if observability_enabled; then
        # Kibana 首次启动较慢；未就绪只提示，不影响后端。
        wait_http 'Kibana' 'https://localhost:5601/api/status' 180 || log '  Kibana 未在预期时间内就绪（可稍后重试页面）'
    fi
    log '  中间件就绪'
}

build_backend() {
    local entry name jar
    if [ "$FORCE_BUILD" -eq 0 ]; then
        for entry in "${SERVICES[@]}"; do
            name="${entry%%:*}"
            if [ "$name" = "iboot-ops" ] && ! ops_enabled; then
                continue
            fi
            jar="$name/target/$name-$VERSION.jar"
            if [ ! -f "$jar" ]; then
                log "  缺少 $jar，需要构建"
                FORCE_BUILD=1
                break
            fi
        done
        if [ "$FORCE_BUILD" -eq 0 ] && [ ! -f "iboot-db-init/target/iboot-db-init-$VERSION-exec.jar" ]; then
            log "  缺少 db-init 可执行 jar，需要构建"
            FORCE_BUILD=1
        fi
    fi
    if [ "$FORCE_BUILD" -eq 1 ]; then
        log '=== 2. 构建后端 ==='
        MVN_ARGS=()
        if [ -n "$MAVEN_SETTINGS" ]; then
            MVN_ARGS=(-s "$MAVEN_SETTINGS")
        fi
        mvn "${MVN_ARGS[@]}" -q -DskipTests package \
            || fail '构建失败，先看上面的 Maven 输出'
        log '  构建完成'
    else
        log '=== 2. 构建后端 === 已存在 jar，跳过（--build 可强制重建）'
    fi
}

run_db_init() {
    log '=== 3. 初始化数据库 ==='
    log '  初始化 MySQL 业务账号...'
    "${COMPOSE[@]}" run --rm --no-deps mysql-users-init >/dev/null \
        || fail 'MySQL 账号初始化失败（mysql-users-init）'
    local -a java_opts=()
    if internal_tls_enabled; then
        local cert_dir="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
        java_opts+=(
            "-Djavax.net.ssl.trustStore=$cert_dir/truststore.p12"
            "-Djavax.net.ssl.trustStorePassword=changeit"
            "-Djavax.net.ssl.trustStoreType=PKCS12"
        )
    else
        export MYSQL_TLS_ENABLED=false
    fi
    # 迁移需要 DDL；业务服务使用各自的 iboot_<service> 账号。
    MYSQL_USERNAME=root MYSQL_PASSWORD="${MYSQL_ROOT_PASSWORD}" \
        java "${java_opts[@]}" -jar "iboot-db-init/target/iboot-db-init-$VERSION-exec.jar" \
        || fail '数据库初始化失败（iboot-db-init）'
    log '  数据库就绪'
}

start_backend() {
    local entry name port
    log '=== 4. 启动后端 ==='
    # 邮件是可选的外部依赖：.env 里配了邮箱时它会参与健康检查，离线或认证失败就会把
    # /actuator/health 拉成 503。本地开发把它排除，健康状态只反映数据库、注册中心等
    # 运行本服务真正需要的东西。
    export MANAGEMENT_HEALTH_MAIL_ENABLED=false
    # 宿主机进程通过映射端口连容器内的 Kafka；关闭时应用不会创建生产者。
    if logging_enabled; then
        export IBOOT_LOGGING_KAFKA_ENABLED=true
        export IBOOT_LOGGING_KAFKA_BOOTSTRAP_SERVERS="${IBOOT_LOGGING_KAFKA_BOOTSTRAP_SERVERS:-localhost:29092}"
    fi
    if skywalking_enabled; then
        [ -f "$SKYWALKING_AGENT" ] \
            || fail "SkyWalking Agent 不存在：$SKYWALKING_AGENT（先执行 scripts/apm/fetch-agent.sh）"
        log "  SkyWalking Agent 已启用：$SKYWALKING_AGENT -> $SKYWALKING_OAP"
    fi
    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        port="${entry##*:}"
        if [ "$name" = "iboot-ops" ] && ! ops_enabled; then
            log "  $name 已关闭（IBOOT_OPS_ENABLED=false）"
            continue
        fi
        local pidfile="$LOG_DIR/$name.pid" running
        running="$(service_pid "$name")"
        if [ -n "$running" ]; then
            echo "$running" > "$pidfile"
            log "  $name 已在运行（pid $running）"
        else
            local -a java_opts=()
            if internal_tls_enabled; then
                local cert_dir="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
                java_opts+=(
                    "-Djavax.net.ssl.trustStore=$cert_dir/truststore.p12"
                    "-Djavax.net.ssl.trustStorePassword=changeit"
                    "-Djavax.net.ssl.trustStoreType=PKCS12"
                    "-Dnacos.remote.client.rpc.tls.enable=true"
                    "-Dnacos.remote.client.rpc.tls.trustCollectionChainPath=file:$cert_dir/ca/rootCA.pem"
                    "-Dnacos.remote.client.rpc.tls.trustAll=false"
                    "-Dnacos.remote.client.rpc.tls.mutualAuth=true"
                    "-Dnacos.remote.client.rpc.tls.certChainFile=file:$cert_dir/portal.pem"
                    "-Dnacos.remote.client.rpc.tls.certPrivateKey=file:$cert_dir/portal-key.pem"
                )
            else
                java_opts+=("--server.ssl.enabled=false")
            fi
            if skywalking_enabled && [ "$name" != "iboot-ops" ]; then
                java_opts+=(
                    "-javaagent:$SKYWALKING_AGENT"
                    "-Dskywalking.agent.service_name=$name"
                    "-Dskywalking.collector.backend_service=$SKYWALKING_OAP"
                    "-Dskywalking.agent.sample_n_per_3_secs=-1"
                )
            fi
            local -a env_opts=(
                -u ELASTIC_PASSWORD -u ES_AI_PASSWORD -u ES_NOTIFY_PASSWORD
                -u ES_MONITOR_PASSWORD -u ES_LOGSTASH_PASSWORD
                -u ES_GRAFANA_PASSWORD -u ES_EXPORTER_PASSWORD
                -u KAFKA_BROKER_PASSWORD -u KAFKA_LOG_PASSWORD
                -u KAFKA_LOGSTASH_PASSWORD -u KAFKA_TOOLS_PASSWORD
                -u REDIS_EXPORTER_PASSWORD
                # 清空共享身份与 Silo 根凭据，避免子进程继承 .env 中的其它身份。
                -u MYSQL_USERNAME -u MYSQL_PASSWORD -u REDIS_USERNAME -u REDIS_PASSWORD
                -u MYSQL_ROOT_PASSWORD -u MYSQL_EXPORTER_PASSWORD
                -u SILO_ROOT_USER -u SILO_ROOT_PASSWORD -u NACOS_ADMIN_PASSWORD
                -u NACOS_USERNAME -u NACOS_PASSWORD
                -u SILO_FILE_ACCESS_KEY -u SILO_FILE_SECRET_KEY
                -u SILO_JOB_ACCESS_KEY -u SILO_JOB_SECRET_KEY
                -u SILO_AI_ACCESS_KEY -u SILO_AI_SECRET_KEY
                -u IBOOT_FILE_SILO_ACCESS_KEY -u IBOOT_FILE_SILO_SECRET_KEY
                -u IBOOT_AI_SILO_ACCESS_KEY -u IBOOT_AI_SILO_SECRET_KEY
                -u IBOOT_ROCKETMQ_ACCESS_KEY -u IBOOT_ROCKETMQ_SECRET_KEY
                -u ROCKETMQ_ADMIN_ACCESS_KEY -u ROCKETMQ_ADMIN_SECRET_KEY
                -u ROCKETMQ_WORKFLOW_ACCESS_KEY -u ROCKETMQ_WORKFLOW_SECRET_KEY
                -u ROCKETMQ_NOTIFY_ACCESS_KEY -u ROCKETMQ_NOTIFY_SECRET_KEY
            )
            local short="${name#iboot-}" acct="iboot_${name#iboot-}" upper svc_pw_var
            upper="$(printf '%s' "$short" | tr '[:lower:]' '[:upper:]')"
            svc_pw_var="MIDDLEWARE_${upper}_PASSWORD"
            # 所有中间件口令变量都从子进程环境中移除；服务只通过 MYSQL_*/REDIS_*/NACOS_* 收到自己的值。
            local -a others=(system workflow file notify job monitor auth ai gateway ops)
            local other other_upper
            for other in "${others[@]}"; do
                other_upper="$(printf '%s' "$other" | tr '[:lower:]' '[:upper:]')"
                env_opts+=(-u "MIDDLEWARE_${other_upper}_PASSWORD")
            done
            env_opts+=("NACOS_USERNAME=$acct" "NACOS_PASSWORD=${!svc_pw_var}")
            case "$short" in
                system|workflow|file|notify|job|monitor|auth|ai)
                    env_opts+=(
                        "MYSQL_USERNAME=$acct" "MYSQL_PASSWORD=${!svc_pw_var}"
                        "REDIS_USERNAME=$acct" "REDIS_PASSWORD=${!svc_pw_var}"
                    )
                    ;;
            esac
            if internal_tls_enabled; then
                local cert_dir="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
                env_opts+=(
                    "IBOOT_TLS_CERT_FILE=$cert_dir/services/$name.pem"
                    "IBOOT_TLS_KEY_FILE=$cert_dir/services/$name-key.pem"
                )
            fi
            case "$name" in
                iboot-ai)
                    env_opts+=(ELASTICSEARCH_USERNAME=iboot_ai "ELASTICSEARCH_PASSWORD=$ES_AI_PASSWORD")
                    env_opts+=(
                        "IBOOT_AI_SILO_ACCESS_KEY=${SILO_AI_ACCESS_KEY:-iboot_ai}"
                        "IBOOT_AI_SILO_SECRET_KEY=$SILO_AI_SECRET_KEY"
                    )
                    ;;
                iboot-workflow)
                    env_opts+=(
                        "IBOOT_ROCKETMQ_ACCESS_KEY=$ROCKETMQ_WORKFLOW_ACCESS_KEY"
                        "IBOOT_ROCKETMQ_SECRET_KEY=$ROCKETMQ_WORKFLOW_SECRET_KEY"
                    )
                    ;;
                iboot-notify)
                    env_opts+=(
                        "IBOOT_ROCKETMQ_ACCESS_KEY=$ROCKETMQ_NOTIFY_ACCESS_KEY"
                        "IBOOT_ROCKETMQ_SECRET_KEY=$ROCKETMQ_NOTIFY_SECRET_KEY"
                        ELASTICSEARCH_USERNAME=iboot_notify
                        "ELASTICSEARCH_PASSWORD=$ES_NOTIFY_PASSWORD"
                    )
                    ;;
                iboot-file)
                    env_opts+=(
                        "IBOOT_FILE_SILO_ACCESS_KEY=${SILO_FILE_ACCESS_KEY:-iboot_file}"
                        "IBOOT_FILE_SILO_SECRET_KEY=$SILO_FILE_SECRET_KEY"
                    )
                    ;;
                iboot-job)
                    env_opts+=(
                        "IBOOT_FILE_SILO_ACCESS_KEY=${SILO_JOB_ACCESS_KEY:-iboot_job}"
                        "IBOOT_FILE_SILO_SECRET_KEY=$SILO_JOB_SECRET_KEY"
                    )
                    ;;
                iboot-system)
                    # 系统服务不再承载上传；遗留存储策略固定为本地。
                    env_opts+=(IBOOT_FILE_STORAGE_TYPE=local)
                    ;;
                iboot-monitor)
                    env_opts+=(ELASTICSEARCH_USERNAME=iboot_monitor "ELASTICSEARCH_PASSWORD=$ES_MONITOR_PASSWORD")
                    ;;
            esac
            nohup env "${env_opts[@]}" java "${java_opts[@]}" -jar "$name/target/$name-$VERSION.jar" \
                --server.address=127.0.0.1 > "$LOG_DIR/$name.log" 2>&1 &
            echo $! > "$pidfile"
            log "  $name 启动中（pid $!，端口 $port）"
        fi
        # 表结构已由 db-init 建好；仍让系统服务先就绪，便于其余服务读取共享库中的配置。
        if [ "$name" = "iboot-system" ]; then
            wait_http "$name" "$(service_scheme)://localhost:$port/actuator/health" 240 \
                || fail "$name 未就绪：其余服务启动时要读取系统域的数据，无法继续"
        fi
    done
}

wait_backend() {
    local entry name port
    log '=== 5. 等待健康 ==='
    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        port="${entry##*:}"
        if [ "$name" = "iboot-ops" ] && ! ops_enabled; then
            continue
        fi
        if ! wait_http "$name" "$(service_scheme)://localhost:$port/actuator/health" 240; then
            printf '  最近日志（logs/%s.log）：\n' "$name" >&2
            tail -20 "$LOG_DIR/$name.log" >&2 2>/dev/null
            return 1
        fi
    done
}

start_frontend() {
    log '=== 6. 前端 ==='
    if [ ! -d iboot-portal/node_modules ]; then
        ( cd iboot-portal && cnpm install ) || fail '前端依赖安装失败'
    fi
    if [ -f "$LOG_DIR/portal.pid" ] && kill -0 "$(cat "$LOG_DIR/portal.pid")" 2>/dev/null; then
        log "  前端已在运行（pid $(cat "$LOG_DIR/portal.pid")）"
        return
    fi
    ( cd iboot-portal && nohup cnpm run dev > "$LOG_DIR/portal.log" 2>&1 & echo $! > "$LOG_DIR/portal.pid" )
    local origin
    origin="$(public_origin)"
    log "  前端启动中（日志 logs/portal.log，$origin）"
    wait_http 'iboot-portal' "$origin/" 60 || fail "前端未在预期时间内就绪：$origin"
}

show_status() {
    local entry name port pid state health scheme
    scheme="$(service_scheme)"
    local -a health_opts=(-sf -m 2)
    if [ "$scheme" = "https" ]; then
        local cert_dir="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
        health_opts+=(--cacert "$cert_dir/ca/rootCA.pem")
        if [ -f "$cert_dir/portal.pem" ] && [ -f "$cert_dir/portal-key.pem" ]; then
            health_opts+=(--cert "$cert_dir/portal.pem" --key "$cert_dir/portal-key.pem")
        fi
    fi
    log '=== 中间件 ==='
    "${COMPOSE[@]}" --profile platform --profile observability ps --format '  {{.Name}}\t{{.Status}}' 2>/dev/null || true
    log '=== 后端 ==='
    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        port="${entry##*:}"
        if [ "$name" = "iboot-ops" ] && ! ops_enabled; then
            printf '  %-16s %-24s %s  %s://localhost:%s\n' "$name" "已关闭（IBOOT_OPS_ENABLED=false）" "-" "$scheme" "$port"
            continue
        fi
        pid="$(service_pid "$name")"
        state='未运行'
        [ -n "$pid" ] && state="运行中（pid $pid）"
        health="$(curl "${health_opts[@]}" "$scheme://localhost:$port/actuator/health" > /dev/null 2>&1 && echo 健康 || echo 不可用)"
        printf '  %-16s %-24s %s  %s://localhost:%s\n' "$name" "$state" "$health" "$scheme" "$port"
    done
    local portal origin
    portal="$(frontend_pids)"
    origin="$(public_origin)"
    if [ -n "$portal" ]; then
        printf '  %-16s %-24s %s  %s\n' "iboot-portal" "运行中（pid $portal）" "开发服务器" "$origin"
    else
        printf '  %-16s %-24s %s  %s\n' "iboot-portal" "未运行" "-" "$origin"
    fi
    local relay
    relay="$(relay_pid)" || true
    if [ -n "$relay" ]; then
        printf '  %-16s %-24s %s  %s\n' "host-relay" "运行中（pid $relay）" "回环转发" "8080-8089"
    else
        printf '  %-16s %-24s %s  %s\n' "host-relay" "未运行" "-" "8080-8089"
    fi
}

case "$ACTION" in
    status)
        show_status
        ;;
    stop)
        log '=== 停止后端与前端 ==='
        stop_all_services
        log '后端与前端已停止；中间件仍在运行（--down 可一并移除）'
        ;;
    down)
        log '=== 停止并移除 ==='
        stop_all_services
        "${COMPOSE[@]}" --profile platform --profile observability down --remove-orphans > /dev/null 2>&1 || true
        log '后端、前端与中间件已停止，数据卷保留'
        ;;
    purge)
        log '=== 停止并清除数据 ==='
        stop_all_services
        "${COMPOSE[@]}" --profile platform --profile observability down -v --remove-orphans > /dev/null 2>&1 || true
        log '后端、前端、中间件与数据卷均已移除'
        ;;
    up)
        if [ -z "${DASHSCOPE_CHAT_API_KEY:-}" ]; then
            fail '缺少 DASHSCOPE_CHAT_API_KEY：iboot-ai 启动时校验该值，缺失会直接退出。
       请写入根目录 .env（字段参考 .env.example）后重试。'
        fi
        prepare_tls
        start_middleware
        build_backend
        run_db_init
        start_backend
        start_host_loopback_relay
        wait_backend || fail '服务未在预期时间内就绪'
        [ "$WITH_FRONTEND" -eq 1 ] && start_frontend
        log ''
        show_status
        log ''
        if ops_enabled; then
            log "开发环境已就绪：门户 $IBOOT_PUBLIC_ORIGIN，网关 $(service_scheme)://localhost:8088，系统服务 $(service_scheme)://localhost:8080，AI $(service_scheme)://localhost:8081，认证 $(service_scheme)://localhost:8083，运维控制台 $(service_scheme)://localhost:8089/sba，Sentinel Dashboard https://localhost:8858"
        else
            log "开发环境已就绪：门户 $IBOOT_PUBLIC_ORIGIN，网关 $(service_scheme)://localhost:8088，系统服务 $(service_scheme)://localhost:8080，AI $(service_scheme)://localhost:8081，认证 $(service_scheme)://localhost:8083，Sentinel Dashboard https://localhost:8858（运维控制台已关闭）"
        fi
        log '登录账号 admin / admin123456'
        log '服务治理控制台：Nacos https://localhost:8849/next/，Sentinel https://localhost:8858；门户“微服务治理”菜单改为新窗口打开'
        if observability_enabled; then
            log '可观测平台：Grafana https://localhost:3000，Prometheus https://localhost:9090，Kibana https://localhost:5601，Alertmanager https://localhost:9093'
        fi
        ;;
esac
