#!/usr/bin/env bash
# 回归脚本公共函数：登录、带鉴权请求、断言与统计。
#
# 由 smoke.sh / permission-matrix.sh 通过 source 引入，不单独执行。
# 所有地址与账号可用环境变量覆盖：REG_BASE / REG_AI_BASE / REG_USER / REG_PASS。

REG_BASE="${REG_BASE:-http://localhost:8080}"
REG_AI_BASE="${REG_AI_BASE:-http://localhost:8081}"
REG_USER="${REG_USER:-admin}"
REG_PASS="${REG_PASS:-admin123456}"

REG_TOKEN=""
REG_LAST_ID=""
REG_PASSED=0
REG_FAILED=0

reg_log()  { printf '%s\n' "$*"; }
reg_pass() { REG_PASSED=$((REG_PASSED + 1)); printf '  [PASS] %s\n' "$*"; }
reg_fail() { REG_FAILED=$((REG_FAILED + 1)); printf '  [FAIL] %s\n' "$*"; }
reg_die()  { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

# 登录并缓存令牌。
# @param $1 用户名，缺省取 REG_USER
# @param $2 密码，缺省取 REG_PASS
reg_login() {
    local user="${1:-$REG_USER}" pass="${2:-$REG_PASS}" resp token
    resp=$(curl -sS -X POST "$REG_BASE/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$user\",\"password\":\"$pass\"}" | reg_unwrap) || reg_die "登录请求失败"
    token=$(printf '%s' "$resp" | jq -r '.data.token // empty')
    [ -n "$token" ] || reg_die "登录失败（$user）：$resp"
    REG_TOKEN="$token"
}

# 管理端返回体被 CloudEvent 信封包裹（data 字段才是结果），这里统一拆封；
# 非信封响应（例如网关或安全拦截的原始错误）原样返回。
reg_unwrap() {
    local body
    body="$(cat)"
    printf '%s' "$body" | jq -c 'if type == "object" and (.specversion? != null) and (.data? != null) then .data else . end' 2>/dev/null \
        || printf '%s' "$body"
}

reg_get()  { curl -sS -H "Authorization: Bearer $REG_TOKEN" "$REG_BASE$1" | reg_unwrap; }

reg_post() {
    local body="${2:-}"
    [ -n "$body" ] || body='{}'
    curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" -H 'Content-Type: application/json' -d "$body" "$REG_BASE$1" | reg_unwrap
}

reg_put() {
    local body="${2:-}"
    [ -n "$body" ] || body='{}'
    curl -sS -X PUT -H "Authorization: Bearer $REG_TOKEN" -H 'Content-Type: application/json' -d "$body" "$REG_BASE$1" | reg_unwrap
}

reg_del()  { curl -sS -X DELETE -H "Authorization: Bearer $REG_TOKEN" "$REG_BASE$1" | reg_unwrap; }

# 只返回 HTTP 状态码。
reg_status() { curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $REG_TOKEN" "$REG_BASE$1"; }

# 断言业务成功码为 200。
# @param $1 用例描述
# @param $2 响应体
reg_expect_ok() {
    local desc="$1" body="$2" code
    code=$(printf '%s' "$body" | jq -r '.code // empty' 2>/dev/null)
    if [ "$code" = "200" ]; then
        reg_pass "$desc"
    else
        reg_fail "$desc（code=${code:-无}, body=$(printf '%s' "$body" | head -c 200)）"
    fi
}

# 断言 jq 表达式为真。
# @param $1 用例描述
# @param $2 jq 表达式
# @param $3 响应体
reg_expect_jq() {
    local desc="$1" expr="$2" body="$3"
    if printf '%s' "$body" | jq -e "$expr" >/dev/null 2>&1; then
        reg_pass "$desc"
    else
        reg_fail "$desc（表达式: $expr）"
    fi
}

# 断言 HTTP 状态码属于给定集合（空格分隔）。
reg_expect_status() {
    local desc="$1" actual="$2" allowed="$3" s
    for s in $allowed; do
        if [ "$actual" = "$s" ]; then reg_pass "$desc"; return; fi
    done
    reg_fail "$desc（期望 $allowed，实际 $actual）"
}

# 确保存在指定用户名的测试用户，结果写入 REG_LAST_ID。
# 已存在时直接复用，因此可重复执行。
# @param $1 用户名
# @param $2 昵称
# @param $3 角色 ID
reg_ensure_user() {
    local username="$1" nickname="$2" role="$3" list created
    list=$(reg_get "/api/user/list?pageNum=1&pageSize=100&username=$username")
    # 分页接口返回 Result<PageResult<T>>，页对象本身也继承 Result，故数据在 .data.data
    REG_LAST_ID=$(printf '%s' "$list" | jq -r --arg u "$username" '.data.data[]? | select(.username == $u) | .id' | head -1)
    if [ -n "$REG_LAST_ID" ]; then
        return 0
    fi
    created=$(reg_post /api/user "{\"username\":\"$username\",\"password\":\"Regress123\",\"nickname\":\"$nickname\",\"deptId\":1,\"roleIds\":[$role],\"status\":1}")
    REG_LAST_ID=$(printf '%s' "$created" | jq -r '.data.id // empty')
    [ -n "$REG_LAST_ID" ] || reg_die "创建用户 $username 失败：$created"
}

# 汇总结果；存在失败时以非零退出。
reg_summary() {
    printf '\n通过 %d 项，失败 %d 项\n' "$REG_PASSED" "$REG_FAILED"
    [ "$REG_FAILED" -eq 0 ]
}
