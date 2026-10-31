#!/usr/bin/env bash
# 一键拉起本地开发环境：中间件 → 建库 → 构建（按需）→ 启动三个服务 → 等待健康。
#
# 用法：
#   dev-init.sh              拉起中间件与后端三个服务
#   dev-init.sh --build      强制重新构建后端
#   dev-init.sh --frontend   同时安装前端依赖并启动开发服务器
#   dev-init.sh --status     查看中间件与后端服务的状态
#   dev-init.sh --stop       停止后端三个服务（中间件保留）
#   dev-init.sh --down       停止服务并移除中间件容器（数据卷保留）
#
# 密钥与私有配置沿用根目录的 .env（已在 .gitignore 中，模板见 .env.example），
# 与 README 里 `set -a; source .env; set +a` 的既有约定一致；脚本会自动加载。
#
# 说明：
# - 中间件只启动开发必需的一组（mysql/redis/minio/es + nacos/tempo），
#   不启动 Prometheus、Kibana、Thanos 等观测组件；
# - 全新数据卷下 MySQL 会执行 docker/init.sql 建出 iboot_admin，iboot_ai 由本脚本创建；
# - 后端日志写入 logs/，进程号写入 logs/<name>.pid。

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

COMPOSE=(docker compose -f docker/docker-compose.yml)
LOG_DIR="$ROOT/logs"
MAVEN_SETTINGS="${MAVEN_SETTINGS:-/home/lam/repo/settings.xml}"
VERSION="1.0.0"

# 后端服务：名称:端口
SERVICES=("iboot-admin:8080" "iboot-ai:8081" "iboot-gateway:8088")

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
        -h|--help)  sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
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
    pid="$(pgrep -f "java -jar $name/target/$name-$VERSION.jar" 2>/dev/null | head -1)"
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

stop_all_services() {
    local entry
    for entry in "${SERVICES[@]}"; do
        stop_service "${entry%%:*}"
    done
}

wait_http() {
    local desc="$1" url="$2" timeout="${3:-180}" i
    for i in $(seq 1 "$timeout"); do
        if curl -sf -m 3 "$url" > /dev/null 2>&1; then
            log "  $desc 就绪"
            return 0
        fi
        sleep 1
    done
    printf '[ERROR] %s 在 %s 秒内未就绪：%s\n' "$desc" "$timeout" "$url" >&2
    return 1
}

start_middleware() {
    log '=== 1. 中间件 ==='
    "${COMPOSE[@]}" up -d mysql redis minio minio-init elasticsearch > /dev/null || fail '中间件启动失败'
    "${COMPOSE[@]}" --profile platform up -d nacos tempo > /dev/null || fail 'nacos/tempo 启动失败'

    local i
    for i in $(seq 1 90); do
        docker exec iboot-mysql mysqladmin ping -h localhost -uroot -proot --silent > /dev/null 2>&1 && break
        sleep 2
    done
    docker exec iboot-mysql mysqladmin ping -h localhost -uroot -proot --silent > /dev/null 2>&1 \
        || fail 'MySQL 未在预期时间内就绪'

    docker exec iboot-mysql mysql -uroot -proot \
        -e 'CREATE DATABASE IF NOT EXISTS iboot_ai CHARACTER SET utf8mb4' 2>/dev/null \
        || fail '创建 iboot_ai 库失败'

    wait_http 'Elasticsearch' 'http://localhost:9200' 180 || fail 'Elasticsearch 未就绪'
    wait_http 'MinIO' 'http://localhost:9000/minio/health/live' 120 || fail 'MinIO 未就绪'
    # 注册中心没就绪就启动后端，会以 ErrCode:-401 Client not connected 直接启动失败。
    # Nacos 3.x 的 v1 健康接口已下线，改用控制台的 readiness 端点（宿主机 8849）。
    wait_http 'Nacos' 'http://localhost:8849/v3/console/health/readiness' 180 || fail 'Nacos 未就绪'
    log '  中间件就绪'
}

