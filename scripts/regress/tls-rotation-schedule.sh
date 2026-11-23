#!/usr/bin/env bash
# 证书轮换调度回归：安装、幂等、卸载、运行路径与锁。
#
# 使用临时 HOME 与假 crontab/systemctl/tls-rotate，不修改开发者真实定时任务或证书。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

MARKER_BEGIN="# >>> iboot-tls-rotate >>>"
SCHED="$ROOT/scripts/tls-rotate-schedule.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/bin" "$TMP/home" "$TMP/config" "$TMP/state" "$TMP/systemd" "$TMP/logs" "$TMP/run"
export HOME="$TMP/home"
export XDG_CONFIG_HOME="$TMP/config"
export XDG_STATE_HOME="$TMP/state"
export XDG_RUNTIME_DIR="$TMP/run"
export IBOOT_LOG_DIR="$TMP/logs"
export IBOOT_STATE_DIR="$TMP/state/iboot"
export IBOOT_SYSTEMD_USER_DIR="$TMP/systemd"
export IBOOT_CRONTAB_BIN="$TMP/bin/crontab"
export IBOOT_SYSTEMCTL_BIN="$TMP/bin/systemctl"
export IBOOT_TLS_ROTATE_BIN="$TMP/bin/tls-rotate"
export FAKE_CRONTAB_STORE="$TMP/crontab.txt"
export FAKE_SYSTEMCTL_LOG="$TMP/systemctl.log"
export FAKE_TLS_CALLS="$TMP/tls-calls.log"
export FAKE_SYSTEMD_AVAILABLE=0
export FAKE_CHECK_RC=0
export FAKE_ROTATE_RC=0

cat > "$IBOOT_CRONTAB_BIN" <<'EOF'
#!/usr/bin/env bash
store="${FAKE_CRONTAB_STORE:?}"
case "${1:-}" in
    -l)
        if [ -f "$store" ]; then cat "$store"; else exit 1; fi
        ;;
    -)
        cat > "$store"
        ;;
    *)
        exit 2
        ;;
esac
EOF
cat > "$IBOOT_SYSTEMCTL_BIN" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${FAKE_SYSTEMCTL_LOG:?}"
if [ "${1:-}" = "--user" ] && [ "${2:-}" = "show-environment" ]; then
    exit "${FAKE_SYSTEMD_AVAILABLE:-0}"
fi
exit 0
EOF
cat > "$IBOOT_TLS_ROTATE_BIN" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${FAKE_TLS_CALLS:?}"
case "${1:-}" in
    --check) exit "${FAKE_CHECK_RC:-0}" ;;
    --rotate) exit "${FAKE_ROTATE_RC:-0}" ;;
esac
exit 0
EOF
chmod +x "$IBOOT_CRONTAB_BIN" "$IBOOT_SYSTEMCTL_BIN" "$IBOOT_TLS_ROTATE_BIN"

reset_fake() {
    rm -f "$FAKE_CRONTAB_STORE" "$FAKE_SYSTEMCTL_LOG" "$FAKE_TLS_CALLS"
    rm -rf "$IBOOT_STATE_DIR"
    export FAKE_SYSTEMD_AVAILABLE=0
    export FAKE_CHECK_RC=0
    export FAKE_ROTATE_RC=0
}

reg_log "== 调度内容 =="
for arg in '--scheduler=cron' '--scheduler=systemd'; do
    out="$("$SCHED" --print "$arg" --restart=docker --at=04:15)"
    if printf '%s' "$out" | grep -q 'restart=docker\|--restart=docker'; then
        reg_pass "$arg 输出包含重启范围"
    else
        reg_fail "$arg 输出缺少重启范围"
    fi
done
cron_print="$("$SCHED" --print --scheduler=cron --at=04:15)"
if printf '%s\n' "$cron_print" | grep -q '^15 4 \* \* \*'; then
    reg_pass "cron 时间按 HH:MM 生成"
else
    reg_fail "cron 时间生成不正确"
fi
systemd_print="$("$SCHED" --print --scheduler=systemd --at=04:15)"
if printf '%s\n' "$systemd_print" | grep -q 'OnCalendar=.* 04:15:00'; then
    reg_pass "systemd OnCalendar 按 HH:MM 生成"
else
    reg_fail "systemd OnCalendar 生成不正确"
fi

reg_log "== cron 安装、幂等与卸载 =="
reset_fake
printf '5 * * * * echo keep\n' > "$FAKE_CRONTAB_STORE"
"$SCHED" --install --scheduler=cron --restart=docker --at=04:15 >/dev/null 2>&1
if [ "$(grep -Fxc "$MARKER_BEGIN" "$FAKE_CRONTAB_STORE")" = "1" ]; then
    reg_pass "cron 首次安装只有一个标记块"
else
    reg_fail "cron 首次安装标记块数量不为 1"
fi
if grep -Fq 'echo keep' "$FAKE_CRONTAB_STORE"; then
    reg_pass "cron 安装保留既有任务"
else
    reg_fail "cron 安装删除了既有任务"
fi
"$SCHED" --install --scheduler=cron --restart=host --at=05:00 >/dev/null 2>&1
if [ "$(grep -Fxc "$MARKER_BEGIN" "$FAKE_CRONTAB_STORE")" = "1" ]; then
    reg_pass "重复安装不产生重复标记块"
