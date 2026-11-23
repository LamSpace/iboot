#!/usr/bin/env bash
# SPIRE Workload API 回归：Server/Agent 健康、demo SVID、SPIFFE ID 与清理。
#
# 启动 `spire` profile；失败时保留容器日志，结束后 down（保留数据卷）。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
. "$HERE/lib.sh"

OUTPUT="$(mktemp)"
trap 'rm -f "$OUTPUT"' EXIT

if ! "$ROOT/scripts/spire-up.sh" >"$OUTPUT" 2>&1; then
    reg_fail "SPIRE 启动或 SVID 获取失败"
    tail -60 "$OUTPUT"
    "$ROOT/scripts/spire-down.sh" >/dev/null 2>&1 || true
    reg_summary
    exit "$REG_FAILED"
fi

if grep -q 'spiffe://iboot.local/service/iboot-demo' "$OUTPUT"; then
    reg_pass "demo 工作负载取得 SVID（SPIFFE ID iboot-demo）"
else
    reg_fail "demo 输出缺少 SPIFFE ID"
fi

if grep -q 'x509_svid' "$OUTPUT"; then
    reg_pass "Workload API 返回 X.509-SVID"
else
    reg_fail "Workload API 未返回 X.509-SVID"
fi

if docker exec iboot-spire-server /opt/spire/bin/spire-server entry show \
    -socketPath /run/spire/server/api.sock 2>/dev/null \
    | grep -q 'spiffe://iboot.local/service/iboot-demo'; then
    reg_pass "SPIRE Server 已注册 demo 工作负载"
else
    reg_fail "SPIRE Server 未找到 demo 注册条目"
fi

if docker exec iboot-spire-agent /opt/spire/bin/spire-agent healthcheck \
    -socketPath /run/spire/sockets/agent.sock >/dev/null 2>&1; then
    reg_pass "SPIRE Agent 健康"
else
    reg_fail "SPIRE Agent 不健康"
fi

"$ROOT/scripts/spire-down.sh" >/dev/null 2>&1 || true
reg_summary
exit "$REG_FAILED"