build_backend() {
    local entry name jar
    if [ "$FORCE_BUILD" -eq 0 ]; then
        for entry in "${SERVICES[@]}"; do
            name="${entry%%:*}"
            jar="$name/target/$name-$VERSION.jar"
            if [ ! -f "$jar" ]; then
                log "  缺少 $jar，需要构建"
                FORCE_BUILD=1
                break
            fi
        done
    fi
    if [ "$FORCE_BUILD" -eq 1 ]; then
        log '=== 2. 构建后端 ==='
        mvn -s "$MAVEN_SETTINGS" -q -DskipTests package \
            || fail '构建失败，先看上面的 Maven 输出'
        log '  构建完成'
    else
        log '=== 2. 构建后端 === 已存在 jar，跳过（--build 可强制重建）'
    fi
}

start_backend() {
    local entry name port
    log '=== 3. 启动后端 ==='
    # 邮件是可选的外部依赖：.env 里配了邮箱时它会参与健康检查，离线或认证失败就会把
    # /actuator/health 拉成 503。本地开发把它排除，健康状态只反映数据库、注册中心等
    # 运行本服务真正需要的东西。
    export MANAGEMENT_HEALTH_MAIL_ENABLED=false
    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        port="${entry##*:}"
        local pidfile="$LOG_DIR/$name.pid" running
        running="$(service_pid "$name")"
        if [ -n "$running" ]; then
            echo "$running" > "$pidfile"
            log "  $name 已在运行（pid $running）"
            continue
        fi
        nohup java -jar "$name/target/$name-$VERSION.jar" > "$LOG_DIR/$name.log" 2>&1 &
        echo $! > "$pidfile"
        log "  $name 启动中（pid $!，端口 $port）"
    done
}

wait_backend() {
    local entry name port
    log '=== 4. 等待健康 ==='
    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        port="${entry##*:}"
        if ! wait_http "$name" "http://localhost:$port/actuator/health" 240; then
            printf '  最近日志（logs/%s.log）：\n' "$name" >&2
            tail -20 "$LOG_DIR/$name.log" 2>/dev/null >&2
            return 1
        fi
    done
}

start_frontend() {
    log '=== 5. 前端 ==='
    if [ ! -d iboot-portal/node_modules ]; then
        ( cd iboot-portal && cnpm install ) || fail '前端依赖安装失败'
    fi
    if [ -f "$LOG_DIR/portal.pid" ] && kill -0 "$(cat "$LOG_DIR/portal.pid")" 2>/dev/null; then
        log "  前端已在运行（pid $(cat "$LOG_DIR/portal.pid")）"
        return
    fi
    ( cd iboot-portal && nohup cnpm run dev > "$LOG_DIR/portal.log" 2>&1 & echo $! > "$LOG_DIR/portal.pid" )
    log "  前端启动中（日志 logs/portal.log，默认 http://localhost:5173）"
}

show_status() {
    local entry name port pid state health
    log '=== 中间件 ==='
    "${COMPOSE[@]}" --profile platform ps --format '  {{.Name}}\t{{.Status}}' 2>/dev/null || true
    log '=== 后端 ==='
    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        port="${entry##*:}"
        pid="$(service_pid "$name")"
        state='未运行'
        [ -n "$pid" ] && state="运行中（pid $pid）"
        health="$(curl -sf -m 2 "http://localhost:$port/actuator/health" > /dev/null 2>&1 && echo 健康 || echo 不可用)"
        printf '  %-16s %-24s %s  http://localhost:%s\n' "$name" "$state" "$health" "$port"
    done
}

case "$ACTION" in
    status)
        show_status
        ;;
    stop)
        log '=== 停止后端 ==='
        stop_all_services
        log '后端已停止；中间件仍在运行（--down 可一并移除）'
        ;;
    down)
        log '=== 停止并移除 ==='
        stop_all_services
        "${COMPOSE[@]}" --profile platform down --remove-orphans > /dev/null 2>&1 || true
        log '后端与中间件已停止，数据卷保留'
        ;;
    up)
        if [ -z "${DASHSCOPE_CHAT_API_KEY:-}" ]; then
            fail '缺少 DASHSCOPE_CHAT_API_KEY：iboot-ai 启动时校验该值，缺失会直接退出。
       请写入根目录 .env（字段参考 .env.example）后重试。'
        fi
        start_middleware
        build_backend
        start_backend
        wait_backend || fail '服务未在预期时间内就绪'
        [ "$WITH_FRONTEND" -eq 1 ] && start_frontend
        log ''
        show_status
        log ''
        log '开发环境已就绪：网关 http://localhost:8088，管理端 http://localhost:8080，AI http://localhost:8081'
        log '登录账号 admin / admin123456'
        ;;
esac
