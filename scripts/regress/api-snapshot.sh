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
if [ -n "${SVC_CACERT:-}" ]; then
    CA_OPTS+=(--cacert "$SVC_CACERT")
    # 服务端口要求客户端证书；显式参数优先，默认使用门户证书。
    cert_dir="${IBOOT_TLS_CERT_DIR:-$HERE/../../docker/certs}"
    client_cert="${SVC_CLIENT_CERT:-${REG_CLIENT_CERT:-$cert_dir/portal.pem}}"
    client_key="${SVC_CLIENT_KEY:-${REG_CLIENT_KEY:-$cert_dir/portal-key.pem}}"
    if [ -f "$client_cert" ] && [ -f "$client_key" ]; then
        CA_OPTS+=(--cert "$client_cert" --key "$client_key")
    fi
fi

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