else
    reg_fail "重复安装产生重复标记块"
fi
if grep -Fq -- '--restart=host' "$FAKE_CRONTAB_STORE"; then
    reg_pass "重复安装更新重启范围"
else
    reg_fail "重复安装未更新重启范围"
fi
status_out="$("$SCHED" --status 2>/dev/null)" || true
if printf '%s\n' "$status_out" | grep -q '^scheduler=cron'; then
    reg_pass "status 报告 cron"
else
    reg_fail "status 未报告 cron"
fi
"$SCHED" --uninstall >/dev/null 2>&1
if ! grep -Fq "$MARKER_BEGIN" "$FAKE_CRONTAB_STORE"; then
    reg_pass "卸载移除 cron 标记块"
else
    reg_fail "卸载未移除 cron 标记块"
fi
if grep -Fq 'echo keep' "$FAKE_CRONTAB_STORE"; then
    reg_pass "卸载保留既有任务"
else
    reg_fail "卸载删除了既有任务"
fi

reg_log "== systemd 安装与卸载 =="
reset_fake
"$SCHED" --install --scheduler=systemd --restart=host --at=04:15 >/dev/null 2>&1
if [ -f "$IBOOT_SYSTEMD_USER_DIR/iboot-tls-rotate.service" ] && [ -f "$IBOOT_SYSTEMD_USER_DIR/iboot-tls-rotate.timer" ]; then
    reg_pass "systemd 单元已写入临时目录"
else
    reg_fail "systemd 单元缺失"
fi
if grep -Fq 'daemon-reload' "$FAKE_SYSTEMCTL_LOG" && grep -Fq 'enable --now iboot-tls-rotate.timer' "$FAKE_SYSTEMCTL_LOG"; then
    reg_pass "systemd 安装调用 daemon-reload 与 enable"
else
    reg_fail "systemd 安装未调用预期命令"
fi
status_out="$("$SCHED" --status 2>/dev/null)" || true
if printf '%s\n' "$status_out" | grep -q '^scheduler=systemd'; then
    reg_pass "status 报告 systemd"
else
    reg_fail "status 未报告 systemd"
fi
"$SCHED" --uninstall >/dev/null 2>&1
if [ ! -f "$IBOOT_SYSTEMD_USER_DIR/iboot-tls-rotate.service" ] && [ ! -f "$IBOOT_SYSTEMD_USER_DIR/iboot-tls-rotate.timer" ]; then
    reg_pass "systemd 卸载移除单元"
else
    reg_fail "systemd 卸载未移除单元"
fi

reg_log "== auto 回退 =="
reset_fake
export FAKE_SYSTEMD_AVAILABLE=1
auto_print="$("$SCHED" --print)"
if printf '%s\n' "$auto_print" | grep -q "$MARKER_BEGIN"; then
    reg_pass "systemd 不可用时 auto 回退 cron"
else
    reg_fail "systemd 不可用时 auto 未回退 cron"
fi

reg_log "== 运行路径 =="
reset_fake
"$SCHED" --run-now --restart=docker >/dev/null 2>&1
if grep -Fqx -- '--check' "$FAKE_TLS_CALLS" && ! grep -Fq -- '--rotate' "$FAKE_TLS_CALLS"; then
    reg_pass "检查通过时只调用 --check"
else
    reg_fail "检查通过时仍调用了重签"
fi
if grep -Fq 'check ok' "$IBOOT_LOG_DIR/tls-rotate.log"; then
    reg_pass "检查通过写入日志"
else
    reg_fail "检查通过未写入日志"
fi

reset_fake
export FAKE_CHECK_RC=1
"$SCHED" --run-now --restart=docker >/dev/null 2>&1
if grep -Fq -- '--rotate --restart=docker' "$FAKE_TLS_CALLS"; then
    reg_pass "检查失败时按范围重签"
else
    reg_fail "检查失败时未按范围重签"
fi
if grep -Fq 'rotate ok' "$IBOOT_LOG_DIR/tls-rotate.log"; then
    reg_pass "重签成功写入日志"
else
    reg_fail "重签成功未写入日志"
fi

reset_fake
mkdir -p "$IBOOT_STATE_DIR/tls-rotate.lock.d"
if ! "$SCHED" --run-now --restart=none >/dev/null 2>&1; then
    reg_pass "锁已存在时拒绝并发运行"
else
    reg_fail "锁已存在时仍运行"
fi
if grep -Fq 'another rotation' "$IBOOT_LOG_DIR/tls-rotate.log"; then
    reg_pass "并发拒绝写入日志"
else
    reg_fail "并发拒绝未写入日志"
fi

reset_fake
mkdir -p "$IBOOT_STATE_DIR/tls-rotate.lock.d"
printf '999999\n' > "$IBOOT_STATE_DIR/tls-rotate.lock.d/pid"
if "$SCHED" --run-now --restart=none >/dev/null 2>&1; then
    reg_pass "过期锁被清理后继续运行"
else
    reg_fail "过期锁阻塞了运行"
fi
if grep -Fq 'removing stale lock' "$IBOOT_LOG_DIR/tls-rotate.log"; then
    reg_pass "过期锁清理写入日志"
else
    reg_fail "过期锁清理未写入日志"
fi

reg_summary
exit "$REG_FAILED"
