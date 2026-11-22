#!/usr/bin/env bash
# 服务间授权回归：允许链路（门户、网关、探针、SBA、AI、scope、internal）与拒绝链路。
#
# 前置：服务、网关与 ops 已启用 IBOOT_SERVICE_AUTHORIZATION_ENABLED=true；
# 证书目录已由 scripts/tls-init.sh 生成。
# 环境变量：REG_CACERT、REG_BASE、REG_AUTH_BASE、IBOOT_SYSTEM_INTERNAL_TOKEN。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
CERT_DIR="${IBOOT_TLS_CERT_DIR:-$ROOT/docker/certs}"
SYSTEM="https://localhost:8080"
GATEWAY="https://localhost:8088"
OPS="https://localhost:8089"
INTERNAL_TOKEN="${IBOOT_SYSTEM_INTERNAL_TOKEN:-iboot-dev-internal-token}"
BODY="$(mktemp)"
trap 'rm -f "$BODY"' EXIT

svc_request() {
    local name="$1"
    shift
    curl -sS --cacert "$CA" \
        --cert "$CERT_DIR/services/$name.pem" --key "$CERT_DIR/services/$name-key.pem" "$@"
}

client_request() {
    local name="$1"
    shift
    curl -sS --cacert "$CA" \
        --cert "$CERT_DIR/clients/$name.pem" --key "$CERT_DIR/clients/$name-key.pem" "$@"
}

portal_request() {
    curl -sS --cacert "$CA" \
        --cert "$CERT_DIR/portal.pem" --key "$CERT_DIR/portal-key.pem" "$@"
}

status_of() {
    local status
    status="$("$@" -o "$BODY" -w '%{http_code}' 2>/dev/null || true)"
    printf '%s' "${status:-000}"
}

expect_status() {
    local desc="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        reg_pass "$desc（HTTP $actual）"
    else
        reg_fail "$desc（期望 $expected，实际 ${actual:-无响应}）"
    fi
}

expect_denied() {
    local desc="$1" status="$2"
    if [ "$status" = "403" ] && grep -q '调用方无权访问' "$BODY"; then
        reg_pass "$desc（403）"
    else
        reg_fail "$desc（期望 403 授权拒绝，实际 ${status:-无响应}）"
    fi
}

reg_log "== 0. 登录 =="
reg_login || exit 3
reg_pass "门户以管理员身份登录成功"

reg_log "== 1. 允许链路 =="
expect_status "门户经网关查询用户列表" 200 \
    "$(status_of portal_request -H "Authorization: Bearer $REG_TOKEN" "$GATEWAY/api/user/list")"
expect_status "门户访问系统服务 Actuator" 200 \
    "$(status_of portal_request "$SYSTEM/actuator/health")"
expect_status "门户访问系统服务接口文档" 200 \
    "$(status_of portal_request "$SYSTEM/v3/api-docs")"
expect_status "Prometheus 访问系统指标" 200 \
    "$(status_of client_request prometheus "$SYSTEM/actuator/prometheus")"
expect_status "blackbox 访问系统健康检查" 200 \
    "$(status_of client_request blackbox "$SYSTEM/actuator/health")"
expect_status "SBA 访问系统健康检查" 200 \
    "$(status_of svc_request iboot-ops "$SYSTEM/actuator/health")"
expect_status "AI 访问系统用户列表" 200 \
    "$(status_of svc_request iboot-ai -H "Authorization: Bearer $REG_TOKEN" "$SYSTEM/api/user/list")"
expect_status "AI 访问系统部门列表" 200 \
    "$(status_of svc_request iboot-ai -H "Authorization: Bearer $REG_TOKEN" "$SYSTEM/api/dept/list")"
expect_status "文件服务访问系统数据权限范围" 200 \
    "$(status_of svc_request iboot-file -H "Authorization: Bearer $REG_TOKEN" "$SYSTEM/api/auth/scope")"
expect_status "审批服务访问系统内部额度" 200 \
    "$(status_of svc_request iboot-workflow -H "X-Internal-Token: $INTERNAL_TOKEN" \
        "$SYSTEM/internal/leave/quota?userId=1&year=2026")"

if [ "${IBOOT_OPS_ENABLED:-true}" = "true" ]; then
    expect_status "门户访问运维控制台 Actuator" 200 \
        "$(status_of portal_request "$OPS/actuator/health")"
fi

reg_log "== 2. 拒绝链路 =="
expect_denied "文件服务不能直连网关 API" \
    "$(status_of svc_request iboot-file -H "Authorization: Bearer $REG_TOKEN" "$GATEWAY/api/user/list")"
expect_denied "文件服务不能访问系统用户列表" \
    "$(status_of svc_request iboot-file -H "Authorization: Bearer $REG_TOKEN" "$SYSTEM/api/user/list")"
expect_denied "文件服务不能访问系统 Actuator" \
    "$(status_of svc_request iboot-file "$SYSTEM/actuator/health")"
expect_denied "文件服务不能访问系统接口文档" \
    "$(status_of svc_request iboot-file "$SYSTEM/v3/api-docs")"
expect_denied "文件服务不能访问系统内部额度" \
    "$(status_of svc_request iboot-file -H "X-Internal-Token: $INTERNAL_TOKEN" \
        "$SYSTEM/internal/leave/quota?userId=1&year=2026")"
expect_denied "文件服务不能访问网关 Actuator" \
    "$(status_of svc_request iboot-file "$GATEWAY/actuator/health")"

if [ "${IBOOT_OPS_ENABLED:-true}" = "true" ]; then
    expect_denied "文件服务不能访问运维控制台 Actuator" \
        "$(status_of svc_request iboot-file "$OPS/actuator/health")"
fi

reg_summary
