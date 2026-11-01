#!/usr/bin/env bash
# 关键链路端到端冒烟：登录、用户信息、文件上传下载、实时推送、定时任务、数据备份、审批流转。
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

reg_log "== 3. 文件上传与下载（经网关，/api/file/** 已迁至 iboot-file） =="
REG_GATEWAY="${REG_GATEWAY:-http://localhost:8088}"
echo "iBoot 回归冒烟 $(date +%s)" > "$WORK/smoke.txt"
upload=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
    -F "file=@$WORK/smoke.txt" -F "fileCategory=regress" \
    "$REG_GATEWAY/api/file/upload" | reg_unwrap)
reg_expect_ok "经网关上传文件" "$upload"
file_id=$(printf '%s' "$upload" | jq -r '.data.id // empty')
if [ -n "$file_id" ]; then
    file_list=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
        "$REG_GATEWAY/api/file/list?pageNum=1&pageSize=50" | reg_unwrap)
    reg_expect_ok "经网关查询文件列表" "$file_list"
    reg_expect_jq "列表包含刚上传的文件" "[.data.data[]? | select(.id == $file_id)] | length == 1" "$file_list"
    download=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
        "$REG_GATEWAY/api/file/download-url/$file_id" | reg_unwrap)
    reg_expect_ok "经网关获取下载地址" "$download"
    if curl -sS -f -H "Authorization: Bearer $REG_TOKEN" \
        "$REG_GATEWAY/api/file/download/$file_id" -o "$WORK/downloaded.txt"; then
        reg_pass "经网关下载文件内容"
    else
        reg_fail "经网关下载文件失败"
    fi
    removed=$(curl -sS -X DELETE -H "Authorization: Bearer $REG_TOKEN" \
        "$REG_GATEWAY/api/file/$file_id" | reg_unwrap)
    reg_expect_ok "经网关删除文件" "$removed"
else
    reg_fail "上传未返回文件 ID"
fi

reg_log "== 4. 实时推送（经网关，消息与 SSE 已迁至 iboot-notify） =="
message=$(curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" -H 'Content-Type: application/json' \
    -d '{"title":"回归冒烟消息","content":"回归冒烟内容","messageType":"1","priority":"1","receiverType":"0"}' \
    "$REG_GATEWAY/api/message" | reg_unwrap)
reg_expect_ok "经网关创建站内消息" "$message"
message_id=$(printf '%s' "$message" | jq -r '.data.id // empty')

curl -sN --max-time 15 -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/push/connect?t=$REG_TOKEN" > "$WORK/sse.txt" 2>/dev/null &
SSE_PID=$!
sleep 2

if [ -n "$message_id" ] && [ "$message_id" != "null" ]; then
    sent=$(curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" -H 'Content-Type: application/json' \
        -d "{\"messageId\":$message_id,\"userIds\":[1]}" "$REG_GATEWAY/api/message/send" | reg_unwrap)
    reg_expect_ok "经网关把消息发送给当前用户" "$sent"
else
    reg_fail "消息创建未返回 ID，跳过发送"
fi

sleep 4
if grep -q '^data:' "$WORK/sse.txt"; then
    reg_pass "经网关的 SSE 连接收到推送事件"
else
    reg_fail "经网关的 SSE 连接未收到事件（$(head -c 200 "$WORK/sse.txt" | tr '\n' ' ')）"
fi
if grep -q '回归冒烟消息' "$WORK/sse.txt"; then
    reg_pass "SSE 事件内容包含刚发送的消息标题"
else
    reg_fail "SSE 事件内容缺少消息标题"
fi

reg_log "== 4b. 通知投递（经网关，/api/notification/** 已迁至 iboot-notify） =="
deliveries=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/notification/delivery?pageNum=1&pageSize=10" | reg_unwrap)
reg_expect_ok "经网关查询通知投递记录" "$deliveries"
# 开发环境的渠道都可用，天然不会产生失败投递；造一条 DEAD 记录覆盖人工重试路径。
# 不复用库里已有的 FAILED 记录：通知服务的定时重试会在一分钟内把它重置，造成 409 竞态。
# 重投走真实站内渠道，目标状态是 SUCCESS，而不是停留在 PENDING。
retry_id=$(docker exec iboot-mysql mysql -uroot -proot -N -e "
    INSERT INTO iboot_notify.sys_notification_delivery
        (event_id, event_type, channel, recipient_key, recipient_id, template_code, title, content,
         status, attempts, max_attempts, create_time)
    VALUES ('regress-retry-$(date +%s)', 'APPROVAL_TODO', 'IN_APP', 'USER:1', 1,
            'APPROVAL_TODO', '回归重试', '回归重试内容', 'DEAD', 5, 5, NOW());
    SELECT LAST_INSERT_ID();" 2>/dev/null | tail -1)
if [ -n "$retry_id" ]; then
    retried=$(curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" \
        "$REG_GATEWAY/api/notification/delivery/$retry_id/retry" | reg_unwrap)
    reg_expect_ok "经网关人工重试失败投递" "$retried"
    retry_status=""
    for _ in $(seq 1 15); do
        retry_status=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
            "$REG_GATEWAY/api/notification/delivery?pageNum=1&pageSize=50" | reg_unwrap \
            | jq -r --argjson id "$retry_id" '[.data.data[]? | select(.id == $id)][0].status // empty')
        [ "$retry_status" = "PENDING" ] || [ "$retry_status" = "SENDING" ] || break
        sleep 1
    done
    case "$retry_status" in
        PENDING|SENDING) reg_pass "重试后记录从 DEAD 复位为 $retry_status" ;;
        SUCCESS)         reg_pass "重试后记录从 DEAD 重投成功" ;;
        *)               reg_fail "重试后记录状态为 $retry_status，未完成重置与重新投递" ;;
    esac
