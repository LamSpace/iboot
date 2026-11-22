#!/bin/sh
# 幂等创建 RocketMQ 应用账号与最小 ACL。
#
# 运行在 apache/rocketmq 镜像中；mqadmin 读取入口脚本生成的 tools.yml。
set -eu

: "${ROCKETMQ_ADMIN_ACCESS_KEY:?ROCKETMQ_ADMIN_ACCESS_KEY 必须在 .env 中设置}"
: "${ROCKETMQ_ADMIN_SECRET_KEY:?ROCKETMQ_ADMIN_SECRET_KEY 必须在 .env 中设置}"
: "${ROCKETMQ_WORKFLOW_ACCESS_KEY:?ROCKETMQ_WORKFLOW_ACCESS_KEY 必须在 .env 中设置}"
: "${ROCKETMQ_WORKFLOW_SECRET_KEY:?ROCKETMQ_WORKFLOW_SECRET_KEY 必须在 .env 中设置}"
: "${ROCKETMQ_NOTIFY_ACCESS_KEY:?ROCKETMQ_NOTIFY_ACCESS_KEY 必须在 .env 中设置}"
: "${ROCKETMQ_NOTIFY_SECRET_KEY:?ROCKETMQ_NOTIFY_SECRET_KEY 必须在 .env 中设置}"

ROCKETMQ_HOME="${ROCKETMQ_HOME:-/home/rocketmq/rocketmq-5.5.0}"
BROKER="${ROCKETMQ_BROKER_ADDR:-rocketmq-broker:10911}"
TOPIC="${ROCKETMQ_TOPIC:-iboot-notification}"
GROUP="${ROCKETMQ_GROUP:-iboot-notify-consumer}"

cat > "$ROCKETMQ_HOME/conf/tools.yml" <<TOOLS
accessKey: ${ROCKETMQ_ADMIN_ACCESS_KEY}
secretKey: ${ROCKETMQ_ADMIN_SECRET_KEY}
TOOLS

cd "$ROCKETMQ_HOME/bin"

mq() {
    sh mqadmin "$@"
}

wait_broker() {
    i=0
    while [ "$i" -lt 60 ]; do
        if mq listUser -b "$BROKER" 2>&1 | grep -q "$ROCKETMQ_ADMIN_ACCESS_KEY"; then
            return 0
        fi
        i=$((i + 1))
        sleep 2
    done
    return 1
}

wait_broker || { echo '[acl-init] Broker ACL 未就绪' >&2; exit 1; }

# 先删用户（连带旧 ACL），再创建，保证重复执行结果一致。
for user in "$ROCKETMQ_WORKFLOW_ACCESS_KEY" "$ROCKETMQ_NOTIFY_ACCESS_KEY"; do
    mq deleteAcl -b "$BROKER" -s "User:$user" >/dev/null 2>&1 || true
    mq deleteUser -b "$BROKER" -u "$user" >/dev/null 2>&1 || true
done
mq createUser -b "$BROKER" -u "$ROCKETMQ_WORKFLOW_ACCESS_KEY" -p "$ROCKETMQ_WORKFLOW_SECRET_KEY" -t NORMAL >/dev/null
mq createUser -b "$BROKER" -u "$ROCKETMQ_NOTIFY_ACCESS_KEY" -p "$ROCKETMQ_NOTIFY_SECRET_KEY" -t NORMAL >/dev/null

mq createAcl -b "$BROKER" -s "User:$ROCKETMQ_WORKFLOW_ACCESS_KEY" \
    -r "Topic:$TOPIC" -a PUB -d ALLOW >/dev/null
mq createAcl -b "$BROKER" -s "User:$ROCKETMQ_NOTIFY_ACCESS_KEY" \
    -r "Topic:$TOPIC" -a "SUB,GET" -d ALLOW >/dev/null
mq createAcl -b "$BROKER" -s "User:$ROCKETMQ_NOTIFY_ACCESS_KEY" \
    -r "Topic:%RETRY%$GROUP" -a "SUB,GET" -d ALLOW >/dev/null
mq createAcl -b "$BROKER" -s "User:$ROCKETMQ_NOTIFY_ACCESS_KEY" \
    -r "Group:$GROUP" -a "SUB,GET" -d ALLOW >/dev/null

printf '[acl-init] 账号与 ACL 已就绪：%s、%s\n' \
    "$ROCKETMQ_WORKFLOW_ACCESS_KEY" "$ROCKETMQ_NOTIFY_ACCESS_KEY"
