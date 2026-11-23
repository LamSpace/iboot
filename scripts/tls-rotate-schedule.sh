#!/usr/bin/env bash
# 证书轮换周期调度入口。
#
# 默认不安装任何任务；--install 显式安装，--run-now 只执行一次。
# 定时任务先检查，检查失败才重签；普通启动路径不会调用本脚本。
#
# 用法：
#   scripts/tls-rotate-schedule.sh --install [--restart none|docker|host] [--scheduler auto|systemd|cron] [--at HH:MM]
#   scripts/tls-rotate-schedule.sh --uninstall
#   scripts/tls-rotate-schedule.sh --status
#   scripts/tls-rotate-schedule.sh --run-now [--restart none|docker|host]
#   scripts/tls-rotate-schedule.sh --print [--scheduler auto|systemd|cron] [--restart none|docker|host] [--at HH:MM]

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MODE=""
RESTART="none"
SCHEDULER="auto"
AT="03:30"

UNIT="iboot-tls-rotate"
MARKER_BEGIN="# >>> iboot-tls-rotate >>>"
MARKER_END="# <<< iboot-tls-rotate <<<"

LOG_DIR="${IBOOT_LOG_DIR:-$ROOT/logs}"
LOG_FILE="$LOG_DIR/tls-rotate.log"
STATE_DIR="${IBOOT_STATE_DIR:-${XDG_STATE_HOME:-$HOME/.local/state}/iboot}"
SYSTEMD_USER_DIR="${IBOOT_SYSTEMD_USER_DIR:-${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user}"
SYSTEMCTL="${IBOOT_SYSTEMCTL_BIN:-systemctl}"
CRONTAB="${IBOOT_CRONTAB_BIN:-crontab}"
TLS_ROTATE="${IBOOT_TLS_ROTATE_BIN:-$ROOT/scripts/tls-rotate.sh}"
LOCK_HELD=""

usage() {
    awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

say() { printf '[tls-rotate-schedule] %s\n' "$*"; }
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
        --restart)
            [ "$#" -ge 2 ] || die "--restart 缺少取值（none|docker|host）"
            RESTART="$2"
            shift
            ;;
        --restart=none) RESTART="none" ;;
        --restart=docker) RESTART="docker" ;;
        --restart=host) RESTART="host" ;;
        --scheduler)
            [ "$#" -ge 2 ] || die "--scheduler 缺少取值（auto|systemd|cron）"
            SCHEDULER="$2"
            shift
            ;;
        --scheduler=*) SCHEDULER="${1#*=}" ;;
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
case "$RESTART" in
    none|docker|host) ;;
    *) die "未知重启范围：$RESTART（none|docker|host）" ;;
esac
case "$SCHEDULER" in
    auto|systemd|cron) ;;
    *) die "未知调度器：$SCHEDULER（auto|systemd|cron）" ;;
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

service_content() {
    cat <<EOF
[Unit]
Description=iBoot TLS certificate check and rotation

[Service]
Type=oneshot
WorkingDirectory=$ROOT
ExecStart=$ROOT/scripts/tls-rotate-schedule.sh --run-now --restart=$RESTART
EOF
}

timer_content() {
    cat <<EOF
[Unit]
Description=Daily iBoot TLS certificate check

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
$(cron_time) cd "$ROOT" && "$ROOT/scripts/tls-rotate-schedule.sh" --run-now --restart=$RESTART >> "$LOG_FILE" 2>&1
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
    say "已安装 cron 调度（$AT，restart=$RESTART）"
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
    say "已安装 systemd user timer（$AT，restart=$RESTART）"
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

run_now() {
    [ -x "$TLS_ROTATE" ] || die "轮换脚本不可执行：$TLS_ROTATE"
    mkdir -p "$LOG_DIR" "$STATE_DIR"
    local lock_dir="$STATE_DIR/tls-rotate.lock.d" acquired=0 owner=""
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
        log "run skipped: another rotation holds $lock_dir"
        say "已有轮换在运行，跳过" >&2
        return 1
    fi
    printf '%s\n' "$$" > "$lock_dir/pid"
    LOCK_HELD="$lock_dir"
    trap 'rm -rf -- "$LOCK_HELD" 2>/dev/null || true' EXIT

    log "run start restart=$RESTART"
    if "$TLS_ROTATE" --check >>"$LOG_FILE" 2>&1; then
        log "check ok"
        say "检查通过，未重签"
        return 0
    fi
    log "check failed; rotating"
    if "$TLS_ROTATE" --rotate --restart="$RESTART" >>"$LOG_FILE" 2>&1; then
        log "rotate ok"
        say "重签完成（restart=$RESTART）"
        return 0
    fi
    log "rotate failed"
    say "重签失败，详见 $LOG_FILE" >&2
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
