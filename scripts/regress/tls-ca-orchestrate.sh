#!/usr/bin/env bash
# 跨主机 CA 编排回归：checklist 顺序、dry-run、执行顺序、失败停止与状态巡检。
#
# 使用假 ssh，不连接真实主机。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

ORCH="$ROOT/scripts/tls-ca-orchestrate.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin"
export FAKE_SSH_LOG="$TMP/ssh.log"
export FAKE_SSH_FAIL_HOST=""

cat > "$TMP/bin/ssh" <<'EOF'
#!/usr/bin/env bash
# 跳过 -o 及其取值，记录 host|command
host=""
cmd=""
while [ "$#" -gt 0 ]; do
    case "$1" in
        -o) shift 2 ;;
        -p|-i) shift 2 ;;
        *) host="$1"; shift; cmd="$*"; break ;;
    esac
done
printf '%s|%s\n' "$host" "$cmd" >> "${FAKE_SSH_LOG:?}"
if [ -n "${FAKE_SSH_FAIL_HOST:-}" ] && [ "$host" = "$FAKE_SSH_FAIL_HOST" ]; then
    exit 1
fi
exit 0
EOF
chmod +x "$TMP/bin/ssh"
export PATH="$TMP/bin:$PATH"

reg_log "== checklist 顺序 =="
cl="$("$ORCH" --hosts host-a,host-b --checklist 2>&1)"
prepare_line="$(printf '%s\n' "$cl" | grep -n 'tls-ca-rotate.sh --prepare' | head -1 | cut -d: -f1)"
activate_line="$(printf '%s\n' "$cl" | grep -n 'tls-ca-rotate.sh --activate' | head -1 | cut -d: -f1)"
finalize_line="$(printf '%s\n' "$cl" | grep -n 'tls-ca-rotate.sh --finalize' | head -1 | cut -d: -f1)"
if [ -n "$prepare_line" ] && [ -n "$activate_line" ] && [ -n "$finalize_line" ] \
        && [ "$prepare_line" -lt "$activate_line" ] && [ "$activate_line" -lt "$finalize_line" ]; then
    reg_pass "checklist 按 prepare → activate → finalize 排列"
else
    reg_fail "checklist 阶段顺序异常"
fi
for host in host-a host-b; do
    for stage in prepare activate finalize; do
        if printf '%s\n' "$cl" | grep -q "$host.*--$stage"; then
            reg_pass "checklist 包含 $host $stage"
        else
            reg_fail "checklist 缺少 $host $stage"
        fi
    done
    if printf '%s\n' "$cl" | grep -q "$host.*tls-rotate-schedule.sh --install"; then
        reg_pass "checklist 包含 $host 轮换调度安装"
    else
        reg_fail "checklist 缺少 $host 轮换调度安装"
    fi
    if printf '%s\n' "$cl" | grep -q "$host.*svid-refresh-schedule.sh --install"; then
        reg_pass "checklist 包含 $host SVID 调度安装"
    else
        reg_fail "checklist 缺少 $host SVID 调度安装"
    fi
done

reg_log "== dry-run 不执行 =="
: > "$FAKE_SSH_LOG"
out="$("$ORCH" --hosts host-a,host-b --stage prepare --restart rolling 2>&1)"
if printf '%s' "$out" | grep -q 'ssh .*host-a' && printf '%s' "$out" | grep -q 'ssh .*host-b'; then
    reg_pass "dry-run 打印两台主机的 ssh 命令"
else
    reg_fail "dry-run 未打印预期命令"
fi
if [ ! -s "$FAKE_SSH_LOG" ]; then
    reg_pass "dry-run 未调用 ssh"
else
    reg_fail "dry-run 实际调用了 ssh"
fi

reg_log "== 执行顺序 =="
: > "$FAKE_SSH_LOG"
"$ORCH" --hosts host-a,host-b --stage prepare --restart rolling --ssh-opts= --execute >/dev/null 2>&1
first="$(sed -n '1p' "$FAKE_SSH_LOG")"
second="$(sed -n '2p' "$FAKE_SSH_LOG")"
if printf '%s' "$first" | grep -q '^host-a|.*--prepare --restart=rolling$'; then
    reg_pass "先对 host-a 执行 prepare"
else
    reg_fail "host-a prepare 命令异常：$first"
fi
if printf '%s' "$second" | grep -q '^host-b|.*--prepare --restart=rolling$'; then
    reg_pass "再对 host-b 执行 prepare"
else
    reg_fail "host-b prepare 命令异常：$second"
fi

reg_log "== 失败停止 =="
: > "$FAKE_SSH_LOG"
export FAKE_SSH_FAIL_HOST=host-b
if "$ORCH" --hosts host-a,host-b --stage activate --ssh-opts= --execute >/dev/null 2>&1; then
    reg_fail "host-b 失败时仍返回成功"
else
    reg_pass "主机失败时返回非零"
fi
if [ "$(wc -l < "$FAKE_SSH_LOG")" -eq 2 ]; then
    reg_pass "失败后停止，不再执行后续主机"
else
    reg_fail "失败后仍继续执行其它主机"
fi
export FAKE_SSH_FAIL_HOST=""

reg_log "== 调度安装与巡检 =="
: > "$FAKE_SSH_LOG"
"$ORCH" --hosts host-a,host-b --install-schedules --ssh-opts= --execute >/dev/null 2>&1
if grep -q 'tls-rotate-schedule.sh --install' "$FAKE_SSH_LOG" \
        && grep -q 'svid-refresh-schedule.sh --install' "$FAKE_SSH_LOG"; then
    reg_pass "调度安装包含轮换与 SVID 两个入口"
else
    reg_fail "调度安装命令不完整"
fi
: > "$FAKE_SSH_LOG"
"$ORCH" --hosts host-a,host-b --status --ssh-opts= --execute >/dev/null 2>&1
if [ "$(wc -l < "$FAKE_SSH_LOG")" -eq 2 ] && grep -q 'tls-ca-rotate.sh --status' "$FAKE_SSH_LOG"; then
    reg_pass "巡检对每台主机汇总三个状态入口"
else
    reg_fail "巡检命令异常"
fi

reg_log "== 参数校验 =="
if ! "$ORCH" --stage prepare >/dev/null 2>&1; then
    reg_pass "缺少 --hosts 时失败"
else
    reg_fail "缺少 --hosts 时仍执行"
fi
if ! "$ORCH" --hosts host-a --stage unknown >/dev/null 2>&1; then
    reg_pass "未知阶段被拒绝"
else
    reg_fail "未知阶段仍被执行"
fi

reg_summary
exit "$REG_FAILED"
