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
cp "$LIVE_CA/rootCA.pem" "$LIVE_CA/rootCA-key.pem" "$TMP/ca/"

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

reg_summary
exit "$REG_FAILED"
