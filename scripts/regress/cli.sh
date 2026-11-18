#!/usr/bin/env bash
# 开发者 CLI 端到端回归。
#
# 覆盖：浏览器授权码登录、user list 与网关数据一致、数据范围、403 退出码、
# 运维写命令（任务/备份/在线强退）、context 增删、--dry-run、logout 撤销与凭据权限，
# 以及个人访问令牌（签发、标准输入注入、在线名单隔离、撤销、PAT 不能管理 PAT、
# 用户停用后失效与重新启用不恢复）。
#
# 前置：九个业务服务与网关已启动；CLI jar 已构建（mvn -pl iboot-cli package）。
# 环境变量：REG_BASE、REG_GATEWAY、REG_USER、REG_PASS、IBOOT_CLI。
# Docker 全量部署时门户回调为 http://localhost/callback，需设置
# REG_OIDC_REDIRECT=http://localhost/callback；宿主机开发默认
# http://localhost:5173/callback。

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

REG_GATEWAY="${REG_GATEWAY:-http://localhost:8088}"
CLI="${IBOOT_CLI:-$ROOT/bin/iboot}"
CLI_CONFIG_DIR="$(mktemp -d)"
CONFIG_TEST_DIR="$(mktemp -d)"
REG_CLI_ROLE_ID=""
REG_CLI_USER_ID=""
REG_CLI_JOB_ID=""
CLI_COOKIE_JAR="$CLI_CONFIG_DIR/cookies.jar"
CLI_LOGIN_LOG="$CLI_CONFIG_DIR/login.log"
USER_NAME="regress_cli"
USER_PASS="Regress123"
ROLE_CODE="ROLE_CLI_TEST"

export IBOOT_CONFIG_DIR="$CLI_CONFIG_DIR"

cleanup() {
    reg_login >/dev/null 2>&1 || true
    if [ -n "$REG_CLI_JOB_ID" ]; then
        curl -sS -X DELETE -H "Authorization: Bearer $REG_TOKEN" \
            "$REG_GATEWAY/api/job/$REG_CLI_JOB_ID" >/dev/null 2>&1 || true
    fi
    if [ -n "$REG_CLI_USER_ID" ]; then reg_del "/api/user/$REG_CLI_USER_ID" >/dev/null 2>&1 || true; fi
    if [ -n "$REG_CLI_ROLE_ID" ]; then reg_del "/api/role/$REG_CLI_ROLE_ID" >/dev/null 2>&1 || true; fi
    rm -rf "$CLI_CONFIG_DIR" "$CONFIG_TEST_DIR"
}
trap cleanup EXIT

[ -f "$CLI" ] || reg_die "找不到 CLI：$CLI；请先执行 mvn -pl iboot-cli package"

# 执行 CLI；源码检出未保留可执行位时回退到 bash。
cli_exec() {
    if [ -x "$CLI" ]; then
        "$CLI" "$@"
    else
        bash "$CLI" "$@"
    fi
}

# 在后台启动 CLI 登录，等待它打印授权地址后，用 curl 完成授权码流程。
#
# @param $1 用户名
# @param $2 密码
cli_login() {
    local user="$1" pass="$2" url="" pid
    : > "$CLI_LOGIN_LOG"
    cli_exec login --no-browser >"$CLI_LOGIN_LOG" 2>&1 &
    pid=$!
    for _ in $(seq 1 100); do
        url="$(sed -n 's/^\(http[^ ]*\)$/\1/p' "$CLI_LOGIN_LOG" | head -1)"
        [ -n "$url" ] && break
        sleep 0.1
    done
    if [ -z "$url" ]; then
        kill "$pid" 2>/dev/null || true
        wait "$pid" 2>/dev/null || true
        reg_die "CLI 未输出授权地址：$(cat "$CLI_LOGIN_LOG")"
    fi
    rm -f "$CLI_COOKIE_JAR"
    curl -sS -c "$CLI_COOKIE_JAR" -b "$CLI_COOKIE_JAR" -o /dev/null "$url" \
        || { kill "$pid" 2>/dev/null || true; reg_die "授权请求失败"; }
    curl -sS -c "$CLI_COOKIE_JAR" -b "$CLI_COOKIE_JAR" -o /dev/null -X POST \
        --data-urlencode "username=$user" --data-urlencode "password=$pass" \
        "$REG_GATEWAY/login" \
        || { kill "$pid" 2>/dev/null || true; reg_die "提交登录表单失败"; }
    curl -sS -c "$CLI_COOKIE_JAR" -b "$CLI_COOKIE_JAR" -L -o /dev/null "$url" \
        || { kill "$pid" 2>/dev/null || true; reg_die "回调 CLI 失败"; }
    wait "$pid" || reg_die "CLI 登录失败：$(cat "$CLI_LOGIN_LOG")"
}

