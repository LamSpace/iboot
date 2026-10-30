#!/usr/bin/env bash
# 权限边界矩阵：以「角色 × 接口 × 期望结果」验证功能权限与数据范围。
#
# 矩阵同时包含允许与拒绝用例——只测允许路径无法发现越权。
# 脚本会创建专用角色与用户（ROLE_SCOPE_TEST / regress_scope），执行结束后清理。
#
# 用法：permission-matrix.sh
# 环境变量：REG_BASE、REG_USER、REG_PASS

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

ROLE_CODE="ROLE_SCOPE_TEST"
ROLE_NAME="回归数据范围角色"
USER_NAME="regress_scope"
USER_PASS="Regress123"
USER_LIST_MENU_ID=100   # 菜单：用户列表（权限码 user:list）
ROLE_ID=""
USER_ID=""

cleanup() {
    # 清理必须在管理员身份下进行
    reg_login >/dev/null 2>&1 || return 0
    if [ -n "$USER_ID" ]; then reg_del "/api/user/$USER_ID" >/dev/null 2>&1 || true; fi
    if [ -n "$ROLE_ID" ]; then reg_del "/api/role/$ROLE_ID" >/dev/null 2>&1 || true; fi
}
trap cleanup EXIT

reg_log "== 准备：创建受限角色与用户 =="
reg_login

roles=$(reg_get /api/role/all)
ROLE_ID=$(printf '%s' "$roles" | jq -r --arg c "$ROLE_CODE" '.data[]? | select(.roleCode == $c) | .id' | head -1)
if [ -z "$ROLE_ID" ]; then
    created_role=$(reg_post /api/role "{\"roleCode\":\"$ROLE_CODE\",\"roleName\":\"$ROLE_NAME\",\"orderNum\":99,\"dataScope\":5,\"status\":1,\"menuIds\":[$USER_LIST_MENU_ID]}")
    ROLE_ID=$(printf '%s' "$created_role" | jq -r '.data.id // empty')
    [ -n "$ROLE_ID" ] || reg_die "创建受限角色失败：$created_role"
fi

users=$(reg_get "/api/user/list?pageNum=1&pageSize=100&username=$USER_NAME")
USER_ID=$(printf '%s' "$users" | jq -r --arg u "$USER_NAME" '.data.data[]? | select(.username == $u) | .id' | head -1)
if [ -z "$USER_ID" ]; then
    created_user=$(reg_post /api/user "{\"username\":\"$USER_NAME\",\"password\":\"$USER_PASS\",\"nickname\":\"回归范围用户\",\"deptId\":1,\"roleIds\":[$ROLE_ID],\"status\":1}")
    USER_ID=$(printf '%s' "$created_user" | jq -r '.data.id // empty')
    [ -n "$USER_ID" ] || reg_die "创建受限用户失败：$created_user"
fi
reg_pass "受限角色 id=$ROLE_ID，受限用户 id=$USER_ID（数据范围=仅本人）"

reg_log "== 功能权限：超管 =="
admin_list=$(reg_get "/api/user/list?pageNum=1&pageSize=10")
reg_expect_ok "[功能权限][允许] 超管查询用户列表" "$admin_list"
admin_total=$(printf '%s' "$admin_list" | jq -r '.data.total // 0')

reg_log "== 功能权限与数据范围：受限用户 =="
reg_login "$USER_NAME" "$USER_PASS"
scope_list=$(reg_get "/api/user/list?pageNum=1&pageSize=10")
reg_expect_ok "[功能权限][允许] 受限角色持有 user:list 时可访问" "$scope_list"
reg_expect_jq "[数据范围][收紧] 仅本人可见时总数不超过 1" '(.data.total // 0) <= 1' "$scope_list"
if printf '%s' "$scope_list" | jq -e --arg n "$USER_NAME" '[.data.data[]? | select(.username == $n)] | length > 0' >/dev/null 2>&1; then
    reg_pass "[数据范围][收紧] 返回记录中包含自己"
else
    reg_fail "[数据范围][收紧] 返回记录中不含自己"
fi
scope_total=$(printf '%s' "$scope_list" | jq -r '.data.total // 0')

if [ "$admin_total" -gt "$scope_total" ]; then
    reg_pass "[数据范围][对比] 超管可见 $admin_total 条，受限用户可见 $scope_total 条"
else
    reg_fail "[数据范围][对比] 超管($admin_total) 未多于受限用户($scope_total)，数据范围可能未生效"
fi

reg_expect_status "[功能权限][拒绝] 受限用户访问备份记录（无 backup 权限）" "$(reg_status /api/monitor/backup/records)" "401 403"

status=$(curl -sS -o /dev/null -w '%{http_code}' "$REG_BASE/api/user/list?pageNum=1&pageSize=10")
reg_expect_status "[功能权限][拒绝] 未携带令牌访问用户列表" "$status" "401 403"

reg_log "== 清理 =="
reg_login
reg_pass "矩阵执行完毕"

reg_summary
