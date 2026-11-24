#!/usr/bin/env bash
# Compose 全量模式的 SVID 刷新入口。
#
# 先重新运行 svid-* 一次性 sidecar，把新 SVID 写入命名卷；再逐个重建业务容器加载新证书。
# 依赖 compose 的 svid 覆盖层与 spire profile。
#
# 用法：
#   scripts/docker-svid-refresh.sh [--no-restart]
#
# 默认滚动重启十个业务服务；--no-restart 只刷新卷内 SVID。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

COMPOSE=(docker compose -f docker/docker-compose.yml -f docker/docker-compose.svid.yml \
    --profile spire --profile svid)
SERVICES=(iboot-system iboot-ai iboot-file iboot-auth iboot-notify iboot-job iboot-workflow iboot-monitor iboot-gateway iboot-ops)
RESTART=1
[ "${1:-}" = "--no-restart" ] && RESTART=0

log() { printf '[svid-refresh] %s\n' "$*"; }
fail() { log "$*" >&2; exit 1; }

log '重新注册条目并合并 SPIRE CA'
"$ROOT/scripts/spire-svid-sync.sh" --setup >/dev/null || fail 'SVID 初始化失败'

log '重新取回十个服务的 SVID'
mapfile -t sidecars < <(printf 'svid-%s\n' "${SERVICES[@]}")
"${COMPOSE[@]}" up -d --force-recreate --no-deps "${sidecars[@]}" >/dev/null \
    || fail 'SVID sidecar 重建失败'

wait_exited_ok() {
    local name="$1" i state
    for i in $(seq 1 60); do
        state="$(docker inspect -f '{{.State.Status}}:{{.State.ExitCode}}' "$name" 2>/dev/null || true)"
        case "$state" in
            exited:0) return 0 ;;
            exited:*) fail "sidecar $name 失败（$state）" ;;
        esac
        sleep 1
    done
    fail "sidecar $name 未在预期时间内完成"
}

for svc in "${SERVICES[@]}"; do
    wait_exited_ok "docker-svid-$svc-1"
done
log 'SVID 卷已刷新'

if [ "$RESTART" -eq 0 ]; then
    log '按参数跳过业务容器重启'
    exit 0
fi

log '滚动重启业务容器'
for svc in "${SERVICES[@]}"; do
    if [ "$svc" = "iboot-ops" ] && [ "${IBOOT_OPS_ENABLED:-true}" != "true" ]; then
        continue
    fi
    "${COMPOSE[@]}" up -d --no-deps --force-recreate "$svc" >/dev/null \
        || fail "重建 $svc 失败"
    for i in $(seq 1 120); do
        state="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "docker-$svc-1" 2>/dev/null || true)"
        case "$state" in
            healthy|running) break ;;
        esac
        sleep 2
    done
    log "$svc: ${state:-unknown}"
done
log 'SVID 刷新完成'
