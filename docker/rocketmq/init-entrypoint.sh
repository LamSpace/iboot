#!/bin/sh
# RocketMQ topic 初始化入口：先写入受管 tools.yml，再执行 topic 脚本。
set -eu

: "${ROCKETMQ_ADMIN_ACCESS_KEY:?ROCKETMQ_ADMIN_ACCESS_KEY 必须在 .env 中设置}"
: "${ROCKETMQ_ADMIN_SECRET_KEY:?ROCKETMQ_ADMIN_SECRET_KEY 必须在 .env 中设置}"

ROCKETMQ_HOME="${ROCKETMQ_HOME:-/home/rocketmq/rocketmq-5.5.0}"
cat > "$ROCKETMQ_HOME/conf/tools.yml" <<TOOLS
accessKey: ${ROCKETMQ_ADMIN_ACCESS_KEY}
secretKey: ${ROCKETMQ_ADMIN_SECRET_KEY}
TOOLS

exec sh /home/rocketmq/init-topics.sh
