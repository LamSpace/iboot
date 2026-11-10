#!/usr/bin/env bash
# Seata TCC 额度划转验收：正常划转、重复调用、Try 后取消、空回滚、并发、权限、TC/system 不可用。
#
# 前置条件：
#   - MySQL / Redis / Nacos / Seata Server 已启动；
#   - iboot-auth（8083）、iboot-system（8080）、iboot-workflow（8086）已启动；
#   - system 与 workflow 使用相同的 IBOOT_SYSTEM_INTERNAL_TOKEN；
#   - 验证“Try 后失败取消”时，workflow 必须以 seata-tcc-demo profile 启动：
#       SPRING_PROFILES_ACTIVE=seata-tcc-demo
#   - MySQL 容器名为 iboot-mysql（可覆盖）。
#
# 用法：
#   scripts/regress/seata-tcc.sh
#
# 环境变量：
#   REG_WORKFLOW_BASE   workflow 地址，默认 http://localhost:8086
#   REG_AUTH_BASE       认证服务地址，默认 http://localhost:8083
#   MYSQL_CONTAINER     MySQL 容器名，默认 iboot-mysql
#   SEATA_CONTAINER     Seata 容器名，默认 iboot-seata-server
#   SYSTEM_PID_FILE     system 进程号文件，默认 logs/iboot-system.pid
#   IBOOT_SYSTEM_INTERNAL_TOKEN 内部令牌，默认 iboot-dev-internal-token

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
cd "$ROOT"

# shellcheck source=lib.sh
. "$HERE/lib.sh"

REG_WORKFLOW_BASE="${REG_WORKFLOW_BASE:-http://localhost:8086}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-iboot-mysql}"
SEATA_CONTAINER="${SEATA_CONTAINER:-iboot-seata-server}"
SYSTEM_PID_FILE="${SYSTEM_PID_FILE:-logs/iboot-system.pid}"
INTERNAL_TOKEN="${IBOOT_SYSTEM_INTERNAL_TOKEN:-iboot-dev-internal-token}"
OTHER_USER="${OTHER_USER:-regress_tcc_user}"
OTHER_PASS="${OTHER_PASS:-Regress123}"
ADMIN_TOKEN=""
OTHER_TOKEN=""
OTHER_ID=""

sql() {
    docker exec "$MYSQL_CONTAINER" mysql -uroot -proot iboot -N -B -e "$1" 2>/dev/null | tr -d '\r'
}

quota_field() {
    sql "SELECT $3 FROM sys_user_leave_quota WHERE user_id=$1 AND \`year\`=$2"
}

transfer_status() {
    sql "SELECT status FROM sys_leave_quota_transfer WHERE transfer_id='$1'"
}

undo_rows() {
    sql "SELECT COUNT(*) FROM undo_log"
}

expect_eq() {
    local desc="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        reg_pass "$desc"
    else
        reg_fail "$desc（期望 $expected，实际 ${actual:-空}）"
    fi
}

set_quota() {
    local user="$1" year="$2" total="$3" used="$4"
    sql "INSERT INTO sys_user_leave_quota (user_id, \`year\`, total_count, used_count, frozen_count)
         VALUES ($user, $year, $total, $used, 0)
         ON DUPLICATE KEY UPDATE total_count=$total, used_count=$used, frozen_count=0" >/dev/null
}

