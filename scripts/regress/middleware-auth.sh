#!/usr/bin/env bash
# 中间件账号鉴权回归：MySQL 最小权限、Redis ACL、Silo 受管凭据、Nacos 鉴权。
#
# 前置：中间件已启动，根目录 .env 或环境变量提供受管凭据。
# 环境变量：MYSQL_ROOT_PASSWORD、MYSQL_APP_PASSWORD、MYSQL_EXPORTER_PASSWORD、
#          REDIS_USERNAME、REDIS_PASSWORD、REDIS_EXPORTER_PASSWORD、
#          KAFKA_LOG_PASSWORD、KAFKA_LOGSTASH_PASSWORD、KAFKA_TOOLS_PASSWORD、
#          SILO_ROOT_USER、SILO_ROOT_PASSWORD、
#          NACOS_USERNAME、NACOS_PASSWORD、REG_CACERT。

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

CA="${REG_CACERT:-$ROOT/docker/certs/ca/rootCA.pem}"
MYSQL_APP_USER="${MYSQL_USERNAME:-iboot_app}"
MYSQL_APP_PASSWORD="${MYSQL_APP_PASSWORD:-${MYSQL_PASSWORD:-}}"
MYSQL_EXPORTER_USER="${MYSQL_EXPORTER_USER:-iboot_exporter}"
MYSQL_EXPORTER_PASSWORD="${MYSQL_EXPORTER_PASSWORD:-}"
REDIS_USERNAME="${REDIS_USERNAME:-iboot_app}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
REDIS_EXPORTER_PASSWORD="${REDIS_EXPORTER_PASSWORD:-}"
KAFKA_LOG_PASSWORD="${KAFKA_LOG_PASSWORD:-}"
KAFKA_LOGSTASH_PASSWORD="${KAFKA_LOGSTASH_PASSWORD:-}"
KAFKA_TOOLS_PASSWORD="${KAFKA_TOOLS_PASSWORD:-}"
SILO_ROOT_USER="${SILO_ROOT_USER:-}"
SILO_ROOT_PASSWORD="${SILO_ROOT_PASSWORD:-}"
NACOS_USERNAME="${NACOS_USERNAME:-}"
NACOS_PASSWORD="${NACOS_PASSWORD:-}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-}"
ES_AI_PASSWORD="${ES_AI_PASSWORD:-}"
ES_NOTIFY_PASSWORD="${ES_NOTIFY_PASSWORD:-}"
ES_MONITOR_PASSWORD="${ES_MONITOR_PASSWORD:-}"
ES_LOGSTASH_PASSWORD="${ES_LOGSTASH_PASSWORD:-}"
ES_GRAFANA_PASSWORD="${ES_GRAFANA_PASSWORD:-}"
ES_EXPORTER_PASSWORD="${ES_EXPORTER_PASSWORD:-}"

mysql_exec() {
    local user="$1" password="$2" sql="$3"
    docker exec -e MYSQL_PWD="$password" iboot-mysql mysql \
        --protocol=TCP --ssl-mode=VERIFY_IDENTITY --ssl-ca=/certs/rootCA.pem \
        -h127.0.0.1 -u"$user" -e "$sql" 2>/dev/null
}

ES_CA=/usr/share/elasticsearch/config/certs/rootCA.pem
es_status() {
    local user="$1" password="$2" method="$3" path="$4"
    docker exec iboot-elasticsearch \
        curl -sS --cacert "$ES_CA" -u "$user:$password" -X "$method" \
        -o /dev/null -w '%{http_code}' "https://localhost:9200$path" 2>/dev/null
}

reg_log "== 1. MySQL 最小权限 =="
if mysql_exec "$MYSQL_APP_USER" "$MYSQL_APP_PASSWORD" "SELECT 1" >/dev/null; then
    reg_pass "业务账号 $MYSQL_APP_USER 可以连接并查询"
else
    reg_fail "业务账号 $MYSQL_APP_USER 无法连接"
fi
if mysql_exec "$MYSQL_APP_USER" "$MYSQL_APP_PASSWORD" \
    "CREATE TABLE iboot.__auth_probe__ (id INT)" >/dev/null; then
    reg_fail "业务账号可以执行 DDL"
    mysql_exec "$MYSQL_APP_USER" "$MYSQL_APP_PASSWORD" "DROP TABLE IF EXISTS iboot.__auth_probe__" >/dev/null || true
else
    reg_pass "业务账号执行 DDL 被拒绝"
fi
if mysql_exec "$MYSQL_EXPORTER_USER" "$MYSQL_EXPORTER_PASSWORD" \
    "CREATE TABLE iboot.__exporter_probe__ (id INT)" >/dev/null; then
    reg_fail "exporter 账号可以写入业务库"
    mysql_exec "$MYSQL_EXPORTER_USER" "$MYSQL_EXPORTER_PASSWORD" "DROP TABLE IF EXISTS iboot.__exporter_probe__" >/dev/null || true
