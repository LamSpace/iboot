#!/usr/bin/env bash
#
# 把 Sentinel 基线规则幂等写入 Nacos。
#
# 用法：
#   scripts/sentinel-init.sh           写入两个基线规则
#   scripts/sentinel-init.sh --verify  写入后回读并校验内容
#
# 说明：
# - Nacos 是规则权威来源；Dashboard 上的临时修改不会写回 Nacos。
# - 规则文件位于 docker/sentinel/rules/，group 固定为 SENTINEL_GROUP。
# - 依赖 Nacos 3.x 仍保留的 v1 配置写入接口（见 docs/23 §8）。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NACOS_ADDR="${NACOS_ADDR:-localhost:8848}"
NACOS_URL="http://${NACOS_ADDR}/nacos/v1/cs/configs"
GROUP="${SENTINEL_GROUP:-SENTINEL_GROUP}"
RULES_DIR="$ROOT/docker/sentinel/rules"
VERIFY=0

[ "${1:-}" = "--verify" ] && VERIFY=1

log()  { printf '%s\n' "$*"; }
fail() { printf '[ERROR] %s\n' "$*" >&2; exit 1; }

command -v curl >/dev/null 2>&1 || fail '未找到 curl'
[ -d "$RULES_DIR" ] || fail "规则目录不存在：$RULES_DIR"

publish() {
    local data_id="$1" file="$2" remote local_content
    [ -f "$file" ] || fail "规则文件不存在：$file"

    curl -sf -X POST "$NACOS_URL" \
        --data-urlencode "dataId=$data_id" \
        --data-urlencode "group=$GROUP" \
        --data-urlencode "content@$file" \
        --data-urlencode "type=json" >/dev/null \
        || fail "写入 Nacos 失败：dataId=$data_id"

    if [ "$VERIFY" -eq 1 ]; then
        local_content="$(cat "$file")"
        remote=''
        for _ in $(seq 1 10); do
            remote="$(curl -sf "$NACOS_URL?dataId=$data_id&group=$GROUP" || true)"
            [ "$remote" = "$local_content" ] && break
            sleep 0.3
        done
        [ "$remote" = "$local_content" ] \
            || fail "回读 Nacos 失败或内容不一致：dataId=$data_id"
    fi
    log "  已写入 $data_id（group=$GROUP）"
}

log '=== Sentinel 基线规则 ==='
publish 'iboot-gateway-flow-rules' "$RULES_DIR/iboot-gateway-flow-rules.json"
publish 'iboot-notify-flow-rules' "$RULES_DIR/iboot-notify-flow-rules.json"
log 'Sentinel 基线规则就绪'
