#!/usr/bin/env bash
# 回归总入口：契约快照 → 路由覆盖 → 冒烟 → 权限矩阵。
#
# 用法：
#   regress.sh              与 golden 基线比对，出现漂移即失败
#   regress.sh --refresh    重新生成 golden 基线（仅在确认差异合理时使用）
#
# 环境变量：REG_BASE、REG_AI_BASE、REG_USER、REG_PASS

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GOLDEN="$HERE/golden"
TMP_DIR="$HERE/.tmp"

REG_BASE="${REG_BASE:-http://localhost:8080}"
REG_AI_BASE="${REG_AI_BASE:-http://localhost:8081}"
REG_FILE_BASE="${REG_FILE_BASE:-http://localhost:8082}"
export REG_BASE REG_AI_BASE REG_FILE_BASE

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
"$HERE/api-snapshot.sh" "$REG_BASE" admin "$TMP_DIR/admin.json"
"$HERE/api-snapshot.sh" "$REG_AI_BASE" ai "$TMP_DIR/ai.json"
"$HERE/api-snapshot.sh" "$REG_FILE_BASE" file "$TMP_DIR/file.json"

if [ "$REFRESH" -eq 1 ]; then
    mkdir -p "$GOLDEN"
    cp "$TMP_DIR/admin.json" "$GOLDEN/admin.json"
    cp "$TMP_DIR/ai.json" "$GOLDEN/ai.json"
    cp "$TMP_DIR/file.json" "$GOLDEN/file.json"
    printf '基线已刷新：%s\n' "$GOLDEN"
else
    [ -d "$GOLDEN" ] || fail "缺少 golden 基线，请先执行 regress.sh --refresh"
    for svc in admin ai file; do
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

printf '\n===== 4. 冒烟 =====\n'
"$HERE/smoke.sh" || fail "冒烟未通过"

printf '\n===== 5. 权限矩阵 =====\n'
"$HERE/permission-matrix.sh" || fail "权限矩阵未通过"

printf '\n'
if [ "$FAILED" -eq 0 ]; then
    printf '回归全部通过\n'
else
    printf '回归存在失败项\n' >&2
fi
exit "$FAILED"
