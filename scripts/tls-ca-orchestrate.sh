#!/usr/bin/env bash
# 跨主机 TLS/SVID 调度安装与 CA 阶段编排入口。
#
# 默认只打印将执行的动作（dry-run）；显式 --execute 才通过 SSH 执行。
# CA 阶段必须按 prepare → activate → finalize 顺序推进，且每个阶段先在全部主机完成，
# 再进入下一阶段，避免新旧 CA 交错。
#
# 用法：
#   scripts/tls-ca-orchestrate.sh --hosts h1,h2 --checklist
#   scripts/tls-ca-orchestrate.sh --hosts h1,h2 --install-schedules [--execute]
#   scripts/tls-ca-orchestrate.sh --hosts h1,h2 --status [--execute]
#   scripts/tls-ca-orchestrate.sh --hosts h1,h2 --stage prepare|activate|finalize [--restart rolling] [--execute]
#
# 选项：
#   --remote-root PATH   远端仓库路径，默认与本机相同
#   --ssh-opts "..."     SSH 额外参数，默认 "-o BatchMode=yes"

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

HOSTS=""
STAGE=""
RESTART="rolling"
REMOTE_ROOT="$ROOT"
SSH_OPTS="${IBOOT_SSH_OPTS:--o BatchMode=yes}"
MODE=""
EXECUTE=0

usage() {
    awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

say() { printf '[tls-ca-orchestrate] %s\n' "$*"; }
die() { say "$*" >&2; exit 2; }

while [ "$#" -gt 0 ]; do
    case "$1" in
        --hosts)
            [ "$#" -ge 2 ] || die "--hosts 缺少取值（逗号分隔）"
            HOSTS="$2"
            shift
            ;;
        --hosts=*) HOSTS="${1#*=}" ;;
        --stage)
            [ "$#" -ge 2 ] || die "--stage 缺少取值（prepare|activate|finalize）"
            STAGE="$2"
            shift
            ;;
        --stage=*) STAGE="${1#*=}" ;;
        --restart)
            [ "$#" -ge 2 ] || die "--restart 缺少取值（none|docker|rolling|api|host）"
            RESTART="$2"
            shift
            ;;
        --restart=*) RESTART="${1#*=}" ;;
        --remote-root)
            [ "$#" -ge 2 ] || die "--remote-root 缺少路径"
            REMOTE_ROOT="$2"
            shift
            ;;
        --remote-root=*) REMOTE_ROOT="${1#*=}" ;;
        --ssh-opts)
            [ "$#" -ge 2 ] || die "--ssh-opts 缺少取值"
            SSH_OPTS="$2"
            shift
            ;;
        --ssh-opts=*) SSH_OPTS="${1#*=}" ;;
        --checklist) MODE="checklist" ;;
        --install-schedules) MODE="install-schedules" ;;
        --status) MODE="status" ;;
        --execute) EXECUTE=1 ;;
        -h|--help) usage; exit 0 ;;
        *) die "未知参数：$1" ;;
    esac
    shift
done

[ -n "$HOSTS" ] || die "必须提供 --hosts h1,h2"
[ -n "$MODE" ] || [ -n "$STAGE" ] || { usage >&2; exit 2; }
case "$RESTART" in
    none|docker|rolling|api|host) ;;
    *) die "未知重启范围：$RESTART" ;;
esac
if [ -n "$STAGE" ]; then
    case "$STAGE" in
        prepare|activate|finalize) ;;
        *) die "未知阶段：$STAGE（prepare|activate|finalize）" ;;
    esac
fi

IFS=',' read -r -a HOST_LIST <<< "$HOSTS"
for host in "${HOST_LIST[@]}"; do
    [ -n "$host" ] || die "主机列表包含空值"
done

remote_cmd() {
    printf 'cd %q && %s' "$REMOTE_ROOT" "$1"
}

