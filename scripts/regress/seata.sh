#!/usr/bin/env bash
# Seata AT 跨服务验收：正常提交、额度不足、后置失败回滚、TC/RM 不可用、并发与幂等。
#
# 前置条件：
#   - MySQL / Redis / Nacos / Seata Server 已启动；
#   - iboot-auth（8083）、iboot-system（8080）、iboot-workflow（8086）已启动；
#   - system 与 workflow 使用相同的 IBOOT_SYSTEM_INTERNAL_TOKEN；
#   - 需要验证“远程扣减后失败回滚”时，workflow 必须以 seata-demo profile 启动：
#       SPRING_PROFILES_ACTIVE=seata-demo
#   - MySQL 容器名为 iboot-mysql（可覆盖）。
#
# 用法：
#   scripts/regress/seata.sh
#
# 环境变量：
#   REG_WORKFLOW_BASE   workflow 地址，默认 http://localhost:8086
#   REG_AUTH_BASE       认证服务地址，默认 http://localhost:8083
#   MYSQL_CONTAINER     MySQL 容器名，默认 iboot-mysql
#   SEATA_CONTAINER     Seata 容器名，默认 iboot-seata-server
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
INTERNAL_TOKEN="${IBOOT_SYSTEM_INTERNAL_TOKEN:-iboot-dev-internal-token}"
APPROVER_USER="${APPROVER_USER:-regress_approver}"
APPROVER_PASS="${APPROVER_PASS:-Regress123}"
ADMIN_TOKEN=""
APPROVER_TOKEN=""
APPROVER_ID=""

sql() {
    docker exec "$MYSQL_CONTAINER" mysql -uroot -proot iboot -N -B -e "$1" 2>/dev/null | tr -d '\r'
}

quota_used() {
    sql "SELECT used_count FROM sys_user_leave_quota WHERE user_id=$1 AND \`year\`=$2"
}

leave_status() {
    sql "SELECT status FROM biz_leave_application WHERE id=$1"
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
    sql "DELETE FROM sys_leave_quota_log WHERE user_id=$user" >/dev/null
}

wf_post() {
    local path="$1" token="$2" body="${3:-}" extra_header="${4:-}"
    local args=(-sS -X POST -H "Authorization: Bearer $token" -H 'Content-Type: application/json')
    if [ -n "$extra_header" ]; then args+=(-H "$extra_header"); fi
    curl "${args[@]}" -d "$body" "$REG_WORKFLOW_BASE$path" | reg_unwrap
}

submit_leave() {
    local reason="$1" date="$2"
    local body="{\"approverId\":$APPROVER_ID,\"leaveType\":\"ANNUAL\",\"startTime\":\"${date}T09:00:00\",\"endTime\":\"${date}T18:00:00\",\"reason\":\"$reason\"}"
    wf_post /api/approval/leave "$ADMIN_TOKEN" "$body" | jq -r '.data.id // empty'
}

approve_leave() {
    local id="$1" extra_header="${2:-}"
    wf_post "/api/approval/$id/approve" "$APPROVER_TOKEN" '{"comment":"同意"}' "$extra_header"
}

ensure_approver() {
    REG_TOKEN="$ADMIN_TOKEN"
    reg_ensure_user "$APPROVER_USER" "回归审批人" 2
    APPROVER_ID="$REG_LAST_ID"
    sql "
        INSERT INTO sys_role (role_code, role_name, role_sort, data_scope, status, create_by)
        SELECT 'ROLE_APPROVAL_SMOKE', '回归冒烟审批角色', 96, 1, 1, 'system' FROM DUAL
        WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'ROLE_APPROVAL_SMOKE');
        INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
        SELECT id, 9101 FROM sys_role WHERE role_code = 'ROLE_APPROVAL_SMOKE';
        INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
        SELECT id, 9102 FROM sys_role WHERE role_code = 'ROLE_APPROVAL_SMOKE';
        DELETE FROM sys_user_role WHERE user_id = $APPROVER_ID;
        INSERT INTO sys_user_role (user_id, role_id)
        SELECT $APPROVER_ID, id FROM sys_role WHERE role_code = 'ROLE_APPROVAL_SMOKE';
    " >/dev/null
}

