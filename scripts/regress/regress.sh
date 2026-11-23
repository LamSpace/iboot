#!/usr/bin/env bash
# 回归总入口：契约快照 → 路由覆盖 → 公开/内部/日志 TLS → 服务间授权 → 证书到期 → 中间件认证 →
# 暴露面 → 明文审计 → 冒烟 → 权限矩阵。
#
# 用法：
#   regress.sh              与 golden 基线比对，出现漂移即失败
#   regress.sh --refresh    重新生成 golden 基线（仅在确认差异合理时使用）
#
# 环境变量：
#   REG_BASE / REG_AI_BASE / REG_AUTH_BASE / REG_GATEWAY / REG_OIDC_REDIRECT / REG_CACERT
#     公开入口（宿主机默认 https://localhost:5173；Docker 覆盖为 https://localhost）
#   SVC_SYSTEM_BASE 等
#     契约快照直连的服务地址（B2 起全部为 HTTPS）

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
GOLDEN="$HERE/golden"
TMP_DIR="$HERE/.tmp"

# 公开入口：登录、冒烟与权限矩阵都经门户，证明 OIDC/API 的同源配置。
REG_BASE="${REG_BASE:-https://localhost:5173}"
REG_AI_BASE="${REG_AI_BASE:-https://localhost:5173}"
REG_AUTH_BASE="${REG_AUTH_BASE:-https://localhost:5173}"
REG_GATEWAY="${REG_GATEWAY:-https://localhost:5173}"
REG_OIDC_REDIRECT="${REG_OIDC_REDIRECT:-https://localhost:5173/callback}"
REG_CACERT="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"

# 契约快照直连各服务；B2 起服务端口只提供 HTTPS。
SVC_SYSTEM_BASE="${SVC_SYSTEM_BASE:-https://localhost:8080}"
SVC_AI_BASE="${SVC_AI_BASE:-https://localhost:8081}"
SVC_FILE_BASE="${SVC_FILE_BASE:-https://localhost:8082}"
SVC_AUTH_BASE="${SVC_AUTH_BASE:-https://localhost:8083}"
SVC_NOTIFY_BASE="${SVC_NOTIFY_BASE:-https://localhost:8084}"
SVC_JOB_BASE="${SVC_JOB_BASE:-https://localhost:8085}"
SVC_WORKFLOW_BASE="${SVC_WORKFLOW_BASE:-https://localhost:8086}"
SVC_MONITOR_BASE="${SVC_MONITOR_BASE:-https://localhost:8087}"
SVC_CACERT="${SVC_CACERT:-$REG_CACERT}"
export REG_BASE REG_AI_BASE REG_AUTH_BASE REG_GATEWAY REG_OIDC_REDIRECT REG_CACERT
export SVC_CACERT
export SVC_SYSTEM_BASE SVC_AI_BASE SVC_FILE_BASE SVC_AUTH_BASE SVC_NOTIFY_BASE
export SVC_JOB_BASE SVC_WORKFLOW_BASE SVC_MONITOR_BASE

# shellcheck source=lib.sh
. "$HERE/lib.sh"

REFRESH=0
if [ "${1:-}" = "--refresh" ]; then REFRESH=1; fi

reg_login
export REG_TOKEN

FAILED=0
fail() { FAILED=1; printf '[FAIL] %s\n' "$*"; }

rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"

printf '===== 1. 接口契约快照 =====\n'
"$HERE/api-snapshot.sh" "$SVC_SYSTEM_BASE" system "$TMP_DIR/system.json"
"$HERE/api-snapshot.sh" "$SVC_AI_BASE" ai "$TMP_DIR/ai.json"
"$HERE/api-snapshot.sh" "$SVC_FILE_BASE" file "$TMP_DIR/file.json"
"$HERE/api-snapshot.sh" "$SVC_AUTH_BASE" auth "$TMP_DIR/auth.json"
"$HERE/api-snapshot.sh" "$SVC_NOTIFY_BASE" notify "$TMP_DIR/notify.json"
"$HERE/api-snapshot.sh" "$SVC_JOB_BASE" job "$TMP_DIR/job.json"
"$HERE/api-snapshot.sh" "$SVC_WORKFLOW_BASE" workflow "$TMP_DIR/workflow.json"
"$HERE/api-snapshot.sh" "$SVC_MONITOR_BASE" monitor "$TMP_DIR/monitor.json"

