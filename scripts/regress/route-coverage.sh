#!/usr/bin/env bash
# 校验 golden 中所有接口路径都能被网关路由规则唯一解析。
#
# 用法：route-coverage.sh [golden 目录] [routes.txt]
# 未指定时使用脚本同目录下的 golden/ 与 routes.txt。

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GOLDEN="${1:-$HERE/golden}"
ROUTES="${2:-$HERE/routes.txt}"

[ -d "$GOLDEN" ] || { echo "[ERROR] golden 目录不存在：$GOLDEN" >&2; exit 1; }
[ -f "$ROUTES" ] || { echo "[ERROR] 路由规则不存在：$ROUTES" >&2; exit 1; }

python3 -I "$HERE/route_coverage.py" "$GOLDEN" "$ROUTES"
