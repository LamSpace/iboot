#!/usr/bin/env bash
# SVID 刷新调度回归：安装、幂等、卸载、检查/刷新路径、锁与指标输出。
#
# 使用临时 HOME、假 spire-svid-sync/docker-svid-refresh/crontab/systemctl，
# 不修改开发者真实定时任务或 SVID。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

MARKER_BEGIN="# >>> iboot-svid-refresh >>>"
SCHED="$ROOT/scripts/svid-refresh-schedule.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/bin" "$TMP/home" "$TMP/config" "$TMP/state" "$TMP/systemd" "$TMP/logs" "$TMP/run" "$TMP/metrics" "$TMP/certs"
export HOME="$TMP/home"
export XDG_CONFIG_HOME="$TMP/config"
export XDG_STATE_HOME="$TMP/state"
export XDG_RUNTIME_DIR="$TMP/run"
export IBOOT_LOG_DIR="$TMP/logs"
export IBOOT_STATE_DIR="$TMP/state/iboot"
export IBOOT_SYSTEMD_USER_DIR="$TMP/systemd"
export IBOOT_CRONTAB_BIN="$TMP/bin/crontab"
export IBOOT_SYSTEMCTL_BIN="$TMP/bin/systemctl"
export IBOOT_SPIRE_SVID_SYNC_BIN="$TMP/bin/spire-svid-sync"
export IBOOT_DOCKER_SVID_REFRESH_BIN="$TMP/bin/docker-svid-refresh"
export IBOOT_METRIC_DIR="$TMP/metrics"
export FAKE_CRONTAB_STORE="$TMP/crontab.txt"
export FAKE_SYSTEMCTL_LOG="$TMP/systemctl.log"
export FAKE_SYNC_CALLS="$TMP/sync-calls.log"
export FAKE_DOCKER_CALLS="$TMP/docker-calls.log"
export FAKE_CHECK_STATE="$TMP/check-state"
export FAKE_SYSTEMD_AVAILABLE=0
export FAKE_CHECK_RC=0
export FAKE_CHECK_AFTER_RC=0
export FAKE_REFRESH_RC=0
export FAKE_MIN_SECONDS=2592000

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
    *) exit 2 ;;
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
cat > "$IBOOT_SPIRE_SVID_SYNC_BIN" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${FAKE_SYNC_CALLS:?}"
case "${1:-}" in
    --check)
        # 刷新前的第一次检查返回 FAKE_CHECK_RC；刷新后的检查返回 FAKE_CHECK_AFTER_RC。
        if [ -f "${FAKE_CHECK_STATE:?}" ]; then
            rc="${FAKE_CHECK_AFTER_RC:-0}"
        else
            printf 'seen\n' > "$FAKE_CHECK_STATE"
            rc="${FAKE_CHECK_RC:-0}"
        fi
        printf 'min_remaining_seconds=%s\n' "${FAKE_MIN_SECONDS:-0}"
        exit "$rc"
        ;;
    --refresh) exit "${FAKE_REFRESH_RC:-0}" ;;
esac
exit 0
EOF
cat > "$IBOOT_DOCKER_SVID_REFRESH_BIN" <<'EOF'
#!/usr/bin/env bash
printf 'docker-svid-refresh %s\n' "$*" >> "${FAKE_DOCKER_CALLS:?}"
exit "${FAKE_REFRESH_RC:-0}"
EOF
chmod +x "$IBOOT_CRONTAB_BIN" "$IBOOT_SYSTEMCTL_BIN" "$IBOOT_SPIRE_SVID_SYNC_BIN" "$IBOOT_DOCKER_SVID_REFRESH_BIN"

reset_fake() {
    rm -f "$FAKE_CRONTAB_STORE" "$FAKE_SYSTEMCTL_LOG" "$FAKE_SYNC_CALLS" "$FAKE_DOCKER_CALLS" "$FAKE_CHECK_STATE"
    rm -rf "$IBOOT_STATE_DIR" "$IBOOT_METRIC_DIR"
    mkdir -p "$IBOOT_METRIC_DIR"
    export FAKE_SYSTEMD_AVAILABLE=0
    export FAKE_CHECK_RC=0
    export FAKE_CHECK_AFTER_RC=0
    export FAKE_REFRESH_RC=0
}

reg_log "== 调度内容 =="
for arg in '--scheduler=cron' '--scheduler=systemd'; do
    out="$("$SCHED" --print "$arg" --target=host --threshold-days=5 --at=04:15)"
    if printf '%s' "$out" | grep -q -- '--target=host' && printf '%s' "$out" | grep -q -- '--threshold-days=5'; then
        reg_pass "$arg 输出包含目标与阈值"
    else
        reg_fail "$arg 输出缺少目标或阈值"
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
"$SCHED" --install --scheduler=cron --target=compose --at=04:15 >/dev/null 2>&1
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
"$SCHED" --install --scheduler=cron --target=host --at=05:00 >/dev/null 2>&1
if [ "$(grep -Fxc "$MARKER_BEGIN" "$FAKE_CRONTAB_STORE")" = "1" ]; then
    reg_pass "重复安装不产生重复标记块"
else
    reg_fail "重复安装产生重复标记块"
