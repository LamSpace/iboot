#!/usr/bin/env bash
# 回归脚本公共函数：登录、带鉴权请求、断言与统计。
#
# 由 smoke.sh / permission-matrix.sh 通过 source 引入，不单独执行。
# 所有地址与账号可用环境变量覆盖：REG_BASE / REG_AI_BASE / REG_AUTH_BASE / REG_USER / REG_PASS。
# 登录走 REG_AUTH_BASE（认证服务），其余请求走 REG_BASE（管理端）。

REG_BASE="${REG_BASE:-http://localhost:8080}"
REG_AI_BASE="${REG_AI_BASE:-http://localhost:8081}"
# 登录已迁至认证服务；未显式指定时仍走 REG_BASE，便于在拆分前的基线上录制。
REG_AUTH_BASE="${REG_AUTH_BASE:-http://localhost:8083}"
REG_USER="${REG_USER:-admin}"
REG_PASS="${REG_PASS:-admin123456}"

REG_TOKEN=""
REG_ID_TOKEN=""
REG_JAR=""
REG_LAST_ID=""
REG_PASSED=0
REG_FAILED=0

reg_log()  { printf '%s\n' "$*"; }
reg_pass() { REG_PASSED=$((REG_PASSED + 1)); printf '  [PASS] %s\n' "$*"; }
reg_fail() { REG_FAILED=$((REG_FAILED + 1)); printf '  [FAIL] %s\n' "$*"; }
reg_die()  { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

# OIDC 客户端与回调地址，与认证服务登记的客户端保持一致。
REG_OIDC_CLIENT="${REG_OIDC_CLIENT:-iboot-portal}"
REG_OIDC_REDIRECT="${REG_OIDC_REDIRECT:-http://localhost:5173/callback}"

# 登录并缓存令牌。
#
# 走标准授权码 + PKCE：脚本扮演浏览器，依次完成授权请求、提交凭据、取回授权码与兑换令牌。
#
# @param $1 用户名，缺省取 REG_USER
# @param $2 密码，缺省取 REG_PASS
reg_login() {
    local user="${1:-$REG_USER}" pass="${2:-$REG_PASS}"
    local jar state verifier challenge query location code resp
    # 保留 cookie jar：登出用例需要沿用同一个认证服务会话
    jar="$(mktemp)"
    REG_JAR="$jar"
    state="$(openssl rand -hex 8)"
    verifier="$(openssl rand -base64 48 | tr '+/' '-_' | tr -d '=\n')"
    challenge="$(printf '%s' "$verifier" | openssl dgst -sha256 -binary | openssl base64 -A | tr '+/' '-_' | tr -d '=')"
    query="response_type=code&client_id=$REG_OIDC_CLIENT&scope=openid%20profile%20api&state=$state"
    query="$query&redirect_uri=$(reg_urlencode "$REG_OIDC_REDIRECT")&code_challenge=$challenge&code_challenge_method=S256"

    # 1. 发起授权：未登录时会建立会话并跳转到登录页
    curl -sS -c "$jar" -b "$jar" -o /dev/null "$REG_AUTH_BASE/oauth2/authorize?$query" \
        || { rm -f "$jar"; reg_die "授权请求失败"; }
    # 2. 提交登录页表单
    curl -sS -c "$jar" -b "$jar" -o /dev/null -X POST \
        --data-urlencode "username=$user" --data-urlencode "password=$pass" \
        "$REG_AUTH_BASE/login" || { rm -f "$jar"; reg_die "提交登录表单失败"; }
    # 3. 再次请求授权端点，回调地址里带授权码
    location="$(curl -sS -c "$jar" -b "$jar" -o /dev/null -w '%{redirect_url}' \
        "$REG_AUTH_BASE/oauth2/authorize?$query")"
    code="$(printf '%s' "$location" | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p')"
    [ -n "$code" ] || reg_die "登录失败（$user）：未取得授权码（$location）"

    # 4. 授权码换令牌
    resp="$(curl -sS -X POST "$REG_AUTH_BASE/oauth2/token" \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        --data-urlencode "grant_type=authorization_code" \
        --data-urlencode "code=$code" \
        --data-urlencode "redirect_uri=$REG_OIDC_REDIRECT" \
        --data-urlencode "client_id=$REG_OIDC_CLIENT" \
        --data-urlencode "code_verifier=$verifier")" || reg_die "令牌请求失败"
    REG_TOKEN="$(printf '%s' "$resp" | jq -r '.access_token // empty')"
    REG_ID_TOKEN="$(printf '%s' "$resp" | jq -r '.id_token // empty')"
    REG_REFRESH_TOKEN="$(printf '%s' "$resp" | jq -r '.refresh_token // empty')"
    [ -n "$REG_TOKEN" ] || reg_die "登录失败（$user）：$resp"
}

# 旧格式令牌使用的共享密钥，只用于「窗口关闭后应被拒绝」的负向用例。
JWT_LEGACY_SECRET="${JWT_LEGACY_SECRET:-iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits}"

# 生成一枚对称签名（HS256）的旧格式令牌。
#
# 双栈窗口关闭后这种令牌必须被拒绝，用它来验证资源服务不再接受旧协议。
#
# @return 令牌字符串
reg_legacy_token() {
    python3 - "$JWT_LEGACY_SECRET" <<'PY'
import base64
import hashlib
import hmac
import json
import sys
import time

secret = sys.argv[1].encode()


def b64(raw: bytes) -> bytes:
    return base64.urlsafe_b64encode(raw).rstrip(b"=")


header = b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
payload = b64(json.dumps(
    {"userId": 1, "sub": "admin", "exp": int(time.time()) + 600},
    separators=(",", ":"),
).encode())
signing = header + b"." + payload
signature = b64(hmac.new(secret, signing, hashlib.sha256).digest())
print((signing + b"." + signature).decode())
PY
}

# URL 编码。
# @param $1 原始文本
reg_urlencode() {
    printf '%s' "$1" | jq -sRr @uri
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
