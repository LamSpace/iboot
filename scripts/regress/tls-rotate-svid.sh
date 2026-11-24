#!/usr/bin/env bash
# tls-rotate 与 Compose SVID 重启路径回归。
#
# 用假 docker 与假 docker-svid-refresh.sh 验证：
#   - 存在 Compose SVID sidecar 时，--restart docker|rolling 委托 SVID 刷新入口；
#   - 不存在 SVID 栈时仍走基础 compose 重建，不误触发 SVID 刷新。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin"

export FAKE_DOCKER_CALLS="$TMP/docker-calls.log"
export FAKE_SVID_CALLS="$TMP/svid-calls.log"
export IBOOT_DOCKER_SVID_REFRESH_BIN="$TMP/bin/docker-svid-refresh"

cat > "$TMP/bin/docker" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${FAKE_DOCKER_CALLS:?}"
if [ "${1:-}" = "ps" ]; then
    if [ "${FAKE_SVID_STACK:-0}" = "1" ]; then
        printf 'docker-svid-iboot-system-1\n'
    fi
    exit 0
fi
if [ "${1:-}" = "compose" ]; then
    case "$*" in
        *"ps --services"*) printf 'iboot-system\n' ;;
    esac
    exit 0
fi
exit 0
EOF
cat > "$IBOOT_DOCKER_SVID_REFRESH_BIN" <<'EOF'
#!/usr/bin/env bash
printf 'refresh\n' >> "${FAKE_SVID_CALLS:?}"
exit 0
EOF
chmod +x "$TMP/bin/docker" "$IBOOT_DOCKER_SVID_REFRESH_BIN"

run_rotate() {
    PATH="$TMP/bin:$PATH" "$ROOT/scripts/tls-rotate.sh" --restart-only --restart="$1" 2>&1
}

reg_log "== Compose SVID 栈 =="
export FAKE_SVID_STACK=1
for mode in docker rolling; do
    : > "$FAKE_DOCKER_CALLS"
    : > "$FAKE_SVID_CALLS"
    out="$(run_rotate "$mode")"
    if printf '%s' "$out" | grep -q '委托 docker-svid-refresh.sh'; then
        reg_pass "--restart $mode 识别 Compose SVID 栈"
    else
        reg_fail "--restart $mode 未识别 Compose SVID 栈：$(printf '%s' "$out" | tail -3)"
    fi
    if [ -s "$FAKE_SVID_CALLS" ]; then
        reg_pass "--restart $mode 委托 docker-svid-refresh.sh"
    else
        reg_fail "--restart $mode 未调用 docker-svid-refresh.sh"
    fi
    if grep -q 'compose .* up' "$FAKE_DOCKER_CALLS"; then
        reg_fail "--restart $mode 仍按基础 compose 重建"
    else
        reg_pass "--restart $mode 未使用基础 compose 重建"
    fi
done

reg_log "== 非 SVID 栈 =="
export FAKE_SVID_STACK=0
: > "$FAKE_DOCKER_CALLS"
: > "$FAKE_SVID_CALLS"
run_rotate docker >/dev/null 2>&1 || true
if grep -q 'compose .* up -d --force-recreate iboot-system' "$FAKE_DOCKER_CALLS"; then
    reg_pass "非 SVID 栈仍走基础 compose 重建"
else
    reg_fail "非 SVID 栈未执行基础 compose 重建"
fi
if [ -s "$FAKE_SVID_CALLS" ]; then
    reg_fail "非 SVID 栈误触发 SVID 刷新"
else
    reg_pass "非 SVID 栈未触发 SVID 刷新"
fi

reg_summary
exit "$REG_FAILED"
