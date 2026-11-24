#!/usr/bin/env bash
# Nacos 配置命名空间隔离回归：每服务只能读写自己的命名空间，public 保留给服务发现。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

CA="${NACOS_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
BASE="${NACOS_BASE:-https://localhost:8848}"
CURL=(curl -sS --max-time 5 --cacert "$CA")

login() {
    "${CURL[@]}" -X POST "$BASE/nacos/v3/auth/user/login" \
        --data-urlencode "username=$1" --data-urlencode "password=$2" \
        | python3 -c 'import json,sys;print(json.load(sys.stdin).get("accessToken",""))' 2>/dev/null || true
}

probe() {
    local token="$1" ns="$2"
    "${CURL[@]}" -o /dev/null -w '%{http_code}' \
        -H "Authorization: Bearer $token" \
        "$BASE/nacos/v3/client/cs/config?dataId=__iboot_probe__&groupName=DEFAULT_GROUP&namespaceId=$ns" || true
}

if ! "${CURL[@]}" -o /dev/null "$BASE/nacos/" 2>/dev/null; then
    reg_log "  [SKIP] Nacos 未运行"
    reg_summary
    exit "$REG_FAILED"
fi

admin="$(login nacos "${NACOS_ADMIN_PASSWORD:-}")"
if [ -z "$admin" ]; then
    reg_fail "Nacos 管理员登录失败"
    reg_summary
    exit "$REG_FAILED"
fi
for svc in system file; do
    if "${CURL[@]}" -H "Authorization: Bearer $admin" "$BASE/nacos/v3/admin/core/namespace/list" \
            | grep -q "\"namespace\":\"iboot-$svc\""; then
        reg_pass "命名空间存在：iboot-$svc"
    else
        reg_fail "命名空间缺失：iboot-$svc"
    fi
done

check_account() {
    local user="$1" pw="$2" own="$3" other="$4" token own_code other_code public_code
    token="$(login "$user" "$pw")"
    [ -n "$token" ] || { reg_fail "$user 登录失败"; return; }
    own_code="$(probe "$token" "$own")"
    other_code="$(probe "$token" "$other")"
    public_code="$(probe "$token" public)"
    [ "$own_code" = "200" ] && reg_pass "$user 可读自己的命名空间 $own" \
        || reg_fail "$user 无法读取 $own（HTTP $own_code）"
    [ "$other_code" = "403" ] && reg_pass "$user 被拒绝访问 $other" \
        || reg_fail "$user 未被拒绝访问 $other（HTTP $other_code）"
    [ "$public_code" = "200" ] && reg_pass "$user 可读 public（服务发现）" \
        || reg_fail "$user 无法读取 public（HTTP $public_code）"
}

check_account iboot_system "${NACOS_SYSTEM_PASSWORD:-}" iboot-system iboot-file
check_account iboot_file "${NACOS_FILE_PASSWORD:-}" iboot-file iboot-system

reg_summary
exit "$REG_FAILED"
