#!/usr/bin/env bash
# 校验服务迁移的路径守恒：全部服务快照的路径并集前后一致，且迁移登记与实际一致。
#
# 用法：path-conservation.sh [当前快照目录] [基线目录] [migrations.txt]
# 未指定时使用脚本同目录下的 .tmp/、golden/ 与 migrations.txt。

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CURRENT="${1:-$HERE/.tmp}"
BASELINE="${2:-$HERE/golden}"
MIGRATIONS="${3:-$HERE/migrations.txt}"

[ -d "$CURRENT" ] || { echo "[ERROR] 当前快照目录不存在：$CURRENT" >&2; exit 1; }
[ -d "$BASELINE" ] || { echo "[ERROR] 基线目录不存在：$BASELINE" >&2; exit 1; }
[ -f "$MIGRATIONS" ] || { echo "[ERROR] 迁移登记不存在：$MIGRATIONS" >&2; exit 1; }

python3 -I "$HERE/path_conservation.py" "$CURRENT" "$BASELINE" "$MIGRATIONS"
