#!/usr/bin/env bash
# RocketMQ ACL 2.0 回归：账号、Topic 权限、发布/消费身份与负向拒绝。
#
# 前置：Broker/Proxy 使用 ACL 2.0 启动，rocketmq-acl-init 已执行，workflow/notify 已就绪。
# 环境变量：ROCKETMQ_ADMIN_*、ROCKETMQ_WORKFLOW_*、ROCKETMQ_NOTIFY_*。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$HERE/lib.sh"

ROOT="$(cd "$HERE/../.." && pwd)"
if [ -f "$ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT/.env"
    set +a
fi

ADMIN_KEY="${ROCKETMQ_ADMIN_ACCESS_KEY:-}"
ADMIN_SECRET="${ROCKETMQ_ADMIN_SECRET_KEY:-}"
WORKFLOW_KEY="${ROCKETMQ_WORKFLOW_ACCESS_KEY:-}"
WORKFLOW_SECRET="${ROCKETMQ_WORKFLOW_SECRET_KEY:-}"
NOTIFY_KEY="${ROCKETMQ_NOTIFY_ACCESS_KEY:-}"
NOTIFY_SECRET="${ROCKETMQ_NOTIFY_SECRET_KEY:-}"
BROKER_ADDR=127.0.0.1:10911
TOPIC=iboot-notification
GROUP=iboot-notify-consumer
IMAGE=apache/rocketmq:5.5.0

if ! docker inspect iboot-rocketmq-broker >/dev/null 2>&1; then
    reg_log "  [SKIP] RocketMQ Broker 未运行"
    reg_summary
    exit 0
fi

mq_admin() {
    docker exec iboot-rocketmq-broker sh -c \
        'cd /home/rocketmq/rocketmq-5.5.0/bin && exec sh mqadmin "$@"' sh "$@"
}

# 在 Broker 网络命名空间内用指定凭据生成 tools.yml 并执行 mqadmin。
run_mq_as() {
    local key="$1" secret="$2"
    shift 2
    docker run --rm --network container:iboot-rocketmq-broker "$IMAGE" sh -c '
        printf "accessKey: %s\nsecretKey: %s\n" "$1" "$2" > /home/rocketmq/rocketmq-5.5.0/conf/tools.yml
        shift 2
        cd /home/rocketmq/rocketmq-5.5.0/bin
        exec sh mqadmin "$@"
    ' sh "$key" "$secret" "$@"
}

reg_log "== 1. 账号与 ACL 元数据 =="
users="$(mq_admin listUser -b "$BROKER_ADDR" 2>/dev/null || true)"
for user in "$WORKFLOW_KEY" "$NOTIFY_KEY"; do
    if printf '%s\n' "$users" | grep -qw "$user"; then
        reg_pass "Broker 存在账号 $user"
    else
        reg_fail "Broker 缺少账号 $user"
    fi
done
workflow_acl="$(mq_admin getAcl -b "$BROKER_ADDR" -s "User:$WORKFLOW_KEY" 2>/dev/null || true)"
if printf '%s' "$workflow_acl" | grep -q "$TOPIC" && printf '%s' "$workflow_acl" | grep -qi 'pub'; then
    reg_pass "workflow 账号拥有 $TOPIC 的 PUB 授权"
else
    reg_fail "workflow 账号缺少 $TOPIC 的 PUB 授权"
fi
notify_acl="$(mq_admin getAcl -b "$BROKER_ADDR" -s "User:$NOTIFY_KEY" 2>/dev/null || true)"
if printf '%s' "$notify_acl" | grep -q "$TOPIC" && printf '%s' "$notify_acl" | grep -qi 'sub'; then
    reg_pass "notify 账号拥有 $TOPIC 的 SUB 授权"
else
    reg_fail "notify 账号缺少 $TOPIC 的 SUB 授权"
fi

reg_log "== 2. 发布身份 =="
if run_mq_as "$WORKFLOW_KEY" "$WORKFLOW_SECRET" sendMessage \
    -n rocketmq-namesrv:9876 -t "$TOPIC" -p 'iboot-acl-probe' 2>&1 | grep -qE 'SEND_OK|sendResult'; then
    reg_pass "workflow 账号可以发布 $TOPIC"
else
    reg_fail "workflow 账号无法发布 $TOPIC"
fi
notify_publish="$(run_mq_as "$NOTIFY_KEY" "$NOTIFY_SECRET" sendMessage \
    -n rocketmq-namesrv:9876 -t "$TOPIC" -p 'iboot-acl-probe' 2>&1 || true)"
if printf '%s' "$notify_publish" | grep -q 'SEND_OK'; then
    reg_fail "notify 账号可以发布 $TOPIC"
else
    reg_pass "notify 账号不能发布 $TOPIC"
fi
anon_publish="$(run_mq_as "" "" sendMessage \
    -n rocketmq-namesrv:9876 -t "$TOPIC" -p 'iboot-acl-probe' 2>&1 || true)"
if printf '%s' "$anon_publish" | grep -q 'SEND_OK'; then
    reg_fail "无凭据客户端可以发布 $TOPIC"
else
    reg_pass "无凭据客户端被拒绝"
fi
wrong_publish="$(run_mq_as "$WORKFLOW_KEY" "wrong-secret" sendMessage \
    -n rocketmq-namesrv:9876 -t "$TOPIC" -p 'iboot-acl-probe' 2>&1 || true)"
if printf '%s' "$wrong_publish" | grep -q 'SEND_OK'; then
    reg_fail "RocketMQ 错误凭据仍可发布"
else
    reg_pass "RocketMQ 错误凭据被拒绝"
fi

reg_log "== 3. 消费身份 =="
notify_consume="$(run_mq_as "$NOTIFY_KEY" "$NOTIFY_SECRET" consumeMessage \
    -t "$TOPIC" -g "$GROUP" -c 1 -n rocketmq-namesrv:9876 2>&1 || true)"
if printf '%s' "$notify_consume" | grep -q 'Consume ok'; then
    reg_pass "notify 账号可以订阅消费组 $GROUP"
elif printf '%s' "$notify_acl" | grep -q "$GROUP"; then
    # 没有可消费消息时 mqadmin 不会输出 Consume ok；以 ACL 元数据为准。
    reg_pass "notify 账号拥有消费组 $GROUP 的授权"
else
    reg_fail "notify 账号缺少消费组 $GROUP 的授权"
fi
workflow_consume="$(run_mq_as "$WORKFLOW_KEY" "$WORKFLOW_SECRET" consumeMessage \
    -t "$TOPIC" -g "$GROUP" -c 1 -n rocketmq-namesrv:9876 2>&1 || true)"
if printf '%s' "$workflow_consume" | grep -q 'Consume ok'; then
    reg_fail "workflow 账号可以消费 $TOPIC"
else
    reg_pass "workflow 账号不能消费 $TOPIC"
fi

reg_log "== 4. 应用凭据隔离 =="
service_env() {
    local name="$1"
    if docker inspect "$name" >/dev/null 2>&1; then
        docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$name" 2>/dev/null || true
        return
    fi
    local pid
    pid="$(pgrep -f "^java .*$name-1\\.0\\.0\\.jar" | head -1 || true)"
    [ -n "$pid" ] && tr '\0' '\n' < "/proc/$pid/environ" 2>/dev/null || true
}

for pair in "iboot-workflow:$WORKFLOW_KEY" "iboot-notify:$NOTIFY_KEY"; do
    svc="${pair%%:*}"
    key="${pair##*:}"
    env_dump="$(service_env "$svc")"
    if [ -z "$env_dump" ]; then
        reg_log "  [SKIP] $svc 未运行"
        continue
    fi
    if printf '%s\n' "$env_dump" | grep -qx "IBOOT_ROCKETMQ_ACCESS_KEY=$key"; then
        reg_pass "$svc 使用自己的 RocketMQ 凭据"
    else
        reg_fail "$svc 未使用自己的 RocketMQ 凭据"
    fi
    if printf '%s\n' "$env_dump" | grep -qE '^(ROCKETMQ_ADMIN_SECRET_KEY|ROCKETMQ_WORKFLOW_SECRET_KEY|ROCKETMQ_NOTIFY_SECRET_KEY)=..'; then
        reg_fail "$svc 持有其它 RocketMQ 凭据"
    else
        reg_pass "$svc 不持有运维或其它应用凭据"
    fi
done

reg_summary
