#!/bin/sh
# RocketMQ Proxy 入口：生成带 ACL 与 TLS 的 rmq-proxy.json。
#
# Proxy 使用 Broker 的元数据校验调用方；内部客户端凭据必须与 Broker 运维账号一致。
set -eu

: "${ROCKETMQ_ADMIN_ACCESS_KEY:?ROCKETMQ_ADMIN_ACCESS_KEY 必须在 .env 中设置}"
: "${ROCKETMQ_ADMIN_SECRET_KEY:?ROCKETMQ_ADMIN_SECRET_KEY 必须在 .env 中设置}"
NAMESRV_ADDR="${NAMESRV_ADDR:-rocketmq-namesrv:9876}"

cat > /tmp/rmq-proxy.json <<JSON
{
  "rocketMQClusterName": "DefaultCluster",
  "useEndpointPortFromRequest": true,
  "tlsTestModeEnable": false,
  "tlsKeyPath": "/certs/portal-key.pem",
  "tlsCertPath": "/certs/portal.pem",
  "authenticationEnabled": true,
  "authenticationProvider": "org.apache.rocketmq.auth.authentication.provider.DefaultAuthenticationProvider",
  "authenticationMetadataProvider": "org.apache.rocketmq.proxy.auth.ProxyAuthenticationMetadataProvider",
  "authorizationEnabled": true,
  "authorizationProvider": "org.apache.rocketmq.auth.authorization.provider.DefaultAuthorizationProvider",
  "authorizationMetadataProvider": "org.apache.rocketmq.proxy.auth.ProxyAuthorizationMetadataProvider",
  "innerClientAuthenticationCredentials": "{\"accessKey\":\"${ROCKETMQ_ADMIN_ACCESS_KEY}\",\"secretKey\":\"${ROCKETMQ_ADMIN_SECRET_KEY}\"}",
  "enableAclRpcHookForClusterMode": true
}
JSON

cd "${ROCKETMQ_HOME:-/home/rocketmq/rocketmq-5.5.0}/bin"
exec sh mqproxy -n "$NAMESRV_ADDR" -pc /tmp/rmq-proxy.json
