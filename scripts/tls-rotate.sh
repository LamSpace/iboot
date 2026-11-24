#!/usr/bin/env bash
# 证书检查与轮换入口。
#
# 默认只检查；--rotate 显式重签全部叶子证书；--restart 控制是否重启运行时。
# 普通启动不会自动重签，避免证书指纹在无人值守时漂移。
#
# 用法：
#   scripts/tls-rotate.sh --check
#   scripts/tls-rotate.sh --rotate --restart none
#   scripts/tls-rotate.sh --rotate --restart docker
#   scripts/tls-rotate.sh --rotate --restart rolling
#   scripts/tls-rotate.sh --rotate --restart api
#   scripts/tls-rotate.sh --rotate --restart host
#   scripts/tls-rotate.sh --restart-only --restart rolling

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi
MODE="check"
RESTART="none"

usage() {
    awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

EXPECT_RESTART=0
for arg in "$@"; do
    if [ "$EXPECT_RESTART" -eq 1 ]; then
        case "$arg" in
            none|docker|api|rolling|host) RESTART="$arg" ;;
            *) printf '[tls-rotate] 未知重启范围：%s\n' "$arg" >&2; exit 2 ;;
        esac
        EXPECT_RESTART=0
        continue
    fi
    case "$arg" in
        --check) MODE="check" ;;
        --rotate) MODE="rotate" ;;
        --restart-only) MODE="restart-only" ;;
        --restart) EXPECT_RESTART=1 ;;
        --restart=none) RESTART="none" ;;
        --restart=docker) RESTART="docker" ;;
        --restart=api) RESTART="api" ;;
        --restart=rolling) RESTART="rolling" ;;
        --restart=host) RESTART="host" ;;
        -h|--help) usage; exit 0 ;;
        *) printf '[tls-rotate] 未知参数：%s\n' "$arg" >&2; exit 2 ;;
    esac
done
if [ "$EXPECT_RESTART" -eq 1 ]; then
    printf '[tls-rotate] --restart 缺少取值（none|docker|rolling|api|host）\n' >&2
    exit 2
fi

check_certs() {
    "$ROOT/scripts/tls-init.sh" --check
}

restart_api() {
    local socket="${DOCKER_SOCKET:-/var/run/docker.sock}"
    local filter="${TLS_ROTATE_RESTART_FILTER:-^iboot-((auth|system|file|workflow|ops|ai|notify|monitor|job|gateway|portal)|[a-z0-9-]+-exporter)$}"
    command -v curl >/dev/null 2>&1 || {
        printf '[tls-rotate] 缺少 curl，无法通过 Docker API 重启\n' >&2
        return 1
    }
    [ -S "$socket" ] || {
        printf '[tls-rotate] Docker socket 不可用：%s\n' "$socket" >&2
        return 1
    }
    mapfile -t names < <(
        curl -s --unix-socket "$socket" http://localhost/containers/json \
            | grep -o '"Names":\["[^"]*"' \
            | sed 's/.*\["//; s/"$//' \
            | sed 's#^/##' \
            | grep -E "$filter" || true
    )
    if [ "${#names[@]}" -eq 0 ]; then
        printf '[tls-rotate] 未发现需要重启的服务容器；跳过\n'
        return 0
    fi
    local name code failed=0
    for name in "${names[@]}"; do
        code="$(curl -s -o /dev/null -w '%{http_code}' -X POST \
            --unix-socket "$socket" "http://localhost/containers/$name/restart" || true)"
        if [ "$code" = "204" ]; then
            printf '[tls-rotate] 已重启 %s\n' "$name"
        else
            printf '[tls-rotate] 重启 %s 失败（HTTP %s）\n' "$name" "${code:-000}" >&2
            failed=1
        fi
    done
    return "$failed"
}

rolling_restart() {
    printf '[tls-rotate] 逐个重建当前运行的 Docker 服务\n'
    local -a compose=(docker compose -f "$ROOT/docker/docker-compose.yml")
    local -a services=()
    # 调度容器自身不参与重建，避免递归重启
    mapfile -t services < <("${compose[@]}" ps --services 2>/dev/null | grep -vx tls-rotate)
    if [ "${#services[@]}" -eq 0 ]; then
        printf '[tls-rotate] 未发现运行中的 compose 服务\n' >&2
        return 1
    fi
    local svc cid status i
    for svc in "${services[@]}"; do
        "${compose[@]}" up -d --no-deps --force-recreate "$svc" >/dev/null
        cid="$("${compose[@]}" ps -q "$svc" 2>/dev/null | head -1)"
        [ -n "$cid" ] || continue
        for i in $(seq 1 60); do
            status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || true)"
            case "$status" in
                healthy|running) break ;;
            esac
            sleep 2
        done
        printf '[tls-rotate] %s: %s\n' "$svc" "${status:-unknown}"
    done
}

restart_runtime() {
    case "$RESTART" in
        none)
            printf '[tls-rotate] 未重启运行时；需要时执行 --restart docker|rolling|api|host\n'
            ;;
        docker)
            printf '[tls-rotate] 重建当前运行的 Docker 服务\n'
            local -a compose=(docker compose -f "$ROOT/docker/docker-compose.yml")
            local -a services=()
            mapfile -t services < <("${compose[@]}" ps --services 2>/dev/null | grep -vx tls-rotate)
            if [ "${#services[@]}" -eq 0 ]; then
                printf '[tls-rotate] 未发现运行中的 compose 服务\n' >&2
                return 1
            fi
            "${compose[@]}" up -d --force-recreate "${services[@]}"
            ;;
        rolling)
            rolling_restart
            ;;
        api)
            printf '[tls-rotate] 通过 Docker API 重启使用证书的服务\n'
            restart_api
            ;;
        host)
            printf '[tls-rotate] 重启宿主机开发进程\n'
            "$ROOT/scripts/dev-init.sh" --stop
            "$ROOT/scripts/dev-init.sh"
            ;;
    esac
}

if [ "$MODE" = "restart-only" ]; then
    restart_runtime
    exit 0
fi

if [ "$MODE" = "rotate" ]; then
    printf '[tls-rotate] 显式重签叶子证书\n'
    "$ROOT/scripts/tls-init.sh" --force
else
    if ! check_certs; then
        printf '[tls-rotate] 证书检查未通过；确认后执行 scripts/tls-rotate.sh --rotate\n' >&2
        exit 1
    fi
    printf '[tls-rotate] 证书检查通过；未修改任何文件\n'
    exit 0
fi

check_certs
printf '[tls-rotate] 重签后检查通过\n'
restart_runtime
