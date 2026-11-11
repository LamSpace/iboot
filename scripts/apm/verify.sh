#!/usr/bin/env bash
# 双后端链路验收：同一请求在 Tempo 与 SkyWalking 两侧都能还原完整调用链。
#
# 用法：
#   scripts/apm/verify.sh
# 环境变量：
#   APM_GATEWAY  网关地址，默认 http://localhost:8088
#   APM_TEMPO    Tempo 地址，默认 http://localhost:3200
#   APM_OAP      OAP HTTP/GraphQL 地址，默认 http://localhost:12800
#   APM_EXPECT   期望出现在链路中的服务，逗号分隔，默认 iboot-gateway,iboot-system
#   APM_WAIT     等待链路分段到齐的秒数，默认 30
#
# 退出码：0 两侧链路完整；1 链路缺失或不完整；2 参数/依赖错误；3 追踪后端不可用。
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../regress/lib.sh
. "$ROOT/scripts/regress/lib.sh"

GATEWAY="${APM_GATEWAY:-http://localhost:8088}"
TEMPO="${APM_TEMPO:-http://localhost:3200}"
OAP="${APM_OAP:-http://localhost:12800}"
EXPECT="${APM_EXPECT:-iboot-gateway,iboot-system}"
WAIT="${APM_WAIT:-30}"
PROBE_PATH="${APM_PROBE_PATH:-/api/user/profile}"

for tool in curl jq; do
    command -v "$tool" >/dev/null 2>&1 || { printf '[ERROR] 缺少依赖：%s\n' "$tool" >&2; exit 2; }
done

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

printf '== 1. 登录并产生一次跨服务请求 ==\n'
reg_login || exit 3

START="$(date -u '+%Y-%m-%d %H%M%S')"
START_MIN="$(date -u '+%Y-%m-%d %H%M')"
HTTP_CODE="$(curl -sS -D "$WORK/headers" -o "$WORK/body" -w '%{http_code}' \
    -H "Authorization: Bearer $REG_TOKEN" "$GATEWAY$PROBE_PATH" || true)"
if [ "$HTTP_CODE" != "200" ]; then
    printf '[FAIL] 探针请求返回 %s：%s\n' "$HTTP_CODE" "$(head -c 300 "$WORK/body" 2>/dev/null)" >&2
    exit 1
fi
TEMPO_TRACE="$(tr -d '\r' < "$WORK/headers" | awk 'tolower($1) == "x-trace-id:" {print $2}' | tail -1)"
if [ -z "$TEMPO_TRACE" ]; then
    printf '[FAIL] 响应缺少 X-Trace-Id，无法在 Tempo 侧定位链路\n' >&2
    exit 1
fi
printf '  Tempo TraceID：%s\n' "$TEMPO_TRACE"

printf '== 2. Tempo 侧还原链路 ==\n'
if ! "$ROOT/scripts/regress/trace-chain.sh" "$TEMPO_TRACE" \
        --expect "$EXPECT" --tempo "$TEMPO" --wait "$WAIT"; then
    printf '[FAIL] Tempo 链路缺失期望服务：%s\n' "$EXPECT" >&2
    exit 1
fi

printf '== 3. SkyWalking 侧还原链路 ==\n'
# BanyanDB 存储不支持 queryBasicTracesByName，必须按 serviceId + endpointId 走 queryTraces。
# 时间窗精度为分钟（yyyy-MM-dd HHmm），由脚本固定生成，不接受人工输入。
FIRST_SERVICE="${EXPECT%%,*}"

oap_query() {
    curl -sS -m 15 -X POST -H 'Content-Type: application/json' \
        -d "$1" "$OAP/graphql"
}

SERVICE_RESP="$(oap_query "$(jq -n --arg svc "$FIRST_SERVICE" '{
    query: "query($svc: String!){ findService(serviceName: $svc){ id name } }",
    variables: {svc: $svc}
}')")"
SERVICE_ID="$(printf '%s' "$SERVICE_RESP" | jq -r '.data.findService.id // empty')"
if [ -z "$SERVICE_ID" ]; then
    printf '[FAIL] SkyWalking 中没有服务 %s（确认 Agent 已挂载且辅助栈可达）：%s\n' \
        "$FIRST_SERVICE" "$(printf '%s' "$SERVICE_RESP" | head -c 300)" >&2
    exit 1
fi

