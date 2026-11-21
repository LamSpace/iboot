#!/usr/bin/env bash
# 明文默认地址审计：应用配置与 Grafana 数据源中的内部依赖默认地址不得是 http://。
#
# 允许清单见 plaintext-allowlist.txt；命中清单时输出例外，不计为失败。
# 本检查只做静态扫描，实际连接仍由 TLS 回归验证。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$HERE/lib.sh"

ROOT="$(cd "$HERE/../.." && pwd)"
ALLOW="$HERE/plaintext-allowlist.txt"

ALLOW_ENTRIES=()
if [ -f "$ALLOW" ]; then
    while IFS= read -r line; do
        case "$line" in
            ''|'#'*) continue ;;
        esac
        ALLOW_ENTRIES+=("$line")
    done < "$ALLOW"
fi

is_allowed() {
    local content="$1" entry
    for entry in "${ALLOW_ENTRIES[@]}"; do
        if [[ "$content" == *"$entry"* ]]; then
            return 0
        fi
    done
    return 1
}

reg_log "== 明文默认地址审计 =="

shopt -s nullglob
FILES=(
    "$ROOT"/iboot-*/src/main/resources/application*.yml
    "$ROOT"/iboot-*/src/main/resources/iboot-*.yml
    "$ROOT"/docker/scripts/grafana/provisioning/datasources/*.yaml
)
shopt -u nullglob

ALLOWED_COUNT=0
for file in "${FILES[@]}"; do
    [ -f "$file" ] || continue
    rel="${file#"$ROOT"/}"
    while IFS= read -r hit; do
        [ -n "$hit" ] || continue
        content="${hit#*:}"
        # 跳过注释行
        [[ "$content" =~ ^[[:space:]]*# ]] && continue
        if is_allowed "$content"; then
            ALLOWED_COUNT=$((ALLOWED_COUNT + 1))
            reg_log "  [ALLOW] $rel: ${content#"${content%%[![:space:]]*}"}"
        else
            reg_fail "未登记的明文默认地址：$rel: ${content#"${content%%[![:space:]]*}"}"
        fi
    done < <(grep -n "http://" "$file" 2>/dev/null || true)
done

if [ "$ALLOWED_COUNT" -gt 0 ]; then
    reg_log "  已登记例外：$ALLOWED_COUNT 处（见 scripts/regress/plaintext-allowlist.txt）"
fi

reg_summary