# 运行 CLI 并返回退出码。
#
# @param $@ CLI 参数
cli_run() {
    local errexit=0
    case $- in *e*) errexit=1 ;; esac
    set +e
    CLI_OUTPUT="$(cli_exec "$@" 2>&1)"
    CLI_STATUS=$?
    if [ "$errexit" -eq 1 ]; then set -e; else set +e; fi
    printf '%s\n' "$CLI_OUTPUT"
    return "$CLI_STATUS"
}

reg_log "== 准备：管理员登录与受限用户 =="
reg_login

roles="$(reg_get /api/role/all)"
REG_CLI_ROLE_ID="$(printf '%s' "$roles" | jq -r --arg c "$ROLE_CODE" '.data[]? | select(.roleCode == $c) | .id' | head -1)"
if [ -z "$REG_CLI_ROLE_ID" ]; then
    created="$(reg_post /api/role "{\"roleCode\":\"$ROLE_CODE\",\"roleName\":\"回归 CLI 角色\",\"orderNum\":95,\"dataScope\":5,\"status\":1,\"menuIds\":[100]}")"
    REG_CLI_ROLE_ID="$(printf '%s' "$created" | jq -r '.data.id // empty')"
    [ -n "$REG_CLI_ROLE_ID" ] || reg_die "创建 CLI 角色失败：$created"
fi
users="$(reg_get "/api/user/list?pageNum=1&pageSize=100&username=$USER_NAME")"
REG_CLI_USER_ID="$(printf '%s' "$users" | jq -r --arg u "$USER_NAME" '.data.data[]? | select(.username == $u) | .id' | head -1)"
if [ -z "$REG_CLI_USER_ID" ]; then
    created="$(reg_post /api/user "{\"username\":\"$USER_NAME\",\"password\":\"$USER_PASS\",\"nickname\":\"回归 CLI 用户\",\"deptId\":1,\"roleIds\":[$REG_CLI_ROLE_ID],\"status\":1}")"
    REG_CLI_USER_ID="$(printf '%s' "$created" | jq -r '.data.id // empty')"
    [ -n "$REG_CLI_USER_ID" ] || reg_die "创建 CLI 用户失败：$created"
fi
reg_pass "受限用户已就绪（id=$REG_CLI_USER_ID，数据范围=仅本人，权限 user:list）"

reg_log "== CLI 登录 =="
cli_login "$REG_USER" "$REG_PASS"
reg_pass "浏览器授权码登录完成"

if [ "$(stat -c '%a' "$CLI_CONFIG_DIR/credentials.json" 2>/dev/null || echo '')" = "600" ]; then
    reg_pass "凭据文件权限为 0600"
else
    reg_fail "凭据文件权限不是 0600"
fi

reg_log "== 只读查询与解包 =="
cli_json="$(cli_run user list --json --size 5)" || reg_fail "CLI user list 失败"
api_json="$(reg_get "/api/user/list?pageNum=1&pageSize=5")"
# Web 端与 CLI 对 LocalDateTime 的序列化不同（ISO 字符串 / 六元数组），
# 比较业务字段而不比较日期表示。
cli_projection="$(printf '%s' "$cli_json" | jq -S \
    '{total: (.total // 0), users: [.data[]? | {id, username, status}]}')"
api_projection="$(printf '%s' "$api_json" | jq -S \
    '{total: (.data.total // 0), users: [.data.data[]? | {id, username, status}]}')"
if [ "$cli_projection" = "$api_projection" ]; then
    reg_pass "CLI user list --json 与网关业务数据一致"
else
    reg_fail "CLI user list --json 与网关数据不一致（CLI=$cli_projection API=$api_projection）"
fi

cli_run user list >/dev/null || reg_fail "CLI 默认表格输出失败"
reg_pass "CLI 默认表格输出可用"