transfer() {
    local token="$1" count="$2" extra_header="${3:-}"
    local args=(-sS -X POST -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
    if [ -n "$extra_header" ]; then args+=(-H "$extra_header"); fi
    curl "${args[@]}" -d "{\"fromUserId\":1,\"toUserId\":$OTHER_ID,\"year\":2026,\"count\":$count}" \
        "$REG_WORKFLOW_BASE/api/approval/leave/quota/transfer" | reg_unwrap
}

fault_profile_active() {
    local body
    body="$(curl -sS --max-time 5 "$REG_WORKFLOW_BASE/actuator/beans" 2>/dev/null)" || return 1
    printf '%s' "$body" | jq -e '.. | objects | keys? | select(index("seataTccDemoFaultAspect"))' >/dev/null 2>&1
}

wait_seata_healthy() {
    local i status
    for i in $(seq 1 40); do
        status="$(docker inspect -f '{{.State.Health.Status}}' "$SEATA_CONTAINER" 2>/dev/null)"
        [ "$status" = "healthy" ] && return 0
        sleep 2
    done
    return 1
}

# ---------- 前置：登录与目标用户 ----------
reg_log "== 0. 前置 =="
reg_login
ADMIN_TOKEN="$REG_TOKEN"
reg_ensure_user "$OTHER_USER" "TCC 划转目标" 2
OTHER_ID="$REG_LAST_ID"
reg_login "$OTHER_USER" "$OTHER_PASS"
OTHER_TOKEN="$REG_TOKEN"
reg_pass "管理员与目标用户登录成功（目标用户 id=$OTHER_ID）"

# ---------- 1. 正常划转 ----------
reg_log "== 1. 正常划转 =="
set_quota 1 2026 10 0
set_quota "$OTHER_ID" 2026 10 0
normal_body="$(transfer "$ADMIN_TOKEN" 2)"
reg_expect_ok "划转返回成功" "$normal_body"
normal_id="$(printf '%s' "$normal_body" | jq -r '.data.transferId // empty')"
sleep 2
expect_eq "划出方总额度减少 2" "8" "$(quota_field 1 2026 total_count)"
expect_eq "划出方冻结已释放" "0" "$(quota_field 1 2026 frozen_count)"
expect_eq "划入方总额度增加 2" "12" "$(quota_field "$OTHER_ID" 2026 total_count)"
expect_eq "划转记录为 CONFIRMED" "CONFIRMED" "$(transfer_status "$normal_id")"
expect_eq "TCC 事务无 undo_log 残留" "0" "$(undo_rows)"

# ---------- 2. 重复调用 ----------
reg_log "== 2. 重复调用 =="
repeat_body="$(transfer "$ADMIN_TOKEN" 1)"
reg_expect_ok "重复调用返回成功" "$repeat_body"
repeat_id="$(printf '%s' "$repeat_body" | jq -r '.data.transferId // empty')"
sleep 2
if [ "$repeat_id" != "$normal_id" ]; then
    reg_pass "每次调用生成独立划转标识"
else
    reg_fail "重复调用复用了同一划转标识"
fi
expect_eq "重复调用累计扣减 3" "7" "$(quota_field 1 2026 total_count)"
expect_eq "重复调用累计增加 3" "13" "$(quota_field "$OTHER_ID" 2026 total_count)"
expect_eq "第二条记录为 CONFIRMED" "CONFIRMED" "$(transfer_status "$repeat_id")"

# ---------- 3. Try 后失败取消 ----------
reg_log "== 3. Try 后失败取消（需要 seata-tcc-demo profile） =="
if fault_profile_active; then
    reg_pass "workflow 已启用 seata-tcc-demo profile"
    rollback_body="$(transfer "$ADMIN_TOKEN" 3 'X-Seata-Demo-Fail: after-tcc-try')"
    reg_expect_jq "故障注入划转返回错误" '.code != 200' "$rollback_body"
    sleep 3
    expect_eq "取消后划出方总额度不变" "7" "$(quota_field 1 2026 total_count)"
    expect_eq "取消后冻结额度释放" "0" "$(quota_field 1 2026 frozen_count)"
    expect_eq "取消后划入方总额度不变" "13" "$(quota_field "$OTHER_ID" 2026 total_count)"
    expect_eq "取消记录为 CANCELLED" "CANCELLED" \
        "$(sql "SELECT status FROM sys_leave_quota_transfer ORDER BY id DESC LIMIT 1")"
else
    reg_fail "workflow 未启用 seata-tcc-demo profile，无法验证取消（请用 SPRING_PROFILES_ACTIVE=seata-tcc-demo 启动）"
fi

# ---------- 4. 额度不足与空回滚 ----------
reg_log "== 4. 额度不足与空回滚 =="
set_quota 1 2026 1 1
insufficient_body="$(transfer "$ADMIN_TOKEN" 1)"
reg_expect_jq "额度不足返回错误" '.code != 200' "$insufficient_body"
sleep 3
expect_eq "额度不足时已用次数不变" "1" "$(quota_field 1 2026 used_count)"
expect_eq "额度不足时冻结为 0" "0" "$(quota_field 1 2026 frozen_count)"
expect_eq "空回滚留下 CANCELLED 墓碑" "CANCELLED" \
    "$(sql "SELECT status FROM sys_leave_quota_transfer ORDER BY id DESC LIMIT 1")"

# ---------- 5. 权限 ----------
reg_log "== 5. 权限 =="
set_quota 1 2026 10 0
forbidden_body="$(transfer "$OTHER_TOKEN" 1)"
reg_expect_jq "无划转权限返回错误" '.code != 200' "$forbidden_body"
expect_eq "无权限时额度不变" "10" "$(quota_field 1 2026 total_count)"

# ---------- 6. 并发划转 ----------
reg_log "== 6. 并发划转 =="
set_quota 1 2026 1 0
set_quota "$OTHER_ID" 2026 10 0
tmp_a="$(mktemp)"
tmp_b="$(mktemp)"
transfer "$ADMIN_TOKEN" 1 >"$tmp_a" &
pid_a=$!
transfer "$ADMIN_TOKEN" 1 >"$tmp_b" &
pid_b=$!
wait "$pid_a" "$pid_b"
successes=0
for body in "$(cat "$tmp_a")" "$(cat "$tmp_b")"; do
    if [ "$(printf '%s' "$body" | jq -r '.code // 0')" = "200" ]; then
        successes=$((successes + 1))
    fi
done
sleep 2
if [ "$successes" -eq 1 ]; then
    reg_pass "并发划转只有一个成功"
else
    reg_fail "并发划转成功数异常（$successes）"
fi
expect_eq "并发后划出方总额度为 0" "0" "$(quota_field 1 2026 total_count)"
expect_eq "并发后冻结为 0" "0" "$(quota_field 1 2026 frozen_count)"
expect_eq "并发后划入方总额度为 11" "11" "$(quota_field "$OTHER_ID" 2026 total_count)"
rm -f "$tmp_a" "$tmp_b"

# ---------- 7. TC 不可用 ----------
reg_log "== 7. TC 不可用 =="
if docker inspect "$SEATA_CONTAINER" >/dev/null 2>&1; then
    set_quota 1 2026 10 0
    docker stop "$SEATA_CONTAINER" >/dev/null 2>&1
    tc_body="$(transfer "$ADMIN_TOKEN" 1)"
    reg_expect_jq "TC 不可用时划转失败" '.code != 200' "$tc_body"
    expect_eq "TC 不可用时额度不变" "10" "$(quota_field 1 2026 total_count)"
    docker start "$SEATA_CONTAINER" >/dev/null 2>&1
    if wait_seata_healthy; then
        reg_pass "Seata Server 已恢复"
    else
        reg_fail "Seata Server 未在预期时间内恢复"
    fi
    sleep 3
else
    reg_log "  跳过：未找到容器 $SEATA_CONTAINER"
fi

# ---------- 8. 系统服务不可用 ----------
reg_log "== 8. 系统服务不可用 =="
set_quota 1 2026 10 0
SYSTEM_STATE="none"
if docker inspect iboot-system >/dev/null 2>&1; then
    docker pause iboot-system >/dev/null 2>&1
    SYSTEM_STATE="docker"
elif [ -f "$SYSTEM_PID_FILE" ]; then
    kill -STOP "$(cat "$SYSTEM_PID_FILE")" 2>/dev/null
    SYSTEM_STATE="pid"
fi
if [ "$SYSTEM_STATE" != "none" ]; then
    system_body="$(transfer "$ADMIN_TOKEN" 1)"
    reg_expect_jq "系统服务不可用时划转失败" '.code != 200' "$system_body"
    expect_eq "系统服务不可用时额度不变" "10" "$(quota_field 1 2026 total_count)"
    if [ "$SYSTEM_STATE" = "docker" ]; then
        docker unpause iboot-system >/dev/null 2>&1
    else
        kill -CONT "$(cat "$SYSTEM_PID_FILE")" 2>/dev/null
    fi
    reg_pass "系统服务已恢复"
else
    reg_log "  跳过：未找到 iboot-system 容器或 $SYSTEM_PID_FILE"
fi

reg_summary