if [ "$REFRESH" -eq 1 ]; then
    mkdir -p "$GOLDEN"
    cp "$TMP_DIR/system.json" "$GOLDEN/system.json"
    cp "$TMP_DIR/ai.json" "$GOLDEN/ai.json"
    cp "$TMP_DIR/file.json" "$GOLDEN/file.json"
    cp "$TMP_DIR/auth.json" "$GOLDEN/auth.json"
    cp "$TMP_DIR/notify.json" "$GOLDEN/notify.json"
    cp "$TMP_DIR/job.json" "$GOLDEN/job.json"
    cp "$TMP_DIR/workflow.json" "$GOLDEN/workflow.json"
    cp "$TMP_DIR/monitor.json" "$GOLDEN/monitor.json"
    printf '基线已刷新：%s\n' "$GOLDEN"
else
    [ -d "$GOLDEN" ] || fail "缺少 golden 基线，请先执行 regress.sh --refresh"
    for svc in system ai file auth notify job workflow monitor; do
        if [ ! -f "$GOLDEN/$svc.json" ]; then
            fail "缺少 $svc 的基线，确认差异合理后执行 regress.sh --refresh"
            continue
        fi
        if diff -u "$GOLDEN/$svc.json" "$TMP_DIR/$svc.json" > "$TMP_DIR/$svc.diff"; then
            printf '  [PASS] %s 契约零漂移\n' "$svc"
        else
            printf '  [FAIL] %s 契约发生漂移：\n' "$svc"
            head -40 "$TMP_DIR/$svc.diff"
            FAILED=1
        fi
    done
fi

printf '\n===== 2. 路径守恒 =====\n'
"$HERE/path-conservation.sh" "$TMP_DIR" "$GOLDEN" || fail "路径守恒检查未通过"

printf '\n===== 3. 路由覆盖 =====\n'
"$HERE/route-coverage.sh" "$TMP_DIR" || fail "路由覆盖检查未通过"

printf '\n===== 4. TLS 公开入口 =====\n'
"$HERE/tls.sh" || fail "TLS 检查未通过"

printf '\n===== 5. 内部 TLS =====\n'
"$HERE/internal-tls.sh" || fail "内部 TLS 检查未通过"

printf '\n===== 5b. 服务间授权 =====\n'
"$HERE/service-authz.sh" || fail "服务间授权检查未通过"

echo
echo "===== 5c. OPA 服务间授权 ====="
"$HERE/opa-authz.sh" || fail "OPA 服务间授权检查未通过"

printf '\n===== 6. 可观测控制台 TLS =====\n'
"$HERE/observability-tls.sh" || fail "可观测控制台 TLS 检查未通过"

echo
echo "===== 6b. Exporter mTLS ====="
"$HERE/exporter-mtls.sh" || fail "exporter mTLS 检查未通过"

printf '\n===== 7. 证书到期监控 =====\n'
"$HERE/tls-cert-monitor.sh" || fail "证书到期监控检查未通过"

echo
echo "===== 7b. 证书轮换检查模式 ====="
"$ROOT/scripts/tls-rotate.sh" --check || fail "证书轮换检查未通过"

echo
echo "===== 7c. 证书轮换调度 ====="
"$HERE/tls-rotation-schedule.sh" || fail "证书轮换调度检查未通过"

printf '\n===== 8. 中间件认证 =====\n'
"$HERE/middleware-auth.sh" || fail "中间件认证检查未通过"

echo
echo "===== 8a. 表级/键级最小权限 ====="
"$HERE/middleware-least-privilege.sh" || fail "最小权限检查未通过"

echo
echo "===== 8b. RocketMQ ACL ====="
"$HERE/rocketmq-acl.sh" || fail "RocketMQ ACL 检查未通过"

echo
echo "===== 8c. RocketMQ 服务端证书 ====="
"$HERE/rocketmq-server-auth.sh" || fail "RocketMQ 服务端证书检查未通过"

printf '\n===== 9. 日志与检索 TLS =====\n'
"$HERE/log-search-tls.sh" || fail "日志与检索 TLS 检查未通过"

printf '\n===== 10. 暴露面与网络分段 =====\n'
"$HERE/exposure.sh" || fail "暴露面检查未通过"

echo
echo "===== 10b. 宿主机回环绑定 ====="
"$HERE/host-loopback.sh" || fail "宿主机回环绑定检查未通过"

printf '\n===== 11. 明文默认地址审计 =====\n'
"$HERE/plaintext-audit.sh" || fail "明文默认地址审计未通过"

printf '\n===== 12. 冒烟 =====\n'
"$HERE/smoke.sh" || fail "冒烟未通过"

echo
echo "===== 12b. SPIRE Workload API ====="
"$HERE/spire-svid.sh" || fail "SPIRE Workload API 检查未通过"

printf '\n===== 13. 权限矩阵 =====\n'
"$HERE/permission-matrix.sh" || fail "权限矩阵未通过"

printf '\n'
if [ "$FAILED" -eq 0 ]; then
    printf '回归全部通过\n'
else
    printf '回归存在失败项\n' >&2
fi
exit "$FAILED"
