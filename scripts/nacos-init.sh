#!/usr/bin/env sh
# 初始化 Nacos 鉴权账号。
#
# 首次启动：设置内置 nacos 管理员的密码；随后创建/更新服务账号 iboot，
# 绑定角色并授予配置读写权限。脚本可重复执行。
#
# 环境变量：NACOS_ADDR、NACOS_SCHEME、NACOS_CACERT、
#          NACOS_ADMIN_PASSWORD、NACOS_USERNAME、NACOS_PASSWORD

set -eu

NACOS_ADDR="${NACOS_ADDR:-nacos:8848}"
NACOS_SCHEME="${NACOS_SCHEME:-https}"
NACOS_CACERT="${NACOS_CACERT:-/certs/rootCA.pem}"
NACOS_ADMIN_PASSWORD="${NACOS_ADMIN_PASSWORD:?NACOS_ADMIN_PASSWORD 必须在 .env 中设置}"
NACOS_USERNAME="${NACOS_USERNAME:-iboot}"
NACOS_PASSWORD="${NACOS_PASSWORD:?NACOS_PASSWORD 必须在 .env 中设置}"

BASE="${NACOS_SCHEME}://${NACOS_ADDR}"
CURL_OPTS="-sS --max-time 10"
[ "$NACOS_SCHEME" = "https" ] && CURL_OPTS="$CURL_OPTS --cacert $NACOS_CACERT"

fail() {
    printf '[nacos-init] ERROR: %s\n' "$*" >&2
    exit 1
}

# Nacos 官方镜像没有 jq，但自带 python3。
json_get() {
    python3 -c 'import json,sys; d=json.load(sys.stdin); v=d.get(sys.argv[1],""); print("" if v is None else v)' "$1" 2>/dev/null || true
}

# 等待 HTTP 端口可用；readiness 在控制台端口 8080，这里只探测 8848 已接受连接。
i=0
while [ "$i" -lt 60 ]; do
    if curl $CURL_OPTS -o /dev/null "$BASE/nacos/" 2>/dev/null; then
        break
    fi
    i=$((i + 1))
    sleep 2
done
[ "$i" -lt 60 ] || fail "Nacos 未在预期时间内就绪"

# 首次初始化管理员；已初始化时返回 code=409，继续用注入的密码登录。
admin_resp="$(curl $CURL_OPTS -X POST "$BASE/nacos/v3/auth/user/admin" \
    --data-urlencode "password=$NACOS_ADMIN_PASSWORD" || true)"
admin_code="$(printf '%s' "$admin_resp" | json_get code)"
case "$admin_code" in
    0|409) ;;
    *) fail "管理员初始化失败：$admin_resp" ;;
esac

login_resp="$(curl $CURL_OPTS -X POST "$BASE/nacos/v3/auth/user/login" \
    --data-urlencode "username=nacos" \
    --data-urlencode "password=$NACOS_ADMIN_PASSWORD" || true)"
token="$(printf '%s' "$login_resp" | json_get accessToken)"
[ -n "$token" ] || fail "管理员登录失败：$login_resp"

ensure_account() {
    local username="$1" password="$2" resp code svc_resp svc_token probe_status
    resp="$(curl $CURL_OPTS -X POST "$BASE/nacos/v3/auth/user" \
        -H "Authorization: Bearer $token" \
        --data-urlencode "username=$username" \
        --data-urlencode "password=$password" || true)"
    code="$(printf '%s' "$resp" | json_get code)"
    if [ "$code" != "0" ]; then
        curl $CURL_OPTS -X PUT "$BASE/nacos/v3/auth/user" \
            -H "Authorization: Bearer $token" \
            --data-urlencode "username=$username" \
            --data-urlencode "newPassword=$password" >/dev/null \
            || fail "账号口令同步失败（$username）：$resp"
    fi

    # 角色已存在时 POST 会失败；权限授予是幂等的。首次启动必须先建角色再授权。
    curl $CURL_OPTS -X POST "$BASE/nacos/v3/auth/role" \
        -H "Authorization: Bearer $token" \
        --data-urlencode "role=iboot_role" \
        --data-urlencode "username=$username" >/dev/null 2>&1 || true
    curl $CURL_OPTS -X POST "$BASE/nacos/v3/auth/permission" \
        -H "Authorization: Bearer $token" \
        --data-urlencode "role=iboot_role" \
        --data-urlencode "resource=*" \
        --data-urlencode "action=rw" >/dev/null 2>&1 || true

    svc_resp="$(curl $CURL_OPTS -X POST "$BASE/nacos/v3/auth/user/login" \
        --data-urlencode "username=$username" \
        --data-urlencode "password=$password" || true)"
    svc_token="$(printf '%s' "$svc_resp" | json_get accessToken)"
    [ -n "$svc_token" ] || fail "账号登录失败（$username）：$svc_resp"

    probe_status="$(curl $CURL_OPTS -o /dev/null -w '%{http_code}' \
        -H "Authorization: Bearer $svc_token" \
        "$BASE/nacos/v3/client/cs/config?dataId=__iboot_probe__&groupName=DEFAULT_GROUP&namespaceId=public" || true)"
    case "$probe_status" in
        200|404) ;;
        *) fail "账号无权读取配置（$username，HTTP $probe_status）" ;;
    esac
}

# 运维脚本账号；管理脚本使用它，不注入业务服务。
ensure_account "$NACOS_USERNAME" "$NACOS_PASSWORD"

# 每服务账号：账号名 iboot_<service>，口令由 NACOS_<SERVICE>_PASSWORD 注入。
for service in system workflow file notify job monitor auth ai gateway ops; do
    var="NACOS_$(printf '%s' "$service" | tr '[:lower:]' '[:upper:]')_PASSWORD"
    eval "password=\${$var:-}"
    [ -n "$password" ] || fail "缺少 $var"
    ensure_account "iboot_$service" "$password"
done

printf '[nacos-init] 管理员、运维与服务账号已就绪（%s + 10 个服务账号）\n' "$NACOS_USERNAME"
