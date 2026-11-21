#!/bin/sh
#
# 初始化日志 Topic。
#
# kafka-init 在 broker 健康后运行；--if-not-exists 保证重复执行幂等：
# 已存在的 topic 不会被重建，也不会修改既有配置（分区数与保留期保持不变）。
# 保留期是「缓冲窗口」而不是长期存储，集中日志系统（ES）的保留期独立配置。
#
# Kafka 只提供 SSL；客户端配置由 KAFKA_SSL_* 环境变量生成。

set -u

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
TOPIC="${KAFKA_LOG_TOPIC:-iboot-logs}"
PARTITIONS="${KAFKA_LOG_PARTITIONS:-6}"
RETENTION_MS="${KAFKA_LOG_RETENTION_MS:-86400000}"
SECURITY_PROTOCOL="${KAFKA_SECURITY_PROTOCOL:-SSL}"
CLIENT_CONFIG="/tmp/kafka-client.properties"
COMMAND_CONFIG=""

if [ "$SECURITY_PROTOCOL" = "SSL" ]; then
    cat > "$CLIENT_CONFIG" <<EOF
security.protocol=SSL
ssl.truststore.location=${KAFKA_SSL_TRUSTSTORE_LOCATION:-/etc/kafka/secrets/truststore.p12}
ssl.truststore.password=${KAFKA_SSL_TRUSTSTORE_PASSWORD:-changeit}
ssl.truststore.type=${KAFKA_SSL_TRUSTSTORE_TYPE:-PKCS12}
ssl.endpoint.identification.algorithm=HTTPS
EOF
    COMMAND_CONFIG="--command-config $CLIENT_CONFIG"
fi

/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" $COMMAND_CONFIG \
    --create --if-not-exists --topic "$TOPIC" \
    --partitions "$PARTITIONS" --replication-factor 1 \
    --config "retention.ms=$RETENTION_MS" || exit 1

/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" $COMMAND_CONFIG \
    --describe --topic "$TOPIC" || exit 1

printf '[init] Kafka topic %s 就绪（分区 %s，保留 %s ms）\n' \
    "$TOPIC" "$PARTITIONS" "$RETENTION_MS"
