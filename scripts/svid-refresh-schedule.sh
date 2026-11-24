#!/usr/bin/env bash
# SVID 刷新周期调度入口。
#
# 默认不安装任何任务；--install 显式安装，--run-now 只执行一次。
# 每次运行先检查 SVID 剩余有效期，低于阈值（默认 7 天）才刷新：
#   - 宿主机服务：scripts/spire-svid-sync.sh --refresh --restart host
#   - Compose SVID 栈：scripts/docker-svid-refresh.sh
# 结果写入 logs/svid-refresh.log 与运行期指标文件，供 Prometheus 文本文件采集。
#
# 用法：
#   scripts/svid-refresh-schedule.sh --install [--scheduler auto|systemd|cron] [--target auto|host|compose] [--at HH:MM] [--threshold-days N]
#   scripts/svid-refresh-schedule.sh --uninstall [--scheduler auto|systemd|cron]
#   scripts/svid-refresh-schedule.sh --status [--scheduler auto|systemd|cron]
#   scripts/svid-refresh-schedule.sh --run-now [--target auto|host|compose] [--threshold-days N]
#   scripts/svid-refresh-schedule.sh --print [--scheduler auto|systemd|cron] [--target ...] [--at HH:MM] [--threshold-days N]

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MODE=""
TARGET="auto"
SCHEDULER="auto"
AT="04:00"
THRESHOLD_DAYS="${IBOOT_SVID_REFRESH_THRESHOLD_DAYS:-7}"

UNIT="iboot-svid-refresh"
MARKER_BEGIN="# >>> iboot-svid-refresh >>>"
MARKER_END="# <<< iboot-svid-refresh <<<"

LOG_DIR="${IBOOT_LOG_DIR:-$ROOT/logs}"
LOG_FILE="$LOG_DIR/svid-refresh.log"
STATE_DIR="${IBOOT_STATE_DIR:-${XDG_STATE_HOME:-$HOME/.local/state}/iboot}"
SYSTEMD_USER_DIR="${IBOOT_SYSTEMD_USER_DIR:-${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user}"
SYSTEMCTL="${IBOOT_SYSTEMCTL_BIN:-systemctl}"
CRONTAB="${IBOOT_CRONTAB_BIN:-crontab}"
SPIRE_SVID_SYNC="${IBOOT_SPIRE_SVID_SYNC_BIN:-$ROOT/scripts/spire-svid-sync.sh}"
DOCKER_SVID_REFRESH="${IBOOT_DOCKER_SVID_REFRESH_BIN:-$ROOT/scripts/docker-svid-refresh.sh}"
CERT_DIR="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
METRIC_DIR="${IBOOT_METRIC_DIR:-$CERT_DIR/runtime}"
METRIC_FILE="$METRIC_DIR/svid-refresh.prom"
LAST_SUCCESS_FILE="$STATE_DIR/svid-refresh.last-success"
LOCK_HELD=""

usage() {
    awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

say() { printf '[svid-refresh-schedule] %s\n' "$*"; }
die() { say "$*" >&2; exit 2; }

log() {
    mkdir -p "$LOG_DIR"
    printf '%s %s\n' "$(date -Iseconds)" "$*" >> "$LOG_FILE"
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --install) MODE="install" ;;
        --uninstall) MODE="uninstall" ;;
        --status) MODE="status" ;;
        --run-now) MODE="run-now" ;;
        --print) MODE="print" ;;
        --target)
            [ "$#" -ge 2 ] || die "--target 缺少取值（auto|host|compose）"
            TARGET="$2"
            shift
            ;;
        --target=*) TARGET="${1#*=}" ;;
        --scheduler)
            [ "$#" -ge 2 ] || die "--scheduler 缺少取值（auto|systemd|cron）"
            SCHEDULER="$2"
            shift
            ;;
        --scheduler=*) SCHEDULER="${1#*=}" ;;
        --threshold-days)
            [ "$#" -ge 2 ] || die "--threshold-days 缺少取值（正整数）"
            THRESHOLD_DAYS="$2"
            shift
            ;;
        --threshold-days=*) THRESHOLD_DAYS="${1#*=}" ;;
        --at)
            [ "$#" -ge 2 ] || die "--at 缺少取值（HH:MM）"
            AT="$2"
            shift
            ;;
        --at=*) AT="${1#*=}" ;;
        -h|--help) usage; exit 0 ;;
        *) die "未知参数：$1" ;;
    esac
    shift
done

[ -n "$MODE" ] || { usage >&2; exit 2; }
case "$TARGET" in
    auto|host|compose) ;;
    *) die "未知目标：$TARGET（auto|host|compose）" ;;
esac
case "$SCHEDULER" in
    auto|systemd|cron) ;;
    *) die "未知调度器：$SCHEDULER（auto|systemd|cron）" ;;
