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
#   scripts/tls-rotate.sh --rotate --restart host

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="check"
RESTART="none"

usage() {
    awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

for arg in "$@"; do
    case "$arg" in
        --check) MODE="check" ;;
        --rotate) MODE="rotate" ;;
        --restart=none) RESTART="none" ;;
        --restart=docker) RESTART="docker" ;;
        --restart=host) RESTART="host" ;;
        -h|--help) usage; exit 0 ;;
        *) printf '[tls-rotate] 未知参数：%s\n' "$arg" >&2; exit 2 ;;
    esac
done

check_certs() {
    "$ROOT/scripts/tls-init.sh" --check
}

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

case "$RESTART" in
    none)
        printf '[tls-rotate] 未重启运行时；需要时执行 --restart docker|host\n'
        ;;
    docker)
        printf '[tls-rotate] 重建当前运行的 Docker 服务\n'
        COMPOSE=(docker compose -f "$ROOT/docker/docker-compose.yml")
        mapfile -t services < <("${COMPOSE[@]}" ps --services 2>/dev/null)
        if [ "${#services[@]}" -eq 0 ]; then
            printf '[tls-rotate] 未发现运行中的 compose 服务\n' >&2
            exit 1
        fi
        "${COMPOSE[@]}" up -d --force-recreate "${services[@]}"
        ;;
    host)
        printf '[tls-rotate] 重启宿主机开发进程\n'
        "$ROOT/scripts/dev-init.sh" --stop
        "$ROOT/scripts/dev-init.sh"
        ;;
esac