else
    reg_pass "exporter 账号不能写入业务库"
fi
if mysql_exec "$MYSQL_APP_USER" "wrong-password" "SELECT 1" >/dev/null; then
    reg_fail "MySQL 错误口令仍可连接"
else
    reg_pass "MySQL 错误口令被拒绝"
fi

redis_exec() {
    local user="$1" password="$2"
    shift 2
    docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem -p 6379 \
        --user "$user" --pass "$password" "$@" 2>/dev/null
}

reg_log "== 2. Redis ACL =="
if redis_exec "$REDIS_USERNAME" "$REDIS_PASSWORD" ping | grep -q PONG; then
    reg_pass "Redis 业务账号 $REDIS_USERNAME PING 成功"
else
    reg_fail "Redis 业务账号 $REDIS_USERNAME PING 失败"
fi
if docker exec iboot-redis redis-cli --tls --cacert /certs/rootCA.pem -p 6379 ping 2>/dev/null | grep -q PONG; then
    reg_fail "Redis 未认证仍可执行命令"
else
    reg_pass "Redis 未认证被拒绝"
fi
if redis_exec default "$REDIS_PASSWORD" ping | grep -q PONG; then
    reg_fail "Redis default 用户仍可登录"
else
    reg_pass "Redis default 用户被禁用"
fi
if redis_exec "$REDIS_USERNAME" "wrong-password" ping | grep -q PONG; then
    reg_fail "Redis 错误口令仍可执行命令"
else
    reg_pass "Redis 错误口令被拒绝"
fi
if redis_exec iboot_exporter "$REDIS_EXPORTER_PASSWORD" info server | grep -qi redis_version; then
    reg_pass "Redis exporter 账号可读取 INFO"
else
    reg_fail "Redis exporter 账号无法读取 INFO"
fi
if redis_exec iboot_exporter "$REDIS_EXPORTER_PASSWORD" set __exporter_probe__ 1 | grep -q OK; then
    redis_exec iboot_exporter "$REDIS_EXPORTER_PASSWORD" del __exporter_probe__ >/dev/null || true
    reg_fail "Redis exporter 账号可以写入"
else
    reg_pass "Redis exporter 账号不能写入"
fi

reg_log "== 3. Silo 凭据 =="
if docker exec -e SILO_USER="$SILO_ROOT_USER" -e SILO_PASS="$SILO_ROOT_PASSWORD" iboot-silo \
    sh -c 'mcli alias set good https://127.0.0.1:9000 "$SILO_USER" "$SILO_PASS" >/dev/null && mcli ls good >/dev/null' 2>/dev/null; then
    reg_pass "Silo 受管凭据可列举 bucket"
else
    reg_fail "Silo 受管凭据无法访问"
fi
if docker exec iboot-silo sh -c \
    'mcli alias set bad https://127.0.0.1:9000 minioadmin minioadmin >/dev/null 2>&1 && mcli ls bad >/dev/null 2>&1'; then
    reg_fail "Silo 默认凭据 minioadmin 仍可访问"
else
    reg_pass "Silo 默认凭据被拒绝"
fi

reg_log "== 4. Nacos 鉴权 =="
login_resp="$(curl -sS --cacert "$CA" -X POST \
    "https://localhost:8848/nacos/v3/auth/user/login" \
    --data-urlencode "username=$NACOS_USERNAME" \
    --data-urlencode "password=$NACOS_PASSWORD" 2>/dev/null || true)"
if printf '%s' "$login_resp" | grep -q accessToken; then
    reg_pass "Nacos 服务账号登录成功"
else
    reg_fail "Nacos 服务账号登录失败"
fi
anon_status="$(curl -sS --cacert "$CA" -o /dev/null -w '%{http_code}' -X POST \
    "https://localhost:8848/nacos/v3/admin/cs/config" \
    --data-urlencode "dataId=__auth_probe__" \
    --data-urlencode "groupName=DEFAULT_GROUP" \
    --data-urlencode "namespaceId=public" \
    --data-urlencode "content=x" --data-urlencode "type=json" 2>/dev/null || true)"
if [ "$anon_status" = "401" ] || [ "$anon_status" = "403" ]; then
    reg_pass "Nacos 匿名管理请求被拒绝（HTTP $anon_status）"
else
    reg_fail "Nacos 匿名管理请求未被拒绝（HTTP ${anon_status:-无响应}）"
fi
if env -u NACOS_USERNAME -u NACOS_PASSWORD "$ROOT/scripts/sentinel-init.sh" >/dev/null 2>&1; then
    reg_fail "sentinel-init 缺少凭据仍成功"
