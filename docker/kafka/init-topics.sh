#!/bin/sh
#
# 初始化日志 Topic 与最小权限 ACL。
#
# kafka-init 在 broker 健康后运行；--if-not-exists 保证重复执行幂等：
# 已存在的 topic 不会被重建，也不会修改既有配置（分区数与保留期保持不变）。
# 保留期是「缓冲窗口」而不是长期存储，集中日志系统（ES）的保留期独立配置。
#
# Kafka 只提供 SASL_SSL；客户端配置由 KAFKA_* 环境变量生成。
# 本任务使用 iboot_tools 超级用户，它是唯一允许创建 topic 与 ACL 的运维账号。

set -u

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
TOPIC="${KAFKA_LOG_TOPIC:-iboot-logs}"
GROUP="${KAFKA_LOGSTASH_GROUP:-iboot-logstash}"
PARTITIONS="${KAFKA_LOG_PARTITIONS:-6}"
RETENTION_MS="${KAFKA_LOG_RETENTION_MS:-86400000}"
SECURITY_PROTOCOL="${KAFKA_SECURITY_PROTOCOL:-SASL_SSL}"
SASL_MECHANISM="${KAFKA_SASL_MECHANISM:-PLAIN}"
SASL_USERNAME="${KAFKA_SASL_USERNAME:-iboot_tools}"
SASL_PASSWORD="${KAFKA_SASL_PASSWORD:-}"
CLIENT_CONFIG="/tmp/kafka-client.properties"
COMMAND_CONFIG=""

if [ "$SECURITY_PROTOCOL" = "SASL_SSL" ] && [ -z "$SASL_PASSWORD" ]; then
    printf '[init] 缺少 KAFKA_SASL_PASSWORD，拒绝以匿名方式连接\n' >&2
    exit 1
fi

cat > "$CLIENT_CONFIG" <<EOF
security.protocol=${SECURITY_PROTOCOL}
ssl.truststore.location=${KAFKA_SSL_TRUSTSTORE_LOCATION:-/etc/kafka/secrets/truststore.p12}
ssl.truststore.password=${KAFKA_SSL_TRUSTSTORE_PASSWORD:-changeit}
ssl.truststore.type=${KAFKA_SSL_TRUSTSTORE_TYPE:-PKCS12}
ssl.endpoint.identification.algorithm=HTTPS
EOF
if [ "$SECURITY_PROTOCOL" = "SASL_SSL" ]; then
    cat >> "$CLIENT_CONFIG" <<EOF
sasl.mechanism=${SASL_MECHANISM}
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="${SASL_USERNAME}" password="${SASL_PASSWORD}";
EOF
fi
COMMAND_CONFIG="--command-config $CLIENT_CONFIG"

/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" $COMMAND_CONFIG \
    --create --if-not-exists --topic "$TOPIC" \
    --partitions "$PARTITIONS" --replication-factor 1 \
    --config "retention.ms=$RETENTION_MS" || exit 1

/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" $COMMAND_CONFIG \
    --describe --topic "$TOPIC" || exit 1

ACLS="/opt/kafka/bin/kafka-acls.sh --bootstrap-server $BOOTSTRAP $COMMAND_CONFIG"

# 业务服务只允许写日志 topic；Logstash 只允许读该 topic 与自己的消费组。
$ACLS --add --allow-principal User:iboot_log \
    --operation Write --operation Describe --topic "$TOPIC" || exit 1
$ACLS --add --allow-principal User:iboot_logstash \
    --operation Read --operation Describe --topic "$TOPIC" || exit 1
$ACLS --add --allow-principal User:iboot_logstash \
    --operation Read --operation Describe --group "$GROUP" || exit 1

printf '[init] Kafka topic %s 与 ACL 就绪（分区 %s，保留 %s ms）\n' \
    "$TOPIC" "$PARTITIONS" "$RETENTION_MS"
