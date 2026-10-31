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
FILE_LIST_MENU_ID=500   # 菜单：文件管理（权限码 file:list）
ROLE_ID=""
USER_ID=""
FILE_ROLE_ID=""
FILE_USER_ID=""
REG_GATEWAY="${REG_GATEWAY:-http://localhost:8088}"

cleanup() {
    # 清理必须在管理员身份下进行
    reg_login >/dev/null 2>&1 || return 0
    if [ -n "$USER_ID" ]; then reg_del "/api/user/$USER_ID" >/dev/null 2>&1 || true; fi
    if [ -n "$ROLE_ID" ]; then reg_del "/api/role/$ROLE_ID" >/dev/null 2>&1 || true; fi
    if [ -n "$FILE_USER_ID" ]; then reg_del "/api/user/$FILE_USER_ID" >/dev/null 2>&1 || true; fi
    if [ -n "$FILE_ROLE_ID" ]; then reg_del "/api/role/$FILE_ROLE_ID" >/dev/null 2>&1 || true; fi
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
file_denied=$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/file/list?pageNum=1&pageSize=10")
reg_expect_status "[功能权限][拒绝] 受限用户访问文件列表（无 file:list）" "$file_denied" "401 403"

reg_log "== 文件域：功能权限与数据范围（经网关） =="
reg_login
# 种子文件：数据范围对比需要「超管能看到、受限用户看不到」的文件。
# 冒烟会删除自己上传的文件，若依赖它，全新数据卷上两侧都会是 0。
seed_file="$(mktemp --suffix=.txt)"
echo "iBoot 数据范围种子 $(date +%s)" > "$seed_file"
curl -sS -H "Authorization: Bearer $REG_TOKEN" \
    -F "file=@$seed_file" -F "fileCategory=regress-seed" \
    "$REG_GATEWAY/api/file/upload" > /dev/null || true
rm -f "$seed_file"
admin_files=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/file/list?pageNum=1&pageSize=10" | reg_unwrap)
reg_expect_ok "[功能权限][允许] 超管查询文件列表" "$admin_files"
admin_file_total=$(printf '%s' "$admin_files" | jq -r '.data.total // 0')

FILE_ROLE_CODE="ROLE_FILE_TEST"
file_roles=$(reg_get /api/role/all)
FILE_ROLE_ID=$(printf '%s' "$file_roles" | jq -r --arg c "$FILE_ROLE_CODE" '.data[]? | select(.roleCode == $c) | .id' | head -1)
if [ -z "$FILE_ROLE_ID" ]; then
    created=$(reg_post /api/role "{\"roleCode\":\"$FILE_ROLE_CODE\",\"roleName\":\"回归文件角色\",\"orderNum\":98,\"dataScope\":5,\"status\":1,\"menuIds\":[$FILE_LIST_MENU_ID]}")
    FILE_ROLE_ID=$(printf '%s' "$created" | jq -r '.data.id // empty')
    [ -n "$FILE_ROLE_ID" ] || reg_die "创建文件角色失败：$created"
fi
FILE_USER_NAME="regress_file"
users=$(reg_get "/api/user/list?pageNum=1&pageSize=100&username=$FILE_USER_NAME")
FILE_USER_ID=$(printf '%s' "$users" | jq -r --arg u "$FILE_USER_NAME" '.data.data[]? | select(.username == $u) | .id' | head -1)
if [ -z "$FILE_USER_ID" ]; then
    created=$(reg_post /api/user "{\"username\":\"$FILE_USER_NAME\",\"password\":\"$USER_PASS\",\"nickname\":\"回归文件用户\",\"deptId\":1,\"roleIds\":[$FILE_ROLE_ID],\"status\":1}")
    FILE_USER_ID=$(printf '%s' "$created" | jq -r '.data.id // empty')
    [ -n "$FILE_USER_ID" ] || reg_die "创建文件用户失败：$created"
fi

reg_login "$FILE_USER_NAME" "$USER_PASS"
scope_files=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/file/list?pageNum=1&pageSize=10" | reg_unwrap)
reg_expect_ok "[功能权限][允许] 持有 file:list 的受限用户可访问文件列表" "$scope_files"
scope_file_total=$(printf '%s' "$scope_files" | jq -r '.data.total // 0')
reg_expect_jq "[数据范围][收紧] 仅本人可见时无法看到他人上传的文件" '(.data.total // 0) == 0' "$scope_files"
if [ "$admin_file_total" -gt "$scope_file_total" ]; then
    reg_pass "[数据范围][对比] 超管可见 $admin_file_total 个文件，受限用户可见 $scope_file_total 个"
else
    reg_fail "[数据范围][对比] 超管($admin_file_total) 未多于受限用户($scope_file_total)，文件数据范围可能未生效"
fi

status=$(curl -sS -o /dev/null -w '%{http_code}' "$REG_BASE/api/user/list?pageNum=1&pageSize=10")
reg_expect_status "[功能权限][拒绝] 未携带令牌访问用户列表" "$status" "401 403"

reg_log "== 清理 =="
reg_login
reg_pass "矩阵执行完毕"

reg_summary
