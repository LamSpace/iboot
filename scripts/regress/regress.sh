#!/usr/bin/env bash
# 回归总入口：契约快照 → 路由覆盖 → 冒烟 → 权限矩阵。
#
# 用法：
#   regress.sh              与 golden 基线比对，出现漂移即失败
#   regress.sh --refresh    重新生成 golden 基线（仅在确认差异合理时使用）
#
# 环境变量：
#   REG_BASE / REG_AI_BASE / REG_AUTH_BASE / REG_GATEWAY / REG_OIDC_REDIRECT / REG_CACERT
#     公开入口（宿主机默认 https://localhost:5173；Docker 覆盖为 https://localhost）
#   SVC_SYSTEM_BASE 等
#     契约快照直连的服务地址（B1 内部服务仍为 HTTP，B2 再切换）

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

# 契约快照直连各服务；B1 只加密公开入口，内部服务仍是 HTTP。
SVC_SYSTEM_BASE="${SVC_SYSTEM_BASE:-http://localhost:8080}"
SVC_AI_BASE="${SVC_AI_BASE:-http://localhost:8081}"
SVC_FILE_BASE="${SVC_FILE_BASE:-http://localhost:8082}"
SVC_AUTH_BASE="${SVC_AUTH_BASE:-http://localhost:8083}"
SVC_NOTIFY_BASE="${SVC_NOTIFY_BASE:-http://localhost:8084}"
SVC_JOB_BASE="${SVC_JOB_BASE:-http://localhost:8085}"
SVC_WORKFLOW_BASE="${SVC_WORKFLOW_BASE:-http://localhost:8086}"
SVC_MONITOR_BASE="${SVC_MONITOR_BASE:-http://localhost:8087}"
export REG_BASE REG_AI_BASE REG_AUTH_BASE REG_GATEWAY REG_OIDC_REDIRECT REG_CACERT
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

printf '\n===== 5. 冒烟 =====\n'
"$HERE/smoke.sh" || fail "冒烟未通过"

printf '\n===== 6. 权限矩阵 =====\n'
"$HERE/permission-matrix.sh" || fail "权限矩阵未通过"

printf '\n'
if [ "$FAILED" -eq 0 ]; then
    printf '回归全部通过\n'
else
    printf '回归存在失败项\n' >&2
fi
exit "$FAILED"