fi
if grep -Fq -- '--target=host' "$FAKE_CRONTAB_STORE"; then
    reg_pass "重复安装更新目标"
else
    reg_fail "重复安装未更新目标"
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
"$SCHED" --install --scheduler=systemd --target=host --at=04:15 >/dev/null 2>&1
if [ -f "$IBOOT_SYSTEMD_USER_DIR/iboot-svid-refresh.service" ] \
        && [ -f "$IBOOT_SYSTEMD_USER_DIR/iboot-svid-refresh.timer" ]; then
    reg_pass "systemd 单元已写入临时目录"
else
    reg_fail "systemd 单元缺失"
fi
if grep -Fq 'daemon-reload' "$FAKE_SYSTEMCTL_LOG" \
        && grep -Fq 'enable --now iboot-svid-refresh.timer' "$FAKE_SYSTEMCTL_LOG"; then
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
if [ ! -f "$IBOOT_SYSTEMD_USER_DIR/iboot-svid-refresh.service" ] \
        && [ ! -f "$IBOOT_SYSTEMD_USER_DIR/iboot-svid-refresh.timer" ]; then
    reg_pass "systemd 卸载移除单元"
else
    reg_fail "systemd 卸载未移除单元"
fi

reg_log "== 检查通过不刷新 =="
reset_fake
"$SCHED" --run-now --target=host >/dev/null 2>&1
if grep -Fqx -- '--check --threshold-days 7' "$FAKE_SYNC_CALLS" && ! grep -Fq -- '--refresh' "$FAKE_SYNC_CALLS"; then
    reg_pass "检查通过时只调用 --check"
else
    reg_fail "检查通过时仍触发了刷新"
fi
if grep -Fq 'check ok' "$IBOOT_LOG_DIR/svid-refresh.log"; then
    reg_pass "检查通过写入日志"
else
    reg_fail "检查通过未写入日志"
fi
if grep -q '^iboot_svid_refresh_last_exit_code 0$' "$IBOOT_METRIC_DIR/svid-refresh.prom" \
        && grep -q '^iboot_svid_cert_min_remaining_seconds 2592000$' "$IBOOT_METRIC_DIR/svid-refresh.prom"; then
    reg_pass "检查通过写入成功指标"
else
    reg_fail "检查通过未写入预期指标"
fi

reg_log "== 宿主机刷新路径 =="
reset_fake
export FAKE_CHECK_RC=1
"$SCHED" --run-now --target=host >/dev/null 2>&1
if grep -Eq -- '--refresh --restart (none|host)' "$FAKE_SYNC_CALLS"; then
    reg_pass "宿主机目标执行 --refresh"
else
    reg_fail "宿主机目标未执行 --refresh"
fi
if grep -Fq 'refresh ok target=host' "$IBOOT_LOG_DIR/svid-refresh.log"; then
    reg_pass "宿主机刷新成功写入日志"
else
    reg_fail "宿主机刷新成功未写入日志"
fi
if grep -q '^iboot_svid_refresh_last_exit_code 0$' "$IBOOT_METRIC_DIR/svid-refresh.prom"; then
    reg_pass "刷新后指标恢复正常"
else
    reg_fail "刷新后指标仍为失败"
fi

reg_log "== Compose 刷新路径 =="
reset_fake
export FAKE_CHECK_RC=1
"$SCHED" --run-now --target=compose >/dev/null 2>&1
if grep -Fq 'docker-svid-refresh' "$FAKE_DOCKER_CALLS"; then
    reg_pass "Compose 目标委托 docker-svid-refresh.sh"
else
    reg_fail "Compose 目标未委托 docker-svid-refresh.sh"
fi

reg_log "== 刷新失败与锁 =="
reset_fake
export FAKE_CHECK_RC=1
export FAKE_REFRESH_RC=1
"$SCHED" --run-now --target=host >/dev/null 2>&1
if grep -q '^iboot_svid_refresh_last_exit_code 1$' "$IBOOT_METRIC_DIR/svid-refresh.prom"; then
    reg_pass "刷新失败写入失败指标"
else
    reg_fail "刷新失败未写入失败指标"
fi
if grep -Fq 'refresh failed target=host' "$IBOOT_LOG_DIR/svid-refresh.log"; then
    reg_pass "刷新失败写入日志"
else
    reg_fail "刷新失败未写入日志"
fi

reset_fake
mkdir -p "$IBOOT_STATE_DIR/svid-refresh.lock.d"
if ! "$SCHED" --run-now --target=host >/dev/null 2>&1; then
    reg_pass "锁已存在时拒绝并发运行"
else
    reg_fail "锁已存在时仍运行"
fi
if grep -Fq 'another refresh holds' "$IBOOT_LOG_DIR/svid-refresh.log"; then
    reg_pass "并发拒绝写入日志"
else
    reg_fail "并发拒绝未写入日志"
fi

reset_fake
mkdir -p "$IBOOT_STATE_DIR/svid-refresh.lock.d"
printf '999999\n' > "$IBOOT_STATE_DIR/svid-refresh.lock.d/pid"
if "$SCHED" --run-now --target=host >/dev/null 2>&1; then
    reg_pass "过期锁被清理后继续运行"
else
    reg_fail "过期锁阻塞了运行"
fi

reg_summary
exit "$REG_FAILED"
