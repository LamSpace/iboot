#!/usr/bin/env bash
# 只读接口的响应等价性：迁移前录制，迁移后经网关重放并比对。
#
# 用法：
#   equivalence.sh --record [--service file|notify|job|workflow|monitor]   从录制源录制基线
#   equivalence.sh --verify [--service file|notify|job|workflow|monitor]   经网关重放并与基线比对
#
# 环境变量：REG_BASE（登录与默认录制源，默认 http://localhost:8080）、
#           REG_RECORD_BASE（仅 --record 的取数地址；旧实现删除后指向对应服务）、
#           REG_GATEWAY（重放入口，默认 http://localhost:8088）、REG_USER、REG_PASS

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TMP="$HERE/.tmp"
mkdir -p "$TMP"

REG_BASE="${REG_BASE:-http://localhost:8080}"
REG_RECORD_BASE="${REG_RECORD_BASE:-$REG_BASE}"
REG_GATEWAY="${REG_GATEWAY:-http://localhost:8088}"
export REG_BASE

# shellcheck source=lib.sh
. "$HERE/lib.sh"

MODE=""
SERVICE="file"
while [ $# -gt 0 ]; do
    case "$1" in
        --record|--verify) MODE="$1" ;;
        --service)
            shift
            SERVICE="${1:-}"
            ;;
        *)
            echo "用法: equivalence.sh --record|--verify [--service file|notify|job|workflow|monitor]" >&2
            exit 2
            ;;
    esac
    shift
done
[ -n "$MODE" ] || { echo "用法: equivalence.sh --record|--verify [--service file|notify|job|workflow|monitor]" >&2; exit 2; }
case "$SERVICE" in
    file|notify|job|workflow|monitor) ;;
    *) echo "[ERROR] 未知服务：$SERVICE" >&2; exit 2 ;;
esac

GOLDEN="$HERE/golden/${SERVICE}-equivalence.json"

reg_login
export REG_TOKEN

if [ "$MODE" = "--record" ]; then
    python3 -I "$HERE/equivalence.py" "$REG_RECORD_BASE" "$REG_TOKEN" "$GOLDEN" "$SERVICE"
    exit 0
fi

[ -f "$GOLDEN" ] || { echo "[ERROR] 缺少等价性基线：$GOLDEN（先执行 --record）" >&2; exit 1; }
python3 -I "$HERE/equivalence.py" "$REG_GATEWAY" "$REG_TOKEN" "$TMP/${SERVICE}-equivalence.json" "$SERVICE"
if diff -u "$GOLDEN" "$TMP/${SERVICE}-equivalence.json" > "$TMP/${SERVICE}-equivalence.diff"; then
    echo "[PASS] ${SERVICE} 接口等价性一致"
    exit 0
fi
echo "[FAIL] ${SERVICE} 接口响应与基线不一致："
head -60 "$TMP/${SERVICE}-equivalence.diff"
exit 1
