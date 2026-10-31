#!/usr/bin/env bash
# 按 TraceID 查询一条链路：输出涉及的服务与父子关系，并判断是否包含期望服务。
#
# 用法：
#   trace-chain.sh <traceId> [--expect svc1,svc2] [--tempo URL] [--wait 秒]
#
# 复现来源是网关响应头 X-Trace-Id（应用日志的 MDC 亦可），不使用按时间范围检索，
# 因此结论不受系统时钟与查询窗口边界影响。
#
# 追踪后端按批次导出，同一条链路的分段可能先后到达：脚本会在 --wait 指定的时间内
# 按标识轮询，直到期望服务齐全或超时。轮询依据的仍是标识本身，不是时间窗。
#
# 退出码：
#   0  链路包含全部期望服务
#   1  链路存在但缺少期望服务；或追踪后端中没有该标识
#   2  参数错误
#   3  追踪后端不可用

set -uo pipefail

TEMPO="${TEMPO_URL:-http://localhost:3200}"
TRACE_ID=""
EXPECT="iboot-gateway"
WAIT="${TRACE_WAIT:-10}"

usage() {
    printf '用法：%s <traceId> [--expect svc1,svc2] [--tempo URL] [--wait 秒]\n' "$(basename "$0")"
    printf '  --expect  期望出现在同一链路中的服务，逗号分隔，默认 iboot-gateway\n'
    printf '  --tempo   追踪后端地址，默认 %s\n' "$TEMPO"
    printf '  --wait    等待分段导出齐全的秒数，默认 %s\n' "$WAIT"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --expect) EXPECT="${2:-}"; shift 2 ;;
        --tempo)  TEMPO="${2:-}";  shift 2 ;;
        --wait)   WAIT="${2:-}";   shift 2 ;;
        -h|--help) usage; exit 0 ;;
        -*) printf '[ERROR] 未知参数：%s\n' "$1" >&2; usage >&2; exit 2 ;;
        *)  TRACE_ID="$1"; shift ;;
    esac
done

if [ -z "$TRACE_ID" ]; then
    usage >&2
    exit 2
fi

if ! [[ "$TRACE_ID" =~ ^[0-9a-fA-F]{32}$ ]]; then
    printf '[ERROR] TraceID 应为 32 位十六进制，实际：%s\n' "$TRACE_ID" >&2
    exit 2
fi

BODY="$(mktemp)"
REPORT="$(mktemp)"
trap 'rm -f "$BODY" "$REPORT"' EXIT

# 解析一次查询结果；全部期望服务齐全时返回 0。
# @param $1 追踪后端响应文件
# @param $2 期望服务，逗号分隔
# @param $3 TraceID
# @param $4 输出文件
parse() {
    python3 - "$1" "$2" "$3" <<'PY' > "$4"
import collections
import json
import sys

path, expect_raw, trace_id = sys.argv[1], sys.argv[2], sys.argv[3]
with open(path, encoding='utf-8') as fh:
    data = json.load(fh)

spans = []
for batch in data.get('batches', []):
    attrs = {a['key']: a['value'].get('stringValue') for a in batch.get('resource', {}).get('attributes', [])}
    service = attrs.get('service.name', 'unknown')
    for scope in batch.get('scopeSpans', []):
        for span in scope.get('spans', []):
            spans.append({
                'id': span.get('spanId'),
                'parent': span.get('parentSpanId'),
                'name': span.get('name'),
                'service': service,
            })

if not spans:
    print(f'[FAIL] 追踪后端中没有该标识：{trace_id}')
    sys.exit(1)

by_id = {s['id']: s for s in spans}
children = collections.defaultdict(list)
roots = []
for span in spans:
    parent = span['parent']
    if parent and parent in by_id:
        children[parent].append(span)
    else:
        roots.append(span)

for key in children:
    children[key].sort(key=lambda s: (s['service'], s['name']))

services = collections.Counter(s['service'] for s in spans)

print(f'TraceID: {trace_id}')
print(f'span 数: {len(spans)}')
print('服务分布: ' + ', '.join(f'{name}({count})' for name, count in sorted(services.items())))
print('链路:')

def walk(span, depth):
    print('  ' + '  ' * depth + f"{span['service']} / {span['name']}")
    for child in children.get(span['id'], []):
        walk(child, depth + 1)

for root in sorted(roots, key=lambda s: (s['service'], s['name'])):
    walk(root, 0)

expected = [s for s in expect_raw.split(',') if s]
missing = [s for s in expected if s not in services]
if missing:
    print(f'[FAIL] 缺少期望服务：{", ".join(missing)}')
    sys.exit(1)

print(f'[PASS] 包含期望服务：{", ".join(expected)}')
PY
}

DEADLINE=$(( $(date +%s) + WAIT ))
while :; do
    STATUS="$(curl -sS -m 10 -o "$BODY" -w '%{http_code}' "$TEMPO/api/traces/$TRACE_ID" 2>/dev/null)"
    CURL_EXIT=$?

    if [ "$CURL_EXIT" -ne 0 ] || [ "$STATUS" = "000" ]; then
        printf '[ERROR] 追踪后端不可用：%s（curl 退出码 %s）\n' "$TEMPO" "$CURL_EXIT" >&2
        exit 3
    fi

    if [ "$STATUS" = "200" ]; then
        if parse "$BODY" "$EXPECT" "$TRACE_ID" "$REPORT"; then
            cat "$REPORT"
            exit 0
        fi
    elif [ "$STATUS" != "404" ]; then
        printf '[ERROR] 追踪后端返回 HTTP %s：%s\n' "$STATUS" "$TEMPO" >&2
        exit 3
    fi

    if [ "$(date +%s)" -ge "$DEADLINE" ]; then
        break
    fi
    sleep 1
done

if [ "$STATUS" = "404" ]; then
    printf '[FAIL] 追踪后端中没有该标识：%s（已等待 %s 秒）\n' "$TRACE_ID" "$WAIT"
    printf '       可能原因：请求未被采样、追踪未开启，或标识不属于当前追踪后端。\n'
else
    cat "$REPORT"
    printf '（已等待 %s 秒，链路仍未齐全）\n' "$WAIT"
fi
exit 1
