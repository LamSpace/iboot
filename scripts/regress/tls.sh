#!/usr/bin/env bash
# TLS 公开入口回归：证书、协议版本、重定向、信任链与 issuer 一致性。
#
# 前置：门户已以 HTTPS 启动（宿主机 Vite 或 Docker 门户 nginx），证书已生成。
# 环境变量：REG_BASE / REG_AUTH_BASE / REG_OIDC_REDIRECT / REG_CACERT / REG_LEAF_CERT。
# 默认来源为宿主机开发 https://localhost:5173；Docker 覆盖为 https://localhost。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

LEAF_CERT="${REG_LEAF_CERT:-$(cd "$(dirname "$REG_CACERT")/.." 2>/dev/null && pwd)/portal.pem}"

# 从公开来源拆出 host:port。
HOSTPORT="${REG_BASE#*://}"
HOSTPORT="${HOSTPORT%%/*}"
HOST="${HOSTPORT%%:*}"
if [ "$HOST" = "$HOSTPORT" ]; then
    PORT=443
else
    PORT="${HOSTPORT##*:}"
fi
CONNECT="$HOST:$PORT"

EMPTY_CA="$(mktemp)"
INVALID_CA_FAILED=0
cleanup() { rm -f "$EMPTY_CA"; }
trap cleanup EXIT

reg_log "== 1. 证书与信任链 =="
if [ -n "${REG_CACERT:-}" ] && [ -f "$REG_CACERT" ]; then
    reg_pass "本地 CA 存在：$REG_CACERT"
else
    reg_fail "本地 CA 不存在：${REG_CACERT:-<空>}（执行 scripts/tls-init.sh）"
fi

if [ -f "$LEAF_CERT" ]; then
    if openssl x509 -checkend 2592000 -noout -in "$LEAF_CERT" >/dev/null 2>&1; then
        reg_pass "门户证书剩余有效期不少于 30 天"
    else
        reg_fail "门户证书剩余有效期不足 30 天：$LEAF_CERT（执行 scripts/tls-init.sh --force）"
    fi
else
    reg_fail "门户证书不存在：$LEAF_CERT"
fi

if reg_curl -o /dev/null "$REG_BASE/" 2>/dev/null; then
    reg_pass "使用本地 CA 可访问 HTTPS 公开入口"
else
    reg_fail "使用本地 CA 无法访问 HTTPS 公开入口：$REG_BASE"
fi

# 用空 CA 强制失败，避免系统信任库已安装本地 CA 时误判通过。
status="$(curl -sS -o /dev/null -w '%{http_code}' --cacert "$EMPTY_CA" "$REG_BASE/" 2>/dev/null || true)"
if [ "$status" = "000" ] || [ -z "$status" ]; then
    reg_pass "不带本地 CA 时证书校验失败"
else
    reg_fail "不带本地 CA 仍可访问：HTTP $status"
fi

reg_log "== 2. 协议版本 =="
if echo | timeout 10 openssl s_client -connect "$CONNECT" -tls1_2 -CAfile "$REG_CACERT" >/dev/null 2>&1; then
    reg_pass "TLS 1.2 可用"
else
    reg_fail "TLS 1.2 握手失败：$CONNECT"
fi

if echo | timeout 10 openssl s_client -connect "$CONNECT" -tls1_3 -CAfile "$REG_CACERT" >/dev/null 2>&1; then
    reg_pass "TLS 1.3 可用"
else
    reg_fail "TLS 1.3 握手失败：$CONNECT"
fi

if echo | timeout 10 openssl s_client -connect "$CONNECT" -tls1_1 -CAfile "$REG_CACERT" >/dev/null 2>&1; then
    reg_fail "TLS 1.1 仍被接受：$CONNECT"
else
    reg_pass "TLS 1.1 被拒绝"
fi

reg_log "== 3. 明文入口 =="
plain_entry="$(curl -sS -o /dev/null -w '%{http_code}' "http://$HOSTPORT/" 2>/dev/null || true)"
case "$plain_entry" in
    301) reg_pass "明文入口 301 到 HTTPS" ;;
    000|"") reg_pass "明文入口无监听（HTTP ${plain_entry:-无响应}）" ;;
    *) reg_fail "明文入口返回 HTTP $plain_entry（期望 301 或无监听）" ;;
esac

plain_token="$(curl -sS -o /dev/null -w '%{http_code}' -X POST "http://$HOSTPORT/oauth2/token" 2>/dev/null || true)"
case "$plain_token" in
    301|000|"") reg_pass "明文入口不能访问令牌端点（HTTP ${plain_token:-无响应}）" ;;
    *) reg_fail "明文令牌端点返回 HTTP $plain_token（期望 301 或无监听）" ;;
esac

reg_log "== 4. issuer 与回调一致性 =="
discovery="$(reg_curl -L "$REG_BASE/.well-known/openid-configuration" 2>/dev/null || true)"
issuer="$(printf '%s' "$discovery" | jq -r '.issuer // empty' 2>/dev/null)"
expected_issuer="${REG_AUTH_BASE%/}"
if [ -n "$issuer" ] && [ "$issuer" = "$expected_issuer" ]; then
    reg_pass "发现文档 issuer 与公开来源一致：$issuer"
else
    reg_fail "issuer 不一致：发现文档=${issuer:-<空>}，期望=$expected_issuer"
fi

case "$REG_OIDC_REDIRECT" in
    "$expected_issuer"/*) reg_pass "OIDC 回调位于公开来源下：$REG_OIDC_REDIRECT" ;;
    *) reg_fail "OIDC 回调不在公开来源下：$REG_OIDC_REDIRECT（期望前缀 $expected_issuer）" ;;
esac

reg_summary
