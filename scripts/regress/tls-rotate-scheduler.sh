#!/usr/bin/env bash
# 证书轮换调度回归：校验 docker 调度器配置与容器内一次性检查。
#
# 本脚本不启动常驻调度容器；只运行 TLS_ROTATE_RUN_ONCE=1 的一次性检查，
# 验证调度镜像、挂载路径与轮换脚本可用。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

IMAGE="${TLS_ROTATE_IMAGE:-maven:3.10.0-eclipse-temurin-25}"

if ! command -v docker >/dev/null 2>&1; then
    reg_log "  [SKIP] docker 不可用"
    reg_summary
    exit "$REG_FAILED"
fi

print_out="$(cd "$ROOT" && scripts/tls-rotate-schedule.sh --print --scheduler docker --restart=api 2>&1 || true)"
if printf '%s' "$print_out" | grep -q 'tls-rotate'; then
    reg_pass "docker 调度器配置可生成"
else
    reg_fail "docker 调度器配置生成失败：$print_out"
fi

one_shot="$(docker run --rm \
    --entrypoint /workspace/docker/tls-rotate/entrypoint.sh \
    -e TLS_ROTATE_RUN_ONCE=1 \
    -e TLS_ROTATE_RESTART=none \
    -e IBOOT_STATE_DIR=/tmp/iboot-state \
    -v "$ROOT:/workspace" \
    "$IMAGE" 2>&1)"
if printf '%s' "$one_shot" | grep -q '检查通过'; then
    reg_pass "调度容器一次性证书检查通过"
else
    reg_fail "调度容器一次性检查失败：$(printf '%s' "$one_shot" | tail -5)"
fi

if [ -f "$ROOT/docker/tls-rotate/entrypoint.sh" ] && [ -x "$ROOT/docker/tls-rotate/entrypoint.sh" ]; then
    reg_pass "调度容器入口可执行"
else
    reg_fail "调度容器入口缺失或不可执行"
fi

reg_summary
exit "$REG_FAILED"