esac
case "$THRESHOLD_DAYS" in
    ''|*[!0-9]*|0) die "阈值必须是正整数：$THRESHOLD_DAYS" ;;
esac
printf '%s' "$AT" | grep -Eq '^([01][0-9]|2[0-3]):[0-5][0-9]$' || die "时间格式必须是 HH:MM：$AT"

systemd_available() {
    command -v "$SYSTEMCTL" >/dev/null 2>&1 || return 1
    [ -n "${XDG_RUNTIME_DIR:-}" ] || return 1
    "$SYSTEMCTL" --user show-environment >/dev/null 2>&1
}

resolve_scheduler() {
    case "$SCHEDULER" in
        systemd) printf 'systemd' ;;
        cron) printf 'cron' ;;
        auto)
            if systemd_available; then
                printf 'systemd'
            else
                printf 'cron'
            fi
            ;;
    esac
}

compose_svid_active() {
    command -v docker >/dev/null 2>&1 || return 1
    docker ps -a --format '{{.Names}}' 2>/dev/null | grep -qE '^docker-svid-.*-1$'
}

host_services_running() {
    pgrep -f 'iboot-[a-z]+-1\.0\.0\.jar' >/dev/null 2>&1
}

resolve_target() {
    case "$TARGET" in
        host|compose) printf '%s' "$TARGET" ;;
        auto)
            if compose_svid_active; then
                printf 'compose'
            else
                printf 'host'
            fi
            ;;
    esac
}

read_metric() {
    # 从上次指标文件中取字段；缺失时输出空字符串。
    local key="$1"
    [ -f "$METRIC_FILE" ] || return 0
    sed -n "s/^${key} //p" "$METRIC_FILE" 2>/dev/null | tail -1
}

write_metrics() {
    local exit_code="$1" min_seconds="$2" last_success="$3"
    mkdir -p "$METRIC_DIR"
    local tmp="$METRIC_FILE.tmp"
    {
        printf '# HELP iboot_svid_refresh_last_success_timestamp_seconds 最近一次 SVID 刷新成功时间。\n'
        printf '# TYPE iboot_svid_refresh_last_success_timestamp_seconds gauge\n'
        printf 'iboot_svid_refresh_last_success_timestamp_seconds %s\n' "$last_success"
        printf '# HELP iboot_svid_refresh_last_exit_code 最近一次 SVID 检查/刷新退出码。\n'
        printf '# TYPE iboot_svid_refresh_last_exit_code gauge\n'
        printf 'iboot_svid_refresh_last_exit_code %s\n' "$exit_code"
        printf '# HELP iboot_svid_cert_min_remaining_seconds 十个服务 SVID 的最小剩余有效期。\n'
        printf '# TYPE iboot_svid_cert_min_remaining_seconds gauge\n'
        printf 'iboot_svid_cert_min_remaining_seconds %s\n' "$min_seconds"
    } > "$tmp"
    mv -f "$tmp" "$METRIC_FILE"
}

service_content() {
    cat <<EOF
[Unit]
Description=iBoot SVID expiry check and refresh

[Service]
Type=oneshot
WorkingDirectory=$ROOT
# systemd user 环境默认 PATH 不含 SDKMAN/JDK 与用户级工具；固化安装时的 PATH。
Environment="PATH=$PATH"
ExecStart=$ROOT/scripts/svid-refresh-schedule.sh --run-now --target=$TARGET --threshold-days=$THRESHOLD_DAYS
EOF
}

timer_content() {
    cat <<EOF
[Unit]
Description=Daily iBoot SVID expiry check

[Timer]
OnCalendar=*-*-* $AT:00
Persistent=true
RandomizedDelaySec=600

[Install]
WantedBy=timers.target
EOF
}

cron_time() {
    local hour="${AT%%:*}" minute="${AT##*:}"
    printf '%d %d * * *' "$((10#$minute))" "$((10#$hour))"
}

cron_block() {
    cat <<EOF
$MARKER_BEGIN
$(cron_time) cd "$ROOT" && "$ROOT/scripts/svid-refresh-schedule.sh" --run-now --target=$TARGET --threshold-days=$THRESHOLD_DAYS >> "$LOG_FILE" 2>&1
$MARKER_END
EOF
}

print_schedule() {
    local scheduler="$1"
    case "$scheduler" in
        systemd)
            printf '# %s/%s.service\n' "$SYSTEMD_USER_DIR" "$UNIT"
            service_content
            printf '\n# %s/%s.timer\n' "$SYSTEMD_USER_DIR" "$UNIT"
            timer_content
            ;;
        cron)
            cron_block
            ;;
        *) die "未知调度器：$scheduler" ;;
    esac
}

read_crontab() {
    command -v "$CRONTAB" >/dev/null 2>&1 || return 0
    "$CRONTAB" -l 2>/dev/null || true
}

