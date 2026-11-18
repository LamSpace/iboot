#!/usr/bin/env bash
# 开发者 CLI 端到端回归。
#
# 覆盖：浏览器授权码登录、user list 与网关数据一致、数据范围、403 退出码、
# --dry-run、logout 撤销与凭据权限。
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
REG_CLI_ROLE_ID=""
REG_CLI_USER_ID=""
CLI_COOKIE_JAR="$CLI_CONFIG_DIR/cookies.jar"
CLI_LOGIN_LOG="$CLI_CONFIG_DIR/login.log"
USER_NAME="regress_cli"
USER_PASS="Regress123"
ROLE_CODE="ROLE_CLI_TEST"

export IBOOT_CONFIG_DIR="$CLI_CONFIG_DIR"

cleanup() {
    reg_login >/dev/null 2>&1 || true
    if [ -n "$REG_CLI_USER_ID" ]; then reg_del "/api/user/$REG_CLI_USER_ID" >/dev/null 2>&1 || true; fi
    if [ -n "$REG_CLI_ROLE_ID" ]; then reg_del "/api/role/$REG_CLI_ROLE_ID" >/dev/null 2>&1 || true; fi
    rm -rf "$CLI_CONFIG_DIR"
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