else
    reg_fail "无法准备可重试的投递记录"
fi

reg_log "== 5. 定时任务与数据备份（经网关，已迁至 iboot-job） =="
job_body='{"jobName":"回归冒烟任务","jobGroup":"DEFAULT","invokeTarget":"sampleTask.noParams","cronExpression":"0 0 3 * * ?","status":0,"remark":"回归冒烟"}'
job_created=$(curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" -H 'Content-Type: application/json' \
    -d "$job_body" "$REG_GATEWAY/api/job" | reg_unwrap)
reg_expect_ok "经网关创建一条暂停任务" "$job_created"
job_id=$(printf '%s' "$job_created" | jq -r '.data.id // empty')
if [ -n "$job_id" ] && [ "$job_id" != "null" ]; then
    job_run=$(curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" \
        "$REG_GATEWAY/api/job/$job_id/run" | reg_unwrap)
    reg_expect_ok "经网关立即执行一次任务" "$job_run"
    job_log=""
    for _ in $(seq 1 10); do
        job_log=$(curl -sS -H "Authorization: Bearer $REG_TOKEN" \
            "$REG_GATEWAY/api/job/log/list?jobId=$job_id&pageNum=1&pageSize=10" | reg_unwrap)
        [ "$(printf '%s' "$job_log" | jq -r '.data.total // 0')" -gt 0 ] && break
        sleep 1
    done
    reg_expect_jq "执行日志中出现该任务的记录" '.data.total > 0' "$job_log"
    job_removed=$(curl -sS -X DELETE -H "Authorization: Bearer $REG_TOKEN" \
        "$REG_GATEWAY/api/job/$job_id" | reg_unwrap)
    reg_expect_ok "经网关删除任务" "$job_removed"
else
    reg_fail "创建任务未返回 ID"
fi

backup=$(curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" \
    "$REG_GATEWAY/api/monitor/backup/records" | reg_unwrap)
reg_expect_ok "经网关触发一次手动备份" "$backup"

reg_log "== 6. 审批流转 =="
reg_ensure_user "regress_approver" "回归审批人" 2
approver_id="$REG_LAST_ID"
leave=$(reg_post /api/approval/leave "{\"approverId\":$approver_id,\"leaveType\":\"annual\",\"startTime\":\"2026-11-02T09:00:00\",\"endTime\":\"2026-11-02T18:00:00\",\"reason\":\"回归冒烟\"}")
reg_expect_ok "发起一条请假申请" "$leave"
mine=$(reg_get /api/approval/mine)
reg_expect_ok "查询我发起的审批" "$mine"
reg_expect_jq "审批列表中包含刚发起的申请" '[.data.data[]? | select(.reason == "回归冒烟")] | length > 0' "$mine"

reg_log "== 7. 登出与会话撤销 =="
logout=$(curl -sS -X POST -H "Authorization: Bearer $REG_TOKEN" "$REG_AUTH_BASE/api/auth/logout" | reg_unwrap)
reg_expect_ok "调用登出接口" "$logout"
after_logout=$(reg_status /api/user/profile)
reg_expect_status "登出后令牌立即失效" "$after_logout" "401"

reg_log "== 8. 旧登录协议已下线 =="
# 接口已下线：路径不在契约基线里，这里只断言它不再返回成功
legacy_login_status=$(curl -sS -o /dev/null -w '%{http_code}' -X POST \
    -H 'Content-Type: application/json' -d '{}' "$REG_GATEWAY/api/auth/login")
if [ "$legacy_login_status" != "200" ]; then
    reg_pass "旧的用户名密码登录接口已不可用（HTTP $legacy_login_status）"
else
    reg_fail "旧的用户名密码登录接口仍然可用"
fi

legacy_body=$(curl -sS -H "Authorization: Bearer $(reg_legacy_token)" "$REG_BASE/api/user/profile")
reg_expect_jq "旧格式令牌返回令牌版本过期" '.code == 40101' "$legacy_body"

reg_summary
