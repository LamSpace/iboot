#!/usr/bin/env bash
# 证书轮换调度容器入口。
#
# 每天在 TLS_ROTATE_AT（默认 03:30）执行一次检查；检查失败才重签。
# 通过 Docker API 重启使用证书的业务容器，不写宿主机 cron/systemd。
# TLS_ROTATE_RUN_ONCE=1 时只执行一次并退出，供回归测试使用。

set -euo pipefail

AT="${TLS_ROTATE_AT:-03:30}"
RESTART="${TLS_ROTATE_RESTART:-api}"
RUN_ONCE="${TLS_ROTATE_RUN_ONCE:-0}"
SCHEDULE="/workspace/scripts/tls-rotate-schedule.sh"

log() { printf '[tls-rotate] %s\n' "$*"; }

run_once() {
    "$SCHEDULE" --run-now --restart="$RESTART"
}

if [ "$RUN_ONCE" = "1" ]; then
    run_once
    exit 0
fi

log "调度已启动：每天 $AT 检查（restart=$RESTART）"
while true; do
    now="$(date +%s)"
    target="$(date -d "today $AT" +%s)"
    if [ "$target" -le "$now" ]; then
        target="$(date -d "tomorrow $AT" +%s)"
    fi
    sleep "$((target - now))"
    run_once || log "本次检查/轮换失败，等待下一个周期"
done
