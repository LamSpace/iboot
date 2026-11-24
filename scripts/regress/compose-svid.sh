#!/usr/bin/env bash
# Compose 全量模式 SVID 回归：覆盖层静态检查、sidecar 镜像构建、单个 sidecar 取回 SVID。
#
# 不启动完整 Compose 业务栈；完整业务链路由宿主机全栈回归覆盖。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

COMPOSE=(docker compose -f "$ROOT/docker/docker-compose.yml" -f "$ROOT/docker/docker-compose.svid.yml" \
    --profile spire --profile svid)
SERVICES=(iboot-system iboot-ai iboot-file iboot-auth iboot-notify iboot-job iboot-workflow iboot-monitor iboot-gateway iboot-ops)

if ! docker compose version >/dev/null 2>&1; then
    reg_log "  [SKIP] docker compose 不可用"
    reg_summary
    exit "$REG_FAILED"
fi

if [ ! -f "$ROOT/docker/docker-compose.svid.yml" ]; then
    reg_fail "缺少 docker/docker-compose.svid.yml"
    reg_summary
    exit "$REG_FAILED"
fi

if ! config="$("${COMPOSE[@]}" config 2>/dev/null)"; then
    reg_fail "Compose SVID 覆盖层 config 失败"
else
    reg_pass "Compose SVID 覆盖层 config 通过"
fi

for svc in "${SERVICES[@]}"; do
    if grep -q "svid-$svc:" <<< "$config"; then
        reg_pass "sidecar 已定义：svid-$svc"
    else
        reg_fail "sidecar 缺失：svid-$svc"
    fi
done
count="$(printf '%s' "$config" | grep -c 'IBOOT_TLS_CERT_FILE: /svid/svid.0.pem' || true)"
[ "$count" -ge 10 ] && reg_pass "十个应用使用 SVID 证书路径" \
    || reg_fail "应用 SVID 证书路径数量异常：$count"

if ! docker image inspect iboot-svid-sidecar:local >/dev/null 2>&1; then
    if ! docker build -t iboot-svid-sidecar:local "$ROOT/docker/svid-sidecar" >/tmp/svid-sidecar-build.log 2>&1; then
        reg_fail "sidecar 镜像构建失败，详见 /tmp/svid-sidecar-build.log"
        reg_summary
        exit "$REG_FAILED"
    fi
fi
reg_pass "sidecar 镜像可用"

# SPIRE 与 iboot-system 条目由 dev-init 的 SVID setup 创建；此处只验证 sidecar 取回。
if ! docker ps --format '{{.Names}}' | grep -qx iboot-spire-agent; then
    reg_log "  [SKIP] SPIRE Agent 未运行，跳过 sidecar 取回"
    reg_summary
    exit "$REG_FAILED"
fi

# 复用当前项目的 svid 命名卷；测试结束后由部署/purge 流程统一清理。
if ! "${COMPOSE[@]}" up -d --no-deps svid-iboot-system >/tmp/compose-svid-up.log 2>&1; then
    reg_fail "sidecar 启动失败，详见 /tmp/compose-svid-up.log"
    reg_summary
    exit "$REG_FAILED"
fi
for _ in $(seq 1 60); do
    state="$(docker inspect -f '{{.State.Status}}:{{.State.ExitCode}}' docker-svid-iboot-system-1 2>/dev/null || true)"
    case "$state" in
        exited:0) break ;;
        exited:*) reg_fail "sidecar 失败：$state"; break ;;
    esac
    sleep 1
done
if [ "${state:-}" = "exited:0" ]; then
    reg_pass "sidecar 成功取回 SVID"
else
    reg_fail "sidecar 未成功完成：${state:-unknown}"
fi

check="$(docker run --rm -v docker_svid_iboot_system:/svid --entrypoint /bin/sh \
    nginx:1.30.6-alpine3.24 -c 'test -f /svid/svid.0.pem && test -f /svid/svid.0.key && echo ok' 2>/dev/null || true)"
[ "$check" = "ok" ] && reg_pass "SVID 卷包含证书与私钥" \
    || reg_fail "SVID 卷缺少证书或私钥"

"${COMPOSE[@]}" rm -sf svid-iboot-system >/dev/null 2>&1 || true

reg_summary
exit "$REG_FAILED"
