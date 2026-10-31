#!/usr/bin/env bash
# 只读接口的响应等价性：迁移前录制，迁移后经网关重放并比对。
#
# 用法：
#   equivalence.sh --record   从原服务（默认 8080）录制基线
#   equivalence.sh --verify   经网关（默认 8088）重放并与基线比对
#
# 环境变量：REG_BASE（录制源，默认 http://localhost:8080）、
#           REG_GATEWAY（重放入口，默认 http://localhost:8088）、REG_USER、REG_PASS

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GOLDEN="$HERE/golden/file-equivalence.json"
TMP="$HERE/.tmp"
mkdir -p "$TMP"

REG_BASE="${REG_BASE:-http://localhost:8080}"
REG_GATEWAY="${REG_GATEWAY:-http://localhost:8088}"
export REG_BASE

# shellcheck source=lib.sh
. "$HERE/lib.sh"

MODE="${1:-}"
case "$MODE" in
    --record|--verify) ;;
    *) echo "用法: equivalence.sh --record|--verify" >&2; exit 2 ;;
esac

reg_login
export REG_TOKEN

if [ "$MODE" = "--record" ]; then
    python3 -I "$HERE/equivalence.py" "$REG_BASE" "$REG_TOKEN" "$GOLDEN"
    exit 0
fi

[ -f "$GOLDEN" ] || { echo "[ERROR] 缺少等价性基线：$GOLDEN（先执行 --record）" >&2; exit 1; }
python3 -I "$HERE/equivalence.py" "$REG_GATEWAY" "$REG_TOKEN" "$TMP/file-equivalence.json"
if diff -u "$GOLDEN" "$TMP/file-equivalence.json" > "$TMP/file-equivalence.diff"; then
    echo "[PASS] 文件接口等价性一致"
    exit 0
fi
echo "[FAIL] 文件接口响应与基线不一致："
head -60 "$TMP/file-equivalence.diff"
exit 1
