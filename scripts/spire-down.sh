#!/usr/bin/env bash
# 停止 SPIRE demo profile；保留数据卷，便于重复运行。
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi
docker rm -f iboot-spire-demo iboot-spire-agent iboot-spire-server >/dev/null 2>&1 || true
printf '[spire-down] SPIRE 容器已停止（数据卷保留）\n'
