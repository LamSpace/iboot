#!/usr/bin/env bash
#
# 兼容入口：实际部署逻辑已迁移到 scripts/docker-deploy.sh。
# 保留此文件是为了让旧的 `cd docker && ./deploy.sh ...` 用法继续可用。
#
set -e
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "$ROOT/scripts/docker-deploy.sh" "$@"
