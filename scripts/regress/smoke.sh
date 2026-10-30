#!/usr/bin/env bash
# 关键链路端到端冒烟：登录、用户信息、文件上传下载、实时推送、数据备份、审批流转。
#
# 用法：smoke.sh
# 环境变量：REG_BASE（默认 http://localhost:8080）、REG_USER、REG_PASS

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

WORK="$(mktemp -d)"
SSE_PID=""
cleanup() {
    if [ -n "$SSE_PID" ]; then kill "$SSE_PID" 2>/dev/null || true; fi
    rm -rf "$WORK"
}
trap cleanup EXIT

reg_log "== 1. 登录 =="
reg_login
reg_pass "使用 $REG_USER 登录成功"

reg_log "== 2. 用户信息 =="
profile=$(reg_get /api/user/profile)
reg_expect_ok "读取当前用户信息" "$profile"
reg_expect_jq "返回的用户名为 $REG_USER" ".data.username == \"$REG_USER\"" "$profile"

reg_log "== 3. 文件上传与下载 =="
echo "iBoot 回归冒烟 $(date +%s)" > "$WORK/smoke.txt"
upload=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
    -F "file=@$WORK/smoke.txt" -F "fileCategory=regress" \
    "$REG_BASE/api/file/upload" | reg_unwrap)
reg_expect_ok "上传文件" "$upload"
file_id=$(printf '%s' "$upload" | jq -r '.data.id // empty')
if [ -n "$file_id" ]; then
    download=$(reg_get "/api/file/download-url/$file_id")
    reg_expect_ok "获取文件下载地址" "$download"
else
    reg_fail "上传未返回文件 ID"
fi

reg_log "== 4. 实时推送 =="
message=$(reg_post /api/message '{"title":"回归冒烟消息","content":"回归冒烟内容","messageType":"1","priority":"1","receiverType":"0"}')
reg_expect_ok "创建站内消息" "$message"
message_id=$(printf '%s' "$message" | jq -r '.data.id // empty')

curl -sN --max-time 15 -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_BASE/api/push/connect?t=$REG_TOKEN" > "$WORK/sse.txt" 2>/dev/null &
SSE_PID=$!
sleep 2

if [ -n "$message_id" ] && [ "$message_id" != "null" ]; then
    sent=$(reg_post /api/message/send "{\"messageId\":$message_id,\"userIds\":[1]}")
    reg_expect_ok "把消息发送给当前用户" "$sent"
else
    reg_fail "消息创建未返回 ID，跳过发送"
fi

sleep 4
if grep -q '^data:' "$WORK/sse.txt"; then
    reg_pass "SSE 连接收到推送事件"
else
    reg_fail "SSE 连接未收到事件（$(head -c 200 "$WORK/sse.txt" | tr '\n' ' ')）"
fi

reg_log "== 5. 数据备份 =="
backup=$(reg_post /api/monitor/backup/records)
reg_expect_ok "触发一次手动备份" "$backup"

reg_log "== 6. 审批流转 =="
reg_ensure_user "regress_approver" "回归审批人" 2
approver_id="$REG_LAST_ID"
leave=$(reg_post /api/approval/leave "{\"approverId\":$approver_id,\"leaveType\":\"annual\",\"startTime\":\"2026-11-02T09:00:00\",\"endTime\":\"2026-11-02T18:00:00\",\"reason\":\"回归冒烟\"}")
reg_expect_ok "发起一条请假申请" "$leave"
mine=$(reg_get /api/approval/mine)
reg_expect_ok "查询我发起的审批" "$mine"
reg_expect_jq "审批列表中包含刚发起的申请" '[.data.data[]? | select(.reason == "回归冒烟")] | length > 0' "$mine"

reg_summary