else
    reg_pass "sentinel-init 缺少凭据时失败"
fi

reg_log "== 5. Elasticsearch 最小权限 =="
if [ "$(es_status iboot_ai "$ES_AI_PASSWORD" GET '/iboot-kb-chunk-*/_search')" = "200" ]; then
    reg_pass "AI 账号可读取知识库索引"
else
    reg_fail "AI 账号无法读取知识库索引"
fi
if [ "$(es_status iboot_ai "$ES_AI_PASSWORD" GET '/_security/user')" = "403" ]; then
    reg_pass "AI 账号不能访问安全接口"
else
    reg_fail "AI 账号越权访问安全接口"
fi
if [ "$(es_status iboot_ai "$ES_AI_PASSWORD" PUT '/iboot-kb-chunk-auth-probe')" = "200" ]; then
    reg_pass "AI 账号可创建知识库索引"
    es_status elastic "$ELASTIC_PASSWORD" DELETE '/iboot-kb-chunk-auth-probe' >/dev/null
else
    reg_fail "AI 账号无法创建知识库索引"
fi
if [ "$(es_status iboot_notify "$ES_NOTIFY_PASSWORD" PUT '/iboot-content-auth-probe')" = "200" ]; then
    reg_pass "通知账号可创建内容索引"
    es_status elastic "$ELASTIC_PASSWORD" DELETE '/iboot-content-auth-probe' >/dev/null
else
    reg_fail "通知账号无法创建内容索引"
fi
if [ "$(es_status iboot_monitor "$ES_MONITOR_PASSWORD" GET '/iboot-logs-*/_search')" = "200" ]; then
    reg_pass "监控账号可读取日志索引"
else
    reg_fail "监控账号无法读取日志索引"
fi
if [ "$(es_status iboot_monitor "$ES_MONITOR_PASSWORD" PUT '/iboot-logs-auth-probe')" = "403" ]; then
    reg_pass "监控账号不能写入日志索引"
else
    reg_fail "监控账号越权写入日志索引"
fi
if [ "$(es_status iboot_grafana "$ES_GRAFANA_PASSWORD" GET '/iboot-logs-*/_search')" = "200" ]; then
    reg_pass "Grafana 账号可读取日志索引"
else
    reg_fail "Grafana 账号无法读取日志索引"
fi
if [ "$(es_status iboot_logstash "$ES_LOGSTASH_PASSWORD" PUT '/iboot-logs-auth-probe')" = "200" ]; then
    reg_pass "Logstash 账号可写入日志索引"
    es_status elastic "$ELASTIC_PASSWORD" DELETE '/iboot-logs-auth-probe' >/dev/null
else
    reg_fail "Logstash 账号无法写入日志索引"
fi
if [ "$(es_status iboot_exporter "$ES_EXPORTER_PASSWORD" GET '/_cluster/health')" = "200" ]; then
    reg_pass "ES exporter 账号可读取集群健康"
else
    reg_fail "ES exporter 账号无法读取集群健康"
fi
if [ "$(es_status iboot_exporter "$ES_EXPORTER_PASSWORD" PUT '/iboot-exporter-auth-probe')" = "403" ]; then
    reg_pass "ES exporter 账号不能创建索引"
else
    reg_fail "ES exporter 账号越权创建索引"
fi
if [ "$(es_status iboot_monitor wrong-password GET '/_cluster/health')" = "401" ]; then
    reg_pass "ES 错误口令被拒绝"
else
    reg_fail "ES 错误口令未被拒绝"
fi
ai_env=""
if docker inspect iboot-ai >/dev/null 2>&1; then
    ai_env="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' iboot-ai 2>/dev/null || true)"
else
    ai_pid="$(pgrep -f '^java .*iboot-ai-1\.0\.0\.jar' | head -1 || true)"
    [ -n "$ai_pid" ] && ai_env="$(tr '\0' '\n' < "/proc/$ai_pid/environ" 2>/dev/null || true)"
fi
if printf '%s' "$ai_env" | grep -q '^ELASTIC_PASSWORD=..'; then
    reg_fail "业务服务仍注入了 elastic 超级用户口令"
else
    reg_pass "业务服务没有 elastic 超级用户口令"
fi