ENDPOINT_ID=""
SW_TRACE=""
EP_RESP=""
END_MIN="$(date -u '+%Y-%m-%d %H%M')"
DEADLINE=$(( $(date +%s) + WAIT ))
while :; do
    END_MIN="$(date -u '+%Y-%m-%d %H%M')"
    if [ -z "$ENDPOINT_ID" ]; then
        EP_RESP="$(oap_query "$(jq -n \
            --arg sid "$SERVICE_ID" --arg kw "$PROBE_PATH" --arg s "$START_MIN" --arg e "$END_MIN" '{
                query: "query($kw: String, $sid: ID!, $s: String!, $e: String!){ findEndpoint(keyword: $kw, serviceId: $sid, limit: 20, duration: {start: $s, end: $e, step: MINUTE}){ id name } }",
                variables: {kw: $kw, sid: $sid, s: $s, e: $e}
            }')")"
        ENDPOINT_ID="$(printf '%s' "$EP_RESP" | jq -r --arg kw "$PROBE_PATH" \
            '[.data.findEndpoint[]? | select(.name | contains($kw))][0].id // empty')"
    fi
    [ -n "$ENDPOINT_ID" ] || { [ "$(date +%s)" -lt "$DEADLINE" ] && sleep 2 && continue || break; }

    TRACE_RESP="$(oap_query "$(jq -n \
        --arg sid "$SERVICE_ID" --arg eid "$ENDPOINT_ID" --arg s "$START_MIN" --arg e "$END_MIN" '{
            query: "query($sid: ID!, $eid: ID!, $s: String!, $e: String!){ queryTraces(condition: {serviceId: $sid, endpointId: $eid, queryDuration: {start: $s, end: $e, step: MINUTE}, traceState: ALL, queryOrder: BY_START_TIME, paging: {pageNum: 1, pageSize: 10}}){ traces { spans { traceId serviceCode endpointName startTime } } } }",
            variables: {sid: $sid, eid: $eid, s: $s, e: $e}
        }')")"
    if printf '%s' "$TRACE_RESP" | jq -e '.data.queryTraces.traces | length > 0' >/dev/null 2>&1; then
        SW_TRACE="$(printf '%s' "$TRACE_RESP" | jq -r '
            [.data.queryTraces.traces[]] | sort_by([.spans[].startTime] | min) | last | .spans[0].traceId // empty')"
        [ -n "$SW_TRACE" ] && break
    fi
    [ "$(date +%s)" -lt "$DEADLINE" ] || break
    sleep 2
done

if [ -z "$SW_TRACE" ]; then
    printf '[FAIL] SkyWalking 在 %s ~ %s 窗口内没有查到 %s 的链路\n' \
        "$START_MIN" "$END_MIN" "$PROBE_PATH" >&2
    if [ -n "$(printf '%s' "$EP_RESP" | jq -r '.data.findEndpoint[]?.name' 2>/dev/null)" ]; then
        printf '  已登记的同服务端点：%s\n' \
            "$(printf '%s' "$EP_RESP" | jq -r '[.data.findEndpoint[]?.name] | join(", ")')" >&2
    fi
    exit 1
fi

TRACE_RESP="$(oap_query "$(jq -n \
    --arg sid "$SERVICE_ID" --arg eid "$ENDPOINT_ID" --arg s "$START_MIN" --arg e "$END_MIN" '{
        query: "query($sid: ID!, $eid: ID!, $s: String!, $e: String!){ queryTraces(condition: {serviceId: $sid, endpointId: $eid, queryDuration: {start: $s, end: $e, step: MINUTE}, traceState: ALL, queryOrder: BY_START_TIME, paging: {pageNum: 1, pageSize: 10}}){ traces { spans { traceId serviceCode endpointName } } } }",
        variables: {sid: $sid, eid: $eid, s: $s, e: $e}
    }')")"

OBSERVED="$(printf '%s' "$TRACE_RESP" | jq -r \
    --arg tid "$SW_TRACE" '[.data.queryTraces.traces[] | select(any(.spans[]; .traceId == $tid)) | .spans[].serviceCode] | unique | .[]')"
printf '  SkyWalking TraceID：%s\n' "$SW_TRACE"
printf '  SkyWalking 链路服务：%s\n' "$(printf '%s' "$OBSERVED" | paste -sd, -)"

MISSING=""
IFS=',' read -r -a EXPECT_ARR <<< "$EXPECT"
for svc in "${EXPECT_ARR[@]}"; do
    if ! printf '%s\n' "$OBSERVED" | grep -Fxq "$svc"; then
        MISSING="$MISSING $svc"
    fi
done
if [ -n "$MISSING" ]; then
    printf '[FAIL] SkyWalking 链路缺少服务：%s\n' "$MISSING" >&2
    exit 1
fi

printf '\n[PASS] 同一请求在 Tempo（%s）与 SkyWalking（%s）两侧均还原出完整链路\n' \
    "$TEMPO_TRACE" "$SW_TRACE"
