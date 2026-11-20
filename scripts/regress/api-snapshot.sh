#!/usr/bin/env bash
# 导出服务的 OpenAPI 快照并归一化为基线文件。
#
# 用法：api-snapshot.sh <base-url> <service> [输出文件]
# 示例：api-snapshot.sh https://localhost:8080 system
#
# 未指定输出文件时写入 golden/<service>.json。
# 内部 TLS 环境使用 SVC_CACERT 指向的本地 CA；缺少时请求会因证书校验失败。

set -euo pipefail

BASE="${1:?用法: api-snapshot.sh <base-url> <service> [输出文件]}"
SERVICE="${2:?用法: api-snapshot.sh <base-url> <service> [输出文件]}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${3:-$HERE/golden/$SERVICE.json}"

mkdir -p "$(dirname "$OUT")"

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

CA_OPTS=()
[ -n "${SVC_CACERT:-}" ] && CA_OPTS+=(--cacert "$SVC_CACERT")

# 部分服务的 OpenAPI 端点受鉴权保护，REG_TOKEN 存在时附带令牌
if [ -n "${REG_TOKEN:-}" ]; then
    curl -sf "${CA_OPTS[@]}" -H "Authorization: Bearer $REG_TOKEN" "$BASE/v3/api-docs" -o "$TMP" || {
        echo "[ERROR] 无法获取 $BASE/v3/api-docs（已带令牌），服务是否已启动？" >&2
        exit 1
    }
else
    curl -sf "${CA_OPTS[@]}" "$BASE/v3/api-docs" -o "$TMP" || {
        echo "[ERROR] 无法获取 $BASE/v3/api-docs，服务是否已启动？" >&2
        exit 1
    }
fi

python3 -I "$HERE/normalize_openapi.py" "$TMP" "$OUT"