reg_log "== 6. Neo4j 凭据 =="
neo4j_running="$(docker inspect -f '{{.State.Running}}' iboot-neo4j 2>/dev/null || true)"
if [ "$neo4j_running" = "true" ]; then
    # cypher-shell 没有 CA 参数；+ssc 表示加密并接受自签证书，CA 校验由内部 TLS 回归覆盖。
    if docker exec iboot-neo4j /var/lib/neo4j/bin/cypher-shell \
        -a bolt+ssc://localhost:7687 -u neo4j -p "$NEO4J_PASSWORD" "RETURN 1" >/dev/null 2>&1; then
        reg_pass "Neo4j 受管口令可连接"
    else
        reg_fail "Neo4j 受管口令无法连接"
    fi
    if docker exec iboot-neo4j /var/lib/neo4j/bin/cypher-shell \
        -a bolt+ssc://localhost:7687 -u neo4j -p iboot123456 "RETURN 1" >/dev/null 2>&1; then
        reg_fail "Neo4j 旧默认口令仍可连接"
    else
        reg_pass "Neo4j 旧默认口令被拒绝"
    fi
else
    reg_log "  [SKIP] Neo4j 未运行（graph profile 未启用）"
fi

reg_log "== 7. Kafka SASL 与授权 =="
kafka_client_config() {
    local user="$1" password="$2"
    {
        printf 'security.protocol=SASL_SSL\n'
        printf 'sasl.mechanism=PLAIN\n'
        printf 'sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="%s" password="%s";\n' "$user" "$password"
        printf 'ssl.truststore.location=/etc/kafka/secrets/truststore.p12\n'
        printf 'ssl.truststore.password=changeit\n'
        printf 'ssl.truststore.type=PKCS12\n'
        printf 'ssl.endpoint.identification.algorithm=HTTPS\n'
    } | docker exec -i iboot-kafka sh -c 'cat > /tmp/iboot-regress-client.properties'
}

kafka_topics() {
    timeout -k 3 40 docker exec iboot-kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --command-config /tmp/iboot-regress-client.properties "$@" 2>/dev/null
}

kafka_broker_versions() {
    timeout -k 3 15 docker exec iboot-kafka /opt/kafka/bin/kafka-broker-api-versions.sh \
        --bootstrap-server localhost:9092 \
        --command-config /tmp/iboot-regress-client.properties 2>/dev/null
}

kafka_client_config iboot_tools "$KAFKA_TOOLS_PASSWORD"
if kafka_topics --list | grep -qx iboot-logs; then
    reg_pass "Kafka 运维账号可列出日志 topic"
else
    reg_fail "Kafka 运维账号无法列出日志 topic"
fi

kafka_client_config iboot_log "$KAFKA_LOG_PASSWORD"
if kafka_topics --create --if-not-exists --topic __auth_probe__ --partitions 1 --replication-factor 1 >/dev/null 2>&1; then
    reg_fail "Kafka 业务日志账号可以创建 topic"
    kafka_client_config iboot_tools "$KAFKA_TOOLS_PASSWORD"
    kafka_topics --delete --topic __auth_probe__ >/dev/null 2>&1 || true
else
    reg_pass "Kafka 业务日志账号不能创建 topic"
fi

kafka_client_config iboot_logstash "$KAFKA_LOGSTASH_PASSWORD"
group_out="$(docker exec iboot-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --command-config /tmp/iboot-regress-client.properties \
    --describe --group iboot-logstash 2>&1 || true)"
if printf '%s' "$group_out" | grep -q "iboot-logs"; then
    reg_pass "Kafka 消费账号可查看日志消费组"
else
    reg_fail "Kafka 消费账号无法查看日志消费组：$(printf '%s' "$group_out" | head -1)"
fi

kafka_client_config iboot_log "$KAFKA_LOG_PASSWORD"
consumer_out="$(docker exec iboot-kafka timeout 15 /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --command-config /tmp/iboot-regress-client.properties \
    --topic iboot-logs --from-beginning --max-messages 1 2>&1 || true)"
if printf '%s' "$consumer_out" | grep -qE "TopicAuthorizationException|GroupAuthorizationException"; then
    reg_pass "Kafka 业务日志账号不能消费日志 topic"
else
    reg_fail "Kafka 业务日志账号可以消费日志 topic"
fi

{
    printf 'security.protocol=SSL\n'
    printf 'ssl.truststore.location=/etc/kafka/secrets/truststore.p12\n'
    printf 'ssl.truststore.password=changeit\n'
    printf 'ssl.truststore.type=PKCS12\n'
    printf 'ssl.endpoint.identification.algorithm=HTTPS\n'
} | docker exec -i iboot-kafka sh -c 'cat > /tmp/iboot-regress-client.properties'
if kafka_broker_versions | grep -q .; then
    reg_fail "Kafka 无 SASL 凭据仍可访问"
else
    reg_pass "Kafka 无 SASL 凭据被拒绝"
fi

kafka_client_config iboot_log "wrong-password"
if kafka_broker_versions | grep -q .; then
    reg_fail "Kafka 错误 SASL 口令仍可访问"
else
    reg_pass "Kafka 错误 SASL 口令被拒绝"
fi

reg_summary