strip_cron_block() {
    awk -v begin="$MARKER_BEGIN" -v end="$MARKER_END" '
        $0 == begin { skip = 1; next }
        $0 == end   { skip = 0; next }
        !skip
    '
}

cron_installed() {
    read_crontab | grep -Fqx "$MARKER_BEGIN"
}

install_cron() {
    command -v "$CRONTAB" >/dev/null 2>&1 || die "缺少 crontab，无法安装 cron 调度"
    local current cleaned
    current="$(read_crontab)"
    cleaned="$(printf '%s\n' "$current" | strip_cron_block)"
    {
        if [ -n "$cleaned" ]; then
            printf '%s\n' "$cleaned"
        fi
        cron_block
    } | "$CRONTAB" -
    say "已安装 cron 调度（$AT，target=$TARGET，threshold=${THRESHOLD_DAYS}d）"
}

install_systemd() {
    mkdir -p "$SYSTEMD_USER_DIR"
    service_content > "$SYSTEMD_USER_DIR/$UNIT.service"
    timer_content > "$SYSTEMD_USER_DIR/$UNIT.timer"
    if ! "$SYSTEMCTL" --user daemon-reload; then
        rm -f "$SYSTEMD_USER_DIR/$UNIT.service" "$SYSTEMD_USER_DIR/$UNIT.timer"
        return 1
    fi
    if ! "$SYSTEMCTL" --user enable --now "$UNIT.timer"; then
        "$SYSTEMCTL" --user disable --now "$UNIT.timer" >/dev/null 2>&1 || true
        rm -f "$SYSTEMD_USER_DIR/$UNIT.service" "$SYSTEMD_USER_DIR/$UNIT.timer"
        "$SYSTEMCTL" --user daemon-reload >/dev/null 2>&1 || true
        return 1
    fi
    say "已安装 systemd user timer（$AT，target=$TARGET，threshold=${THRESHOLD_DAYS}d）"
}

uninstall_systemd() {
    local removed=0
    if command -v "$SYSTEMCTL" >/dev/null 2>&1; then
        "$SYSTEMCTL" --user disable --now "$UNIT.timer" >/dev/null 2>&1 && removed=1 || true
    fi
    if [ -f "$SYSTEMD_USER_DIR/$UNIT.timer" ] || [ -f "$SYSTEMD_USER_DIR/$UNIT.service" ]; then
        rm -f "$SYSTEMD_USER_DIR/$UNIT.service" "$SYSTEMD_USER_DIR/$UNIT.timer"
        removed=1
    fi
    if [ "$removed" -eq 1 ] && command -v "$SYSTEMCTL" >/dev/null 2>&1; then
        "$SYSTEMCTL" --user daemon-reload >/dev/null 2>&1 || true
    fi
    [ "$removed" -eq 1 ]
}

uninstall_cron() {
    command -v "$CRONTAB" >/dev/null 2>&1 || return 1
    cron_installed || return 1
    local cleaned
    cleaned="$(read_crontab | strip_cron_block)"
    if [ -n "$cleaned" ]; then
        printf '%s\n' "$cleaned" | "$CRONTAB" -
    else
        printf '' | "$CRONTAB" -
    fi
    say "已移除 cron 调度"
}

refresh_target() {
    local target="$1"
    case "$target" in
        compose)
            [ -x "$DOCKER_SVID_REFRESH" ] || die "刷新脚本不可执行：$DOCKER_SVID_REFRESH"
            "$DOCKER_SVID_REFRESH"
            ;;
        host)
            [ -x "$SPIRE_SVID_SYNC" ] || die "同步脚本不可执行：$SPIRE_SVID_SYNC"
            if host_services_running; then
                "$SPIRE_SVID_SYNC" --refresh --restart host
            else
                # 宿主机服务未运行：只刷新文件，下次启动即加载新 SVID。
                "$SPIRE_SVID_SYNC" --refresh --restart none
            fi
            ;;
    esac
}