fault_profile_active() {
    local body
    body="$(curl -sS --max-time 5 "$REG_WORKFLOW_BASE/actuator/beans" 2>/dev/null)" || return 1
    printf '%s' "$body" | jq -e '.. | objects | keys? | select(index("seataDemoFaultAspect"))' >/dev/null 2>&1
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

# ---------- 前置：登录与审批人 ----------
reg_log "== 0. 前置 =="
reg_login
ADMIN_TOKEN="$REG_TOKEN"
ensure_approver
reg_login "$APPROVER_USER" "$APPROVER_PASS"
APPROVER_TOKEN="$REG_TOKEN"
reg_pass "管理员与审批人登录成功（审批人 id=$APPROVER_ID）"

# ---------- 1. 正常提交 ----------
reg_log "== 1. 正常提交 =="
set_quota 1 2026 2 0
normal_id="$(submit_leave "seata normal" "2026-11-20")"
normal_body="$(approve_leave "$normal_id")"
reg_expect_ok "审批通过返回成功" "$normal_body"
expect_eq "申请状态为 APPROVED" "APPROVED" "$(leave_status "$normal_id")"
expect_eq "额度已用 1 次" "1" "$(quota_used 1 2026)"
sleep 2
expect_eq "成功后无 undo_log 残留" "0" "$(undo_rows)"

# ---------- 2. 额度不足 ----------
reg_log "== 2. 额度不足 =="
set_quota 1 2026 1 1
insufficient_id="$(submit_leave "seata insufficient" "2026-11-21")"
insufficient_body="$(approve_leave "$insufficient_id")"
reg_expect_jq "额度不足返回错误" '.code != 200' "$insufficient_body"
expect_eq "额度不足时申请仍为 PENDING" "PENDING" "$(leave_status "$insufficient_id")"
expect_eq "额度不足时已用次数不变" "1" "$(quota_used 1 2026)"

# ---------- 3. 远程扣减后失败回滚 ----------
reg_log "== 3. 后置失败回滚（需要 seata-demo profile） =="
if fault_profile_active; then
    reg_pass "workflow 已启用 seata-demo profile"
    set_quota 1 2026 2 0
    rollback_id="$(submit_leave "seata rollback" "2026-11-22")"
    rollback_body="$(approve_leave "$rollback_id" 'X-Seata-Demo-Fail: after-remote')"
    reg_expect_jq "故障注入审批返回错误" '.code != 200' "$rollback_body"
    sleep 2
    expect_eq "回滚后额度恢复" "0" "$(quota_used 1 2026)"
    expect_eq "回滚后申请仍为 PENDING" "PENDING" "$(leave_status "$rollback_id")"
    expect_eq "回滚后无 undo_log 残留" "0" "$(undo_rows)"
else
    reg_fail "workflow 未启用 seata-demo profile，无法验证回滚（请用 SPRING_PROFILES_ACTIVE=seata-demo 启动）"
fi

# ---------- 4. 并发审批 ----------
reg_log "== 4. 并发审批 =="
set_quota 1 2026 3 2
concurrent_a="$(submit_leave "seata concurrent A" "2026-11-23")"
concurrent_b="$(submit_leave "seata concurrent B" "2026-11-24")"
tmp_a="$(mktemp)"
tmp_b="$(mktemp)"
approve_leave "$concurrent_a" >"$tmp_a" &
pid_a=$!
approve_leave "$concurrent_b" >"$tmp_b" &
pid_b=$!
wait "$pid_a" "$pid_b"
status_a="$(leave_status "$concurrent_a")"
status_b="$(leave_status "$concurrent_b")"
if { [ "$status_a" = "APPROVED" ] && [ "$status_b" = "PENDING" ]; } \
    || { [ "$status_a" = "PENDING" ] && [ "$status_b" = "APPROVED" ]; }; then
    reg_pass "并发审批只有一个成功"
else
    reg_fail "并发审批结果异常（A=$status_a, B=$status_b）"
fi
expect_eq "并发后额度只扣 1 次" "3" "$(quota_used 1 2026)"
rm -f "$tmp_a" "$tmp_b"

# ---------- 5. 内部扣减幂等 ----------
reg_log "== 5. 内部扣减幂等 =="
set_quota 2 2026 5 0
idem_request="idem-$(date +%s)"
idem_body_1="$(curl -sS -X POST -H "X-Internal-Token: $INTERNAL_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"requestId\":\"$idem_request\",\"userId\":2,\"year\":2026}" \
    "$REG_BASE/internal/leave/quota/deduct")"
idem_body_2="$(curl -sS -X POST -H "X-Internal-Token: $INTERNAL_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"requestId\":\"$idem_request\",\"userId\":2,\"year\":2026}" \
    "$REG_BASE/internal/leave/quota/deduct")"
reg_expect_ok "首次内部扣减成功" "$idem_body_1"
reg_expect_ok "重复内部扣减返回成功" "$idem_body_2"
expect_eq "重复扣减只生效一次" "1" "$(quota_used 2 2026)"

# ---------- 6. TC 不可用 ----------
reg_log "== 6. TC 不可用 =="
if docker inspect "$SEATA_CONTAINER" >/dev/null 2>&1; then
    tc_id="$(submit_leave "seata tc down" "2026-11-25")"
    docker stop "$SEATA_CONTAINER" >/dev/null 2>&1
    tc_body="$(approve_leave "$tc_id")"
    reg_expect_jq "TC 不可用时审批失败" '.code != 200' "$tc_body"
    expect_eq "TC 不可用时申请仍为 PENDING" "PENDING" "$(leave_status "$tc_id")"
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

# ---------- 7. 系统服务不可用 ----------
reg_log "== 7. 系统服务不可用 =="
system_id="$(submit_leave "seata system down" "2026-11-26")"
SYSTEM_STATE="none"
if docker inspect iboot-system >/dev/null 2>&1; then
    docker pause iboot-system >/dev/null 2>&1
    SYSTEM_STATE="docker"
elif [ -f logs/iboot-system.pid ]; then
    kill -STOP "$(cat logs/iboot-system.pid)" 2>/dev/null
    SYSTEM_STATE="pid"
fi
if [ "$SYSTEM_STATE" != "none" ]; then
    system_body="$(approve_leave "$system_id")"
    reg_expect_jq "系统服务不可用时审批失败" '.code != 200' "$system_body"
    expect_eq "系统服务不可用时申请仍为 PENDING" "PENDING" "$(leave_status "$system_id")"
    if [ "$SYSTEM_STATE" = "docker" ]; then
        docker unpause iboot-system >/dev/null 2>&1
    else
        kill -CONT "$(cat logs/iboot-system.pid)" 2>/dev/null
    fi
    reg_pass "系统服务已恢复"
else
    reg_log "  跳过：未找到 iboot-system 容器或 logs/iboot-system.pid"
fi

reg_summary