# 公共客户端不签发刷新令牌：破坏 access token 后应得到认证失效退出码，
# 而不是尝试续期；验证后恢复原始凭据继续后续用例。
credentials="$CLI_CONFIG_DIR/credentials.json"
backup="$credentials.bak"
cp -p "$credentials" "$backup"
jq '.dev.accessToken = "invalid-token"' "$credentials" > "$credentials.tmp"
chmod 600 "$credentials.tmp"
mv "$credentials.tmp" "$credentials"
set +e
cli_run user list --json --size 1 >/dev/null
invalid_status=$?
set -e
if [ "$invalid_status" -eq 3 ]; then
    reg_pass "无刷新令牌的 401 以认证失效退出"
else
    reg_fail "无刷新令牌的 401 退出码为 $invalid_status，期望 3"
fi
mv "$backup" "$credentials"

reg_log "== 运维写命令：任务 =="
job_body='{"jobName":"CLI回归任务","jobGroup":"DEFAULT","invokeTarget":"sampleTask.noParams","cronExpression":"0 0 3 * * ?","status":0,"remark":"CLI regress"}'
job_created="$(curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" -H 'Content-Type: application/json' \
    -d "$job_body" "$REG_GATEWAY/api/job" | reg_unwrap)"
REG_CLI_JOB_ID="$(printf '%s' "$job_created" | jq -r '.data.id // empty')"
[ -n "$REG_CLI_JOB_ID" ] || reg_die "创建 CLI 回归任务失败：$job_created"
cli_run job run "$REG_CLI_JOB_ID" --dry-run >/dev/null || reg_fail "job run --dry-run 失败"
cli_run job run "$REG_CLI_JOB_ID" --yes >/dev/null || reg_fail "job run --yes 失败"
job_log_found=0
for _ in $(seq 1 15); do
    job_logs="$(cli_run job logs --job-id "$REG_CLI_JOB_ID" --json --size 10)"
    if printf '%s' "$job_logs" | jq -e --arg id "$REG_CLI_JOB_ID" \
        '[.data[]? | select((.jobId|tostring)==$id)] | length > 0' >/dev/null 2>&1; then
        job_log_found=1
        break
    fi
    sleep 1
done
if [ "$job_log_found" -eq 1 ]; then
    reg_pass "job run 后可按 job-id 查到执行日志"
else
    reg_fail "job logs --job-id 未查到执行日志"
fi
curl -sS -X DELETE -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/job/$REG_CLI_JOB_ID" >/dev/null || true
REG_CLI_JOB_ID=""
job_audit_found=0
for _ in $(seq 1 10); do
    audit_logs="$(cli_run log operate --json --size 50)"
    if printf '%s' "$audit_logs" | jq -e \
        '[.data[]? | select((.operUrl // "") | contains("/api/job/"))] | length > 0' >/dev/null 2>&1; then
        job_audit_found=1
        break
    fi
    sleep 1
done
if [ "$job_audit_found" -eq 1 ]; then
    reg_pass "任务写操作进入操作日志"
else
    reg_fail "操作日志未找到任务写操作"
fi

reg_log "== 运维写命令：备份 =="
set +e
backup_output="$(cli_run backup trigger --wait --json --yes --timeout 120 --interval 2)"
backup_code=$?
set -e
if [ "$backup_code" -eq 6 ] && printf '%s' "$backup_output" | grep -q "备份功能未启用"; then
    reg_pass "备份禁用映射为业务错误"
elif [ "$backup_code" -eq 0 ] && printf '%s' "$backup_output" | jq -e '.status == "SUCCESS"' >/dev/null 2>&1; then
    reg_pass "备份触发并等待到 SUCCESS"
else
    reg_fail "备份触发/等待失败（exit=$backup_code, output=$(printf '%s' "$backup_output" | head -c 200)）"
fi

reg_log "== context 增删 =="
config_cli() {
    IBOOT_CONFIG_DIR="$CONFIG_TEST_DIR" cli_exec "$@"
}
config_cli config add staging --base-url https://staging.example.com --use >/dev/null \
    || reg_fail "config add 失败"
config_json="$(config_cli config list --json)"
if printf '%s' "$config_json" | jq -e '.[]? | select(.name=="staging")' >/dev/null 2>&1; then
    reg_pass "config add 写入新 context"
else
    reg_fail "config add 后列表中没有新 context"
fi
set +e
config_cli config remove staging >/dev/null 2>&1
remove_current_status=$?
set -e
if [ "$remove_current_status" -eq 2 ]; then
    reg_pass "拒绝删除当前 context"
else
    reg_fail "删除当前 context 退出码为 $remove_current_status，期望 2"
fi
config_cli config use dev >/dev/null || reg_fail "config use dev 失败"
config_cli config remove staging >/dev/null || reg_fail "config remove 非当前 context 失败"
reg_pass "非当前 context 删除成功"

