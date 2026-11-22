#!/bin/sh
# RocketMQ Broker 入口：从环境生成带 ACL 的 broker.conf 与 mqadmin tools.yml。
#
# 口令只存在于运行时文件；Broker 的 ACL 元数据持久化到 /home/rocketmq/auth。
set -eu

: "${ROCKETMQ_ADMIN_ACCESS_KEY:?ROCKETMQ_ADMIN_ACCESS_KEY 必须在 .env 中设置}"
: "${ROCKETMQ_ADMIN_SECRET_KEY:?ROCKETMQ_ADMIN_SECRET_KEY 必须在 .env 中设置}"
NAMESRV_ADDR="${NAMESRV_ADDR:-rocketmq-namesrv:9876}"
AUTH_DIR="${ROCKETMQ_AUTH_DIR:-/home/rocketmq/auth}"
ROCKETMQ_HOME="${ROCKETMQ_HOME:-/home/rocketmq/rocketmq-5.5.0}"

mkdir -p "$AUTH_DIR"
cat > /tmp/broker.conf <<CONF
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
deleteWhen = 04
fileReservedTime = 48
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH
aclEnable = true
authenticationEnabled = true
authenticationProvider = org.apache.rocketmq.auth.authentication.provider.DefaultAuthenticationProvider
authenticationMetadataProvider = org.apache.rocketmq.auth.authentication.provider.LocalAuthenticationMetadataProvider
authorizationEnabled = true
authorizationProvider = org.apache.rocketmq.auth.authorization.provider.DefaultAuthorizationProvider
authorizationMetadataProvider = org.apache.rocketmq.auth.authorization.provider.LocalAuthorizationMetadataProvider
initAuthenticationUser = {"username":"${ROCKETMQ_ADMIN_ACCESS_KEY}","password":"${ROCKETMQ_ADMIN_SECRET_KEY}"}
innerClientAuthenticationCredentials = {"accessKey":"${ROCKETMQ_ADMIN_ACCESS_KEY}","secretKey":"${ROCKETMQ_ADMIN_SECRET_KEY}"}
authConfigPath = ${AUTH_DIR}
CONF

cat > "$ROCKETMQ_HOME/conf/tools.yml" <<TOOLS
accessKey: ${ROCKETMQ_ADMIN_ACCESS_KEY}
secretKey: ${ROCKETMQ_ADMIN_SECRET_KEY}
TOOLS

cd "$ROCKETMQ_HOME/bin"
exec sh mqbroker -c /tmp/broker.conf -n "$NAMESRV_ADDR"
