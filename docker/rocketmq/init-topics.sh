#!/bin/sh
#
# 初始化 RocketMQ Topic 与消费者组。
#
# Worker 在 Broker 端口可连接时可能尚未完成 NameServer 注册，此时 mqadmin 会报
# “Make sure the specified clusterName exists” 但退出码仍为 0，仅靠退出码无法判断
# 成败。因此这里按输出中的 success 判断，并在失败时有界重试；最后校验 topic 路由
# 确实写入 NameServer，避免容器以 0 退出却留下缺失的 topic。

set -u

cd "${ROCKETMQ_HOME:-/home/rocketmq/rocketmq-5.3.1}/bin" || exit 1

NAMESRV="${NAMESRV_ADDR:-rocketmq-namesrv:9876}"
TOPIC="${TOPIC:-iboot-notification}"
GROUP="${GROUP:-iboot-notify-consumer}"
MAX_ATTEMPTS=30
RETRY_INTERVAL=3

# 执行 mqadmin 直到输出出现 success；失败时按固定间隔重试。
# mqadmin 的退出码不反映业务失败，只能检查输出。
run_until_success() {
    desc="$1"
    shift
    attempt=1
    while [ "$attempt" -le "$MAX_ATTEMPTS" ]; do
        output="$(sh "$@" 2>&1)"
        printf '%s\n' "$output"
        if printf '%s' "$output" | grep -q 'success'; then
            return 0
        fi
        printf '[init] %s 未就绪（第 %s/%s 次），%s 秒后重试\n' \
            "$desc" "$attempt" "$MAX_ATTEMPTS" "$RETRY_INTERVAL"
        attempt=$((attempt + 1))
        sleep "$RETRY_INTERVAL"
    done
    printf '[init] %s 超过最大重试次数\n' "$desc" >&2
    return 1
}

run_until_success '创建 Topic' mqadmin updateTopic \
    -n "$NAMESRV" -c DefaultCluster -t "$TOPIC" -r 4 -w 4 || exit 1

run_until_success '创建消费者组' mqadmin updateSubGroup \
    -n "$NAMESRV" -c DefaultCluster -g "$GROUP" || exit 1

# 只有 topicRoute 返回 brokerDatas，才说明路由已写入 NameServer。
route="$(sh mqadmin topicRoute -n "$NAMESRV" -t "$TOPIC" 2>&1)"
printf '%s\n' "$route"
if ! printf '%s' "$route" | grep -q 'brokerDatas'; then
    printf '[init] %s 的路由未在 NameServer 注册\n' "$TOPIC" >&2
    exit 1
fi

printf '[init] RocketMQ topic %s 与消费者组 %s 就绪\n' "$TOPIC" "$GROUP"