reg_log "== 数据范围与越权 =="
reg_login "$USER_NAME" "$USER_PASS"
scope_json="$(IBOOT_TOKEN="$REG_TOKEN" "$CLI" user list --json --size 10 2>/dev/null)" || reg_fail "受限用户 CLI 查询失败"
if printf '%s' "$scope_json" | jq -e '(.total // 0) <= 1' >/dev/null 2>&1; then
    reg_pass "受限用户只看到本人数据"
else
    reg_fail "受限用户数据范围未生效：$scope_json"
fi

set +e
IBOOT_TOKEN="$REG_TOKEN" "$CLI" backup list --json >/dev/null 2>&1
denied_status=$?
set -e
if [ "$denied_status" -eq 4 ]; then
    reg_pass "无备份权限时退出码为 4（越权）"
else
    reg_fail "无备份权限时退出码为 $denied_status，期望 4"
fi

reg_log "== 在线会话强退 =="
reg_login "$USER_NAME" "$USER_PASS"
restricted_token="$REG_TOKEN"
online_list="$(cli_run online list --json --size 100)"
token_id="$(printf '%s' "$online_list" | jq -r --arg u "$USER_NAME" \
    '.data[]? | select(.username==$u) | .tokenId' | head -1)"
if [ -n "$token_id" ]; then
    cli_run online kick "$token_id" --yes >/dev/null || reg_fail "online kick 失败"
    kicked_status="$(curl -sS -o /dev/null -w '%{http_code}' \
        -H "Authorization: Bearer $restricted_token" \
        "$REG_GATEWAY/api/user/list?pageNum=1&pageSize=1")"
    if [ "$kicked_status" = "401" ]; then
        reg_pass "online kick 后受限用户令牌被拒绝"
    else
        reg_fail "online kick 后受限用户令牌仍可用（HTTP $kicked_status）"
    fi
    online_audit_found=0
    for _ in $(seq 1 10); do
        online_audit="$(cli_run log operate --json --size 50)"
        if printf '%s' "$online_audit" | jq -e \
            '[.data[]? | select((.operUrl // "") | contains("/api/online/"))] | length > 0' >/dev/null 2>&1; then
            online_audit_found=1
            break
        fi
        sleep 1
    done
    if [ "$online_audit_found" -eq 1 ]; then
        reg_pass "在线强退进入操作日志"
    else
        reg_fail "操作日志未找到在线强退记录"
    fi
else
    reg_fail "在线列表未找到受限用户会话"
fi

reg_log "== 个人访问令牌 =="
reg_login
online_before="$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/online/count" | reg_unwrap | jq -r '.data // 0')"
pat_json="$(cli_run token create regress-pat --expires-days 1 --yes --json)" \
    || reg_fail "token create 失败"
pat_token="$(printf '%s' "$pat_json" | jq -r '.token // empty')"
pat_id="$(printf '%s' "$pat_json" | jq -r '.id // empty')"
if [ -n "$pat_token" ] && [ -n "$pat_id" ]; then
    reg_pass "签发 PAT 并返回一次明文"
else
    reg_fail "签发 PAT 未返回明文或 ID：$(printf '%s' "$pat_json" | head -c 200)"
fi

pat_list="$(cli_run token list --json)"
if printf '%s' "$pat_list" | grep -Fq "$pat_token"; then
    reg_fail "token list 返回了令牌明文"
else
    reg_pass "token list 只返回元数据"
fi

if IBOOT_TOKEN="$pat_token" cli_exec user list --json --size 1 >/dev/null 2>&1; then
    reg_pass "PAT 可调用只读接口"
else
    reg_fail "PAT 调用只读接口失败"
fi

if printf '%s' "$pat_token" | cli_exec user list --json --size 1 --token-stdin >/dev/null 2>&1; then
    reg_pass "PAT 可从标准输入注入"
else
    reg_fail "PAT 标准输入注入失败"
fi

online_after="$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/online/count" | reg_unwrap | jq -r '.data // 0')"
if [ "$online_before" = "$online_after" ]; then
    reg_pass "PAT 不进入在线名单"
else
    reg_fail "PAT 改变了在线数量（$online_before -> $online_after）"
fi

set +e
IBOOT_TOKEN="$pat_token" cli_exec token create not-allowed --yes >/dev/null 2>&1
pat_manage_status=$?
set -e
if [ "$pat_manage_status" -eq 4 ]; then
    reg_pass "PAT 不能签发 PAT（403）"