run_remote() {
    local host="$1" cmd="$2"
    local -a args=()
    # shellcheck disable=SC2206
    args=($SSH_OPTS)
    if [ "$EXECUTE" -ne 1 ]; then
        printf 'ssh %s %s "%s"\n' "$SSH_OPTS" "$host" "$cmd"
        return 0
    fi
    say "$host: $cmd"
    ssh "${args[@]}" "$host" "$cmd"
}

stage_command() {
    local stage="$1"
    remote_cmd "scripts/tls-ca-rotate.sh --$stage --restart=$RESTART"
}

install_commands() {
    printf '%s; %s' \
        "$(remote_cmd "scripts/tls-rotate-schedule.sh --install --restart=$RESTART")" \
        "$(remote_cmd "scripts/svid-refresh-schedule.sh --install --target=auto")"
}

status_commands() {
    remote_cmd "scripts/tls-rotate-schedule.sh --status"
    printf '; '
    remote_cmd "scripts/svid-refresh-schedule.sh --status"
    printf '; '
    remote_cmd "scripts/tls-ca-rotate.sh --status"
}

checklist() {
    cat <<EOF
# iBoot 跨主机 TLS/SVID 编排清单
# 主机：${HOST_LIST[*]}
# 远端仓库：$REMOTE_ROOT
# 重启范围：$RESTART
#
# 0. 预检：每台主机确认调度已安装、证书阶段为 initial/finalized
EOF
    for host in "${HOST_LIST[@]}"; do
        printf 'ssh %s %s "%s"\n' "$SSH_OPTS" "$host" "$(status_commands)"
    done
    cat <<EOF

# 1. 安装调度（每台主机各执行一次）
EOF
    for host in "${HOST_LIST[@]}"; do
        printf 'ssh %s %s "%s"\n' "$SSH_OPTS" "$host" "$(install_commands)"
    done
    cat <<EOF

# 2. prepare：必须先在全部主机完成，再进入 activate
EOF
    for host in "${HOST_LIST[@]}"; do
        printf 'ssh %s %s "%s"\n' "$SSH_OPTS" "$host" "$(stage_command prepare)"
    done
    cat <<EOF

# 3. activate：全部主机完成 prepare 后执行
EOF
    for host in "${HOST_LIST[@]}"; do
        printf 'ssh %s %s "%s"\n' "$SSH_OPTS" "$host" "$(stage_command activate)"
    done
    cat <<EOF

# 4. finalize：全部主机完成 activate 后执行
EOF
    for host in "${HOST_LIST[@]}"; do
        printf 'ssh %s %s "%s"\n' "$SSH_OPTS" "$host" "$(stage_command finalize)"
    done
    cat <<EOF

# 5. 收尾巡检
EOF
    for host in "${HOST_LIST[@]}"; do
        printf 'ssh %s %s "%s"\n' "$SSH_OPTS" "$host" "$(status_commands)"
    done
    cat <<EOF

# 中止：任一阶段失败时，在已完成该阶段的主机执行
#   scripts/tls-ca-rotate.sh --rollback --restart=$RESTART
# 已 finalize 的主机不能回滚，需从备份恢复或重新 prepare。
EOF
}

case "$MODE" in
    checklist)
        checklist
        ;;
    install-schedules)
        for host in "${HOST_LIST[@]}"; do
            run_remote "$host" "$(install_commands)"
        done
        [ "$EXECUTE" -eq 1 ] || say "dry-run：加 --execute 才会通过 SSH 执行"
        ;;
    status)
        failures=0
        for host in "${HOST_LIST[@]}"; do
            if ! run_remote "$host" "$(status_commands)"; then
                failures=1
            fi
        done
        [ "$failures" -eq 0 ] || exit 1
        ;;
esac

if [ -n "$STAGE" ]; then
    for host in "${HOST_LIST[@]}"; do
        run_remote "$host" "$(stage_command "$STAGE")" || {
            say "阶段 $STAGE 在 $host 失败；停止后续主机，按清单执行 rollback" >&2
            exit 1
        }
    done
    [ "$EXECUTE" -eq 1 ] || say "dry-run：加 --execute 才会通过 SSH 执行"
fi
