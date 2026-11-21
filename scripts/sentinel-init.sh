#!/usr/bin/env bash
#
# 把 Sentinel 基线规则幂等写入 Nacos。
#
# 用法：
#   scripts/sentinel-init.sh           写入两个基线规则
#   scripts/sentinel-init.sh --verify  写入后回读并校验内容
#
# 说明：
# - Nacos 是规则权威来源；Dashboard 上的临时修改不会写回 Nacos。
# - 规则文件位于 docker/sentinel/rules/，group 固定为 SENTINEL_GROUP。
# - 使用 Nacos 3.x 的 v3 admin 写入接口与 v3 client 回读接口；
#   本地编排通过 NACOS_AUTH_ADMIN_ENABLE=false 关闭 admin API 认证。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NACOS_ADDR="${NACOS_ADDR:-localhost:8848}"
NACOS_SCHEME="${NACOS_SCHEME:-https}"
NACOS_CACERT="${NACOS_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
NACOS_URL="${NACOS_SCHEME}://${NACOS_ADDR}/nacos/v3/admin/cs/config"
NACOS_READ_URL="${NACOS_SCHEME}://${NACOS_ADDR}/nacos/v3/client/cs/config"
NACOS_LOGIN_URL="${NACOS_SCHEME}://${NACOS_ADDR}/nacos/v3/auth/user/login"
NACOS_USERNAME="${NACOS_USERNAME:-}"
NACOS_PASSWORD="${NACOS_PASSWORD:-}"
CURL_OPTS=(-sS)
if [ "$NACOS_SCHEME" = "https" ]; then
    CURL_OPTS+=(--cacert "$NACOS_CACERT")
fi
NAMESPACE="${NACOS_NAMESPACE:-public}"
GROUP="${SENTINEL_GROUP:-SENTINEL_GROUP}"
RULES_DIR="$ROOT/docker/sentinel/rules"
VERIFY=0

[ "${1:-}" = "--verify" ] && VERIFY=1

log()  { printf '%s\n' "$*"; }
fail() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

command -v curl >/dev/null 2>&1 || fail '未找到 curl'
command -v jq >/dev/null 2>&1 || fail '未找到 jq'
[ -d "$RULES_DIR" ] || fail "规则目录不存在：$RULES_DIR"
[ -n "$NACOS_USERNAME" ] || fail '缺少 NACOS_USERNAME'
[ -n "$NACOS_PASSWORD" ] || fail '缺少 NACOS_PASSWORD'

login_resp="$(curl "${CURL_OPTS[@]}" -X POST "$NACOS_LOGIN_URL" \
    --data-urlencode "username=$NACOS_USERNAME" \
    --data-urlencode "password=$NACOS_PASSWORD")" || fail 'Nacos 登录失败'
access_token="$(printf '%s' "$login_resp" | jq -r '.accessToken // empty')"
[ -n "$access_token" ] || fail "Nacos 登录未返回 accessToken：$login_resp"
CURL_OPTS+=(-H "Authorization: Bearer $access_token")

publish() {
    local data_id="$1" file="$2" remote local_content
    [ -f "$file" ] || fail "规则文件不存在：$file"

    curl "${CURL_OPTS[@]}" -f -X POST "$NACOS_URL" \
        --data-urlencode "dataId=$data_id" \
        --data-urlencode "groupName=$GROUP" \
        --data-urlencode "namespaceId=$NAMESPACE" \
        --data-urlencode "content@$file" \
        --data-urlencode "type=json" >/dev/null \
        || fail "写入 Nacos 失败：dataId=$data_id"

    if [ "$VERIFY" -eq 1 ]; then
        local_content="$(cat "$file")"
        remote=''
        for _ in $(seq 1 10); do
            remote="$(curl "${CURL_OPTS[@]}" -f "$NACOS_READ_URL?dataId=$data_id&groupName=$GROUP&namespaceId=$NAMESPACE" \
                | jq -r '.data.content // empty' || true)"
            [ "$remote" = "$local_content" ] && break
            sleep 0.3
        done
        [ "$remote" = "$local_content" ] \
            || fail "回读 Nacos 失败或内容不一致：dataId=$data_id"
    fi
    log "  已写入 $data_id（group=$GROUP, namespace=$NAMESPACE）"
}

log '=== Sentinel 基线规则 ==='
publish 'iboot-gateway-flow-rules' "$RULES_DIR/iboot-gateway-flow-rules.json"
publish 'iboot-notify-flow-rules' "$RULES_DIR/iboot-notify-flow-rules.json"
log 'Sentinel 基线规则就绪'
