#!/usr/bin/env bash
# CA 轮换回归：在临时证书目录执行 prepare → activate → finalize，不触碰运行中栈。
#
# 校验：
#   - prepare 后信任 bundle 含旧+新两张 CA，叶子证书仍由旧 CA 签发且可验证；
#   - activate 后叶子证书由新 CA 签发，bundle 仍含两张且可验证；
#   - finalize 后 bundle 收敛为新 CA，叶子证书可验证。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

LIVE_CA="$ROOT/docker/certs/ca"
if [ ! -f "$LIVE_CA/rootCA.pem" ] || [ ! -f "$LIVE_CA/rootCA-key.pem" ]; then
    reg_log "  [SKIP] 本地 CA 不存在"
    reg_summary
    exit "$REG_FAILED"
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/ca"
# 运行期 bundle 可能已包含 SPIRE CA；临时测试只取第一张（mkcert）作为单 CA 起点。
openssl x509 -in "$LIVE_CA/rootCA.pem" -out "$TMP/ca/rootCA.pem"
cp "$LIVE_CA/rootCA-key.pem" "$TMP/ca/"

if ! IBOOT_TLS_CERT_DIR="$TMP" "$ROOT/scripts/tls-init.sh" --force >/tmp/ca-rotation-init.log 2>&1; then
    reg_fail "临时证书初始化失败，详见 /tmp/ca-rotation-init.log"
    reg_summary
    exit "$REG_FAILED"
fi
reg_pass "临时证书初始化完成"

old_issuer="$(openssl x509 -noout -issuer -in "$TMP/services/iboot-system.pem")"
if IBOOT_TLS_CERT_DIR="$TMP" "$ROOT/scripts/tls-ca-rotate.sh" --prepare --restart none \
        >/tmp/ca-rotation-prepare.log 2>&1; then
    reg_pass "prepare 完成"
else
    reg_fail "prepare 失败，详见 /tmp/ca-rotation-prepare.log"
    reg_summary
    exit "$REG_FAILED"
fi
certs="$(grep -c 'BEGIN CERTIFICATE' "$TMP/ca/rootCA.pem" || true)"
[ "$certs" = "2" ] && reg_pass "prepare 后信任 bundle 含 2 张 CA" \
    || reg_fail "prepare 后信任 bundle 证书数异常：$certs"
openssl verify -CAfile "$TMP/ca/rootCA.pem" "$TMP/services/iboot-system.pem" >/dev/null 2>&1 \
    && reg_pass "prepare 后旧 CA 叶子证书可验证" \
    || reg_fail "prepare 后旧 CA 叶子证书验证失败"
[ "$(openssl x509 -noout -issuer -in "$TMP/services/iboot-system.pem")" = "$old_issuer" ] \
    && reg_pass "prepare 不改变叶子证书签发者" \
    || reg_fail "prepare 意外改变了叶子证书签发者"

if IBOOT_TLS_CERT_DIR="$TMP" "$ROOT/scripts/tls-ca-rotate.sh" --activate --restart none \
        >/tmp/ca-rotation-activate.log 2>&1; then
    reg_pass "activate 完成"
else
    reg_fail "activate 失败，详见 /tmp/ca-rotation-activate.log"
    reg_summary
    exit "$REG_FAILED"
fi
new_issuer="$(openssl x509 -noout -issuer -in "$TMP/services/iboot-system.pem")"
[ "$new_issuer" != "$old_issuer" ] && reg_pass "activate 后叶子证书由新 CA 签发" \
    || reg_fail "activate 后叶子证书签发者未变化"
openssl verify -CAfile "$TMP/ca/rootCA.pem" "$TMP/services/iboot-system.pem" >/dev/null 2>&1 \
    && reg_pass "activate 后新 CA 叶子证书可验证" \
    || reg_fail "activate 后新 CA 叶子证书验证失败"

if IBOOT_TLS_CERT_DIR="$TMP" "$ROOT/scripts/tls-ca-rotate.sh" --finalize --restart none \
        >/tmp/ca-rotation-finalize.log 2>&1; then
    reg_pass "finalize 完成"
else
    reg_fail "finalize 失败，详见 /tmp/ca-rotation-finalize.log"
    reg_summary
    exit "$REG_FAILED"
fi
certs="$(grep -c 'BEGIN CERTIFICATE' "$TMP/ca/rootCA.pem" || true)"
[ "$certs" = "1" ] && reg_pass "finalize 后信任 bundle 只保留新 CA" \
    || reg_fail "finalize 后信任 bundle 证书数异常：$certs"
openssl verify -CAfile "$TMP/ca/rootCA.pem" "$TMP/services/iboot-system.pem" >/dev/null 2>&1 \
    && reg_pass "finalize 后叶子证书可验证" \
    || reg_fail "finalize 后叶子证书验证失败"

reg_log ""
reg_log "== 回滚：prepare → rollback =="
TMP2="$(mktemp -d)"
trap 'rm -rf "$TMP" "${TMP2:-}"' EXIT
mkdir -p "$TMP2/ca"
openssl x509 -in "$LIVE_CA/rootCA.pem" -out "$TMP2/ca/rootCA.pem"
cp "$LIVE_CA/rootCA-key.pem" "$TMP2/ca/"
if ! IBOOT_TLS_CERT_DIR="$TMP2" "$ROOT/scripts/tls-init.sh" --force >/tmp/ca-rollback-init.log 2>&1; then
    reg_fail "回滚测试证书初始化失败，详见 /tmp/ca-rollback-init.log"
    reg_summary
    exit "$REG_FAILED"