else
    reg_fail "PAT 签发 PAT 退出码为 $pat_manage_status，期望 4"
fi

pat_audit_found=0
for _ in $(seq 1 10); do
    pat_audit="$(cli_run log operate --json --size 50)"
    if printf '%s' "$pat_audit" | jq -e \
        '[.data[]? | select((.operUrl // "") | contains("/api/auth/tokens"))] | length > 0' \
        >/dev/null 2>&1; then
        pat_audit_found=1
        break
    fi
    sleep 1
done
if [ "$pat_audit_found" -eq 1 ]; then
    reg_pass "PAT 签发进入操作日志"
else
    reg_fail "操作日志未找到 PAT 签发记录"
fi
if printf '%s' "$pat_audit" | grep -Fq "$pat_token"; then
    reg_fail "操作日志包含令牌明文"
else
    reg_pass "操作日志不含令牌明文"
fi

cli_run token revoke "$pat_id" --yes >/dev/null || reg_fail "token revoke 失败"
set +e
IBOOT_TOKEN="$pat_token" cli_exec user list --json --size 1 >/dev/null 2>&1
pat_revoked_status=$?
set -e
if [ "$pat_revoked_status" -eq 3 ]; then
    reg_pass "撤销后 PAT 立即失效"
else
    reg_fail "撤销后 PAT 退出码为 $pat_revoked_status，期望 3"
fi

reg_log "== PAT 与用户停用 =="
reg_login "$USER_NAME" "$USER_PASS"
user_pat_json="$(IBOOT_TOKEN="$REG_TOKEN" cli_exec token create regress-disable \
    --expires-days 1 --yes --json)" || reg_fail "受限用户签发 PAT 失败"
user_pat="$(printf '%s' "$user_pat_json" | jq -r '.token // empty')"
[ -n "$user_pat" ] || reg_fail "受限用户 PAT 未返回明文"

reg_login
disable_resp="$(reg_put "/api/user/changeStatus?id=$REG_CLI_USER_ID&status=0")"
reg_expect_ok "停用用户" "$disable_resp"
set +e
IBOOT_TOKEN="$user_pat" cli_exec user list --json --size 1 >/dev/null 2>&1
disabled_pat_status=$?
set -e
if [ "$disabled_pat_status" -eq 3 ]; then
    reg_pass "停用用户后其 PAT 立即失效"
else
    reg_fail "停用用户后 PAT 退出码为 $disabled_pat_status，期望 3"
fi

reg_put "/api/user/changeStatus?id=$REG_CLI_USER_ID&status=1" >/dev/null || true
set +e
IBOOT_TOKEN="$user_pat" cli_exec user list --json --size 1 >/dev/null 2>&1
reenabled_pat_status=$?
set -e
if [ "$reenabled_pat_status" -eq 3 ]; then
    reg_pass "重新启用不恢复旧 PAT"
else
    reg_fail "重新启用后旧 PAT 退出码为 $reenabled_pat_status，期望 3"
fi

reg_log "== 写保护与登出 =="
cli_run api DELETE /api/user/1 --dry-run >/dev/null || reg_fail "api --dry-run 失败"
reg_pass "api --dry-run 不发送写请求"

reg_login
old_token="$(jq -r '.dev.accessToken // empty' "$CLI_CONFIG_DIR/credentials.json")"
[ -n "$old_token" ] || reg_die "无法读取 CLI 凭据中的访问令牌"
status_output="$(cli_run status)" || reg_fail "CLI status 失败"
if printf '%s' "$status_output" | grep -Fq "$old_token"; then
    reg_fail "CLI status 输出了令牌原文"
else
    reg_pass "CLI status 不输出令牌原文"
fi

cli_run logout >/dev/null || reg_fail "CLI logout 失败"
revoked_status="$(curl -sS -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer $old_token" \
    "$REG_GATEWAY/api/user/list?pageNum=1&pageSize=1")"
if [ "$revoked_status" = "401" ]; then
    reg_pass "logout 后旧令牌被服务端拒绝"
else
    reg_fail "logout 后旧令牌仍可用（HTTP $revoked_status）"
fi

if [ "$REG_FAILED" -eq 0 ]; then
    printf '\nCLI 回归通过：%d 项\n' "$REG_PASSED"
else
    printf '\nCLI 回归失败：%d 项失败，%d 项通过\n' "$REG_FAILED" "$REG_PASSED"
    exit 1
fi