run_now() {
    mkdir -p "$LOG_DIR" "$STATE_DIR" "$METRIC_DIR"
    local lock_dir="$STATE_DIR/svid-refresh.lock.d" acquired=0 owner=""
    if mkdir "$lock_dir" 2>/dev/null; then
        acquired=1
    else
        owner="$(cat "$lock_dir/pid" 2>/dev/null || true)"
        if [ -n "$owner" ] && ! kill -0 "$owner" 2>/dev/null; then
            log "removing stale lock held by pid $owner"
            rm -rf -- "$lock_dir"
            if mkdir "$lock_dir" 2>/dev/null; then
                acquired=1
            fi
        fi
    fi
    if [ "$acquired" -ne 1 ]; then
        log "run skipped: another refresh holds $lock_dir"
        say "已有 SVID 刷新在运行，跳过" >&2
        return 1
    fi
    printf '%s\n' "$$" > "$lock_dir/pid"
    LOCK_HELD="$lock_dir"
    trap 'rm -rf -- "$LOCK_HELD" 2>/dev/null || true' EXIT

    log "run start target=$TARGET threshold=${THRESHOLD_DAYS}d"
    local check_out check_rc=0
    check_out="$("$SPIRE_SVID_SYNC" --check --threshold-days "$THRESHOLD_DAYS" 2>&1)" || check_rc=$?
    printf '%s\n' "$check_out" | sed 's/^/[svid-refresh-schedule]   /'
    local min_seconds
    min_seconds="$(printf '%s\n' "$check_out" | sed -n 's/^min_remaining_seconds=//p' | tail -1)"
    case "$min_seconds" in
        ''|*[!0-9]*) min_seconds=0 ;;
    esac

    local last_success
    last_success="$(cat "$LAST_SUCCESS_FILE" 2>/dev/null || true)"
    case "$last_success" in
        ''|*[!0-9]*) last_success="$(read_metric iboot_svid_refresh_last_success_timestamp_seconds)" ;;
    esac
    case "$last_success" in
        ''|*[!0-9]*) last_success=0 ;;
    esac

    if [ "$check_rc" -eq 0 ]; then
        log "check ok min_remaining_seconds=$min_seconds"
        printf '%s\n' "$(date +%s)" > "$LAST_SUCCESS_FILE"
        write_metrics 0 "$min_seconds" "$(date +%s)"
        say "SVID 检查通过，未刷新"
        return 0
    fi

    local target
    target="$(resolve_target)"
    log "check failed; refreshing target=$target"
    if refresh_target "$target" >>"$LOG_FILE" 2>&1; then
        # 刷新后重新检查，避免“脚本成功但证书仍缺失”被误记为成功。
        local after_rc=0 after_out after_min
        after_out="$("$SPIRE_SVID_SYNC" --check --threshold-days "$THRESHOLD_DAYS" 2>&1)" || after_rc=$?
        after_min="$(printf '%s\n' "$after_out" | sed -n 's/^min_remaining_seconds=//p' | tail -1)"
        case "$after_min" in
            ''|*[!0-9]*) after_min="$min_seconds" ;;
        esac
        if [ "$after_rc" -ne 0 ]; then
            log "refresh finished but check still failing"
            write_metrics 1 "$after_min" "$last_success"
            say "刷新后检查仍失败，详见 $LOG_FILE" >&2
            return 1
        fi
        log "refresh ok target=$target min_remaining_seconds=$after_min"
        printf '%s\n' "$(date +%s)" > "$LAST_SUCCESS_FILE"
        write_metrics 0 "$after_min" "$(date +%s)"
        say "SVID 刷新完成（target=$target）"
        return 0
    fi
    log "refresh failed target=$target"
    write_metrics 1 "$min_seconds" "$last_success"
    say "SVID 刷新失败，详见 $LOG_FILE" >&2
    return 1
}

case "$MODE" in
    print)
        print_schedule "$(resolve_scheduler)"
        ;;
    install)
        chosen="$(resolve_scheduler)"
        case "$chosen" in
            systemd)
                if ! install_systemd; then
                    if [ "$SCHEDULER" = "auto" ]; then
                        say "systemd user manager 不可用，回退 cron" >&2
                        install_cron
                    else
                        die "systemd user timer 安装失败"
                    fi
                fi
                ;;
            cron)
                install_cron
                ;;
        esac
        ;;
    uninstall)
        removed=0
        if uninstall_systemd; then removed=1; fi
        if uninstall_cron; then removed=1; fi
        if [ "$removed" -eq 0 ]; then
            say "未发现本项目的调度"
        fi
        ;;
    status)
        found=0
        if [ -f "$SYSTEMD_USER_DIR/$UNIT.timer" ]; then
            found=1
            printf 'scheduler=systemd\n'
            printf 'service=%s/%s.service\n' "$SYSTEMD_USER_DIR" "$UNIT"
            printf 'timer=%s/%s.timer\n' "$SYSTEMD_USER_DIR" "$UNIT"
            if systemd_available; then
                "$SYSTEMCTL" --user list-timers "$UNIT.timer" --no-pager 2>/dev/null || true
            fi
        fi
        if cron_installed; then
            found=1
            printf 'scheduler=cron\n'
            read_crontab | awk -v begin="$MARKER_BEGIN" -v end="$MARKER_END" '
                $0 == begin { inblock = 1 }
                inblock     { print }
                $0 == end   { exit }
            '
        fi
        if [ "$found" -eq 0 ]; then
            printf 'scheduler=none\n'
            exit 1
        fi
        ;;
    run-now)
        run_now
        ;;
esac