fi
original_issuer="$(openssl x509 -noout -issuer -in "$TMP2/services/iboot-system.pem")"

if IBOOT_TLS_CERT_DIR="$TMP2" "$ROOT/scripts/tls-ca-rotate.sh" --prepare --restart none \
        >/tmp/ca-rollback-prepare.log 2>&1; then
    reg_pass "回滚用例 prepare 完成"
else
    reg_fail "回滚用例 prepare 失败，详见 /tmp/ca-rollback-prepare.log"
fi
status_out="$(IBOOT_TLS_CERT_DIR="$TMP2" "$ROOT/scripts/tls-ca-rotate.sh" --status 2>&1)"
printf '%s\n' "$status_out" | grep -q '^rollback=available$' \
    && reg_pass "prepared 状态报告可回滚" \
    || reg_fail "prepared 状态未报告可回滚"
if IBOOT_TLS_CERT_DIR="$TMP2" "$ROOT/scripts/tls-ca-rotate.sh" --rollback --restart none \
        >/tmp/ca-rollback-prepared.log 2>&1; then
    reg_pass "prepared → rollback 完成"
else
    reg_fail "prepared → rollback 失败，详见 /tmp/ca-rollback-prepared.log"
fi
certs="$(grep -c 'BEGIN CERTIFICATE' "$TMP2/ca/rootCA.pem" || true)"
[ "$certs" = "1" ] && reg_pass "prepared 回滚后 bundle 只保留上一张 CA" \
    || reg_fail "prepared 回滚后 bundle 证书数异常：$certs"
[ "$(openssl x509 -noout -issuer -in "$TMP2/services/iboot-system.pem")" = "$original_issuer" ] \
    && reg_pass "prepared 回滚不改变叶子证书签发者" \
    || reg_fail "prepared 回滚改变了叶子证书签发者"
openssl verify -CAfile "$TMP2/ca/rootCA.pem" "$TMP2/services/iboot-system.pem" >/dev/null 2>&1 \
    && reg_pass "prepared 回滚后叶子证书可验证" \
    || reg_fail "prepared 回滚后叶子证书验证失败"
[ "$(IBOOT_TLS_CERT_DIR="$TMP2" "$ROOT/scripts/tls-ca-rotate.sh" --status 2>&1 | sed -n 's/^state=//p')" = "initial" ] \
    && reg_pass "prepared 回滚后状态回到 initial" \
    || reg_fail "prepared 回滚后状态未回到 initial"

reg_log ""
reg_log "== 回滚：activate → rollback =="
if IBOOT_TLS_CERT_DIR="$TMP2" "$ROOT/scripts/tls-ca-rotate.sh" --prepare --restart none \
        >/tmp/ca-rollback-prepare2.log 2>&1; then
    reg_pass "回滚第二轮 prepare 完成"
else
    reg_fail "回滚第二轮 prepare 失败，详见 /tmp/ca-rollback-prepare2.log"
fi
if IBOOT_TLS_CERT_DIR="$TMP2" "$ROOT/scripts/tls-ca-rotate.sh" --activate --restart none \
        >/tmp/ca-rollback-activate.log 2>&1; then
    reg_pass "回滚用例 activate 完成"
else
    reg_fail "回滚用例 activate 失败，详见 /tmp/ca-rollback-activate.log"
fi
new_issuer2="$(openssl x509 -noout -issuer -in "$TMP2/services/iboot-system.pem")"
[ "$new_issuer2" != "$original_issuer" ] && reg_pass "activate 后叶子证书由新 CA 签发" \
    || reg_fail "activate 后叶子证书签发者未变化"
if IBOOT_TLS_CERT_DIR="$TMP2" "$ROOT/scripts/tls-ca-rotate.sh" --rollback --restart none \
        >/tmp/ca-rollback-activated.log 2>&1; then
    reg_pass "activate → rollback 完成"
else
    reg_fail "activate → rollback 失败，详见 /tmp/ca-rollback-activated.log"
fi
[ "$(openssl x509 -noout -issuer -in "$TMP2/services/iboot-system.pem")" = "$original_issuer" ] \
    && reg_pass "activate 回滚后叶子证书由上一张 CA 重新签发" \
    || reg_fail "activate 回滚后叶子证书签发者未恢复"
certs="$(grep -c 'BEGIN CERTIFICATE' "$TMP2/ca/rootCA.pem" || true)"
[ "$certs" = "1" ] && reg_pass "activate 回滚后 bundle 只保留上一张 CA" \
    || reg_fail "activate 回滚后 bundle 证书数异常：$certs"
openssl verify -CAfile "$TMP2/ca/rootCA.pem" "$TMP2/services/iboot-system.pem" >/dev/null 2>&1 \
    && reg_pass "activate 回滚后叶子证书可验证" \
    || reg_fail "activate 回滚后叶子证书验证失败"

reg_summary
exit "$REG_FAILED"
