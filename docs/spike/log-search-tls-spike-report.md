# B3 日志与检索面 TLS Spike 报告

日期：2026-11-21

范围：Elasticsearch 9.5.5、Kafka 4.4.0、Logstash 9.5.5、Kibana 9.5.5、Grafana 13.2.6、elasticsearch-exporter v1.11.0、RocketMQ 5.5.0 Proxy。

## 1. Elasticsearch 9.5.5

### 配置

```yaml
xpack.security.enabled: true
xpack.security.http.ssl.enabled: true
xpack.security.http.ssl.certificate: /usr/share/elasticsearch/config/certs/portal.pem
xpack.security.http.ssl.key: /usr/share/elasticsearch/config/certs/portal-key.pem
xpack.security.transport.ssl.enabled: true
xpack.security.transport.ssl.verification_mode: certificate
xpack.security.transport.ssl.certificate: /usr/share/elasticsearch/config/certs/portal.pem
xpack.security.transport.ssl.key: /usr/share/elasticsearch/config/certs/portal-key.pem
xpack.security.transport.ssl.certificate_authorities: /usr/share/elasticsearch/config/certs/rootCA.pem
```

### 关键发现

1. **证书必须放在 ES 配置目录内**。ES 9.5 的 entitlement 策略拒绝读取 `/certs/portal.pem`：

   ```text
   cannot read configured PEM certificate_authorities [/certs/portal.pem]
   because access to read the file is blocked; SSL resources should be placed in
   the [/usr/share/elasticsearch/config] directory
   ```

   因此 compose 必须把证书挂到 `/usr/share/elasticsearch/config/certs`，不能沿用其他组件的 `/certs` 路径。
2. **已有无 security 的卷可以直接启用 security**。先用 `xpack.security.enabled: false` 启动并写入数据，停止后用 security 配置 + `ELASTIC_PASSWORD` 重启，ES 自动创建 `.security-7`，HTTPS + 凭据返回 200；不需要手工 `elasticsearch-reset-password`。若旧卷已有异常状态，仍保留重置密码或重建本地卷的兜底。
3. 验证矩阵：
   - `https://localhost:19200` + CA + 正确凭据：200
   - `http://localhost:19200`：空响应，未返回数据
   - HTTPS 不带 CA：证书校验失败
   - HTTPS + 错误密码：401

## 2. Kafka 4.4.0

### 配置

三个监听全部 SSL：

```text
SSL://:9092, SSL_HOST://:19092, CONTROLLER://:9093
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=SSL:SSL,SSL_HOST:SSL,CONTROLLER:SSL
KAFKA_INTER_BROKER_LISTENER_NAME=SSL
KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093
```

### 关键发现

1. **镜像的 SSL 配置入口不是 `KAFKA_SSL_KEYSTORE_LOCATION`**。`/etc/kafka/docker/configure` 要求：
   - `KAFKA_SSL_KEYSTORE_FILENAME`
   - `KAFKA_SSL_KEY_CREDENTIALS`
   - `KAFKA_SSL_KEYSTORE_CREDENTIALS`
   - 文件位于 `/etc/kafka/secrets/`，凭据文件内容为密码明文。
2. **client auth 为 none 时，镜像不会自动设置 truststore**。controller/broker 的出站 SSL 仍需要 truststore，否则启动时报：

   ```text
   PKIX path building failed: unable to find valid certification path
   ```

   必须额外直接传 `KAFKA_SSL_TRUSTSTORE_LOCATION`、`KAFKA_SSL_TRUSTSTORE_PASSWORD`、`KAFKA_SSL_TRUSTSTORE_TYPE`。
3. Kafka 4.4 的 console producer/consumer 使用 `--command-config`；`--producer-config` / `--consumer-config` 不被识别。
4. 验证结果：
   - SSL 创建 topic、生产、消费成功
   - 宿主机 19092 端口 TLS 1.3 握手成功，`Verify return code: 0`
   - PLAINTEXT producer 无法建立元数据，超时失败
   - SSL 但无 truststore 的 producer 因 PKIX 失败

## 3. Logstash 9.5.5

可用配置键：

```ruby
input {
  kafka {
    bootstrap_servers => "localhost:19092"
    security_protocol => "SSL"
    ssl_truststore_location => "/certs/truststore.p12"
    ssl_truststore_password => "changeit"
    ssl_truststore_type => "PKCS12"
    ssl_endpoint_identification_algorithm => "https"
  }
}
output {
  elasticsearch {
    hosts => ["https://localhost:19200"]
    ssl_enabled => true
    ssl_certificate_authorities => "/certs/rootCA.pem"
    user => "elastic"
    password => "<env>"
  }
}
```

结果：探针从 Kafka 写入，经 Logstash 进入 ES，按 traceId 查询命中 1 条。Logstash 挂载 pipeline 时应覆盖默认 `logstash.conf`，否则默认 beats 输入会与宿主机 5044 冲突。

## 4. Kibana 9.5.5

1. **不能用 `elastic` 超级用户连接**。Kibana 启动直接失败：

   ```text
   value of "elastic" is forbidden. This is a superuser account that cannot
   write to system indices that Kibana needs to function.
   ```

2. 可用 `kibana_system` 用户：

   ```bash
   curl --cacert rootCA.pem -u elastic:$ELASTIC_PASSWORD -X POST \
     -H 'Content-Type: application/json' \
     -d '{"password":"..."}' \
     https://localhost:9200/_security/user/kibana_system/_password
   ```

   Kibana 环境变量：

   ```text
   ELASTICSEARCH_HOSTS=https://elasticsearch:9200
   ELASTICSEARCH_USERNAME=kibana_system
   ELASTICSEARCH_PASSWORD=<kibana_system password>
   ELASTICSEARCH_SSL_CERTIFICATEAUTHORITIES=/certs/rootCA.pem
   ```

3. 启动后 `/api/status` 返回 200，ES 连接无证书错误。

## 5. Grafana 13.2.6

数据源 provisioning 可用：

```yaml
url: https://elasticsearch:9200
basicAuth: true
basicAuthUser: elastic
secureJsonData:
  basicAuthPassword: <env>
  tlsCACert: $__file{/certs/rootCA.pem}
jsonData:
  tlsAuthWithCACert: true
  index: iboot-logs-*
```

`$__file{}` 在当前版本可用，数据源健康检查返回：

```json
{"message":"Elasticsearch data source is healthy.","status":"OK"}
```

## 6. elasticsearch-exporter v1.11.0

可用参数：

```text
--es.uri=https://elastic:<password>@elasticsearch:9200
--es.ca=/certs/rootCA.pem
```

`/metrics` 返回 200，包含 `elasticsearch_cluster_health_status` 与 `elasticsearch_indices_docs`。凭据会出现在进程参数中，仅限本地开发；生产应使用专用 secret 注入方式。

## 7. RocketMQ 5.5.0 Proxy

配置：

```json
{
  "rocketMQClusterName": "DefaultCluster",
  "useEndpointPortFromRequest": true,
  "tlsTestModeEnable": false,
  "tlsKeyPath": "/certs/portal-key.pem",
  "tlsCertPath": "/certs/portal.pem"
}
```

结果：proxy 启动成功；`openssl s_client` 对 8081 返回 `TLSv1.3`，`Verify return code: 0`。

边界（沿用 B0 并在本轮复核）：

- Proxy 使用协议探测，同一端口仍接受明文；TLS 不强制。
- RocketMQ client 5.2.2 的 SSL 分支使用 `InsecureTrustManagerFactory`，不校验 CA 与主机名。
- 因此只能登记为“加密通道”，不能作为零信任身份或 mTLS 能力。

## 8. 对实现的直接影响

- ES compose 的证书挂载点改为 `/usr/share/elasticsearch/config/certs`。
- Kafka compose 使用 `/etc/kafka/secrets` + filename/credentials 变量，并额外传 truststore。
- Kibana 使用 `kibana_system`，需要幂等设置其密码；不能复用 `elastic`。
- `tls-init.sh` 需生成 `kafka-keystore.p12`、key/keystore 密码文件。
- Logstash 的 pipeline 覆盖默认配置，避免默认 5044 输入。
- Grafana 使用 `$__file{}`；exporter 使用 URI 内嵌凭据。

## 9. 复现命令

以下命令在 `docker/certs` 已生成、`docker/certs/runtime` 存在时执行：

```bash
# ES
docker run -d --name b3-es -p 19200:9200 \
  -e ELASTIC_PASSWORD=iboot-dev-elastic \
  -v $PWD/.cache/iboot-b3-spike/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml:ro \
  -v $PWD/docker/certs/runtime:/usr/share/elasticsearch/config/certs:ro \
  -v b3_es_data:/usr/share/elasticsearch/data elasticsearch:9.5.5

curl --cacert docker/certs/ca/rootCA.pem -u elastic:iboot-dev-elastic \
  https://localhost:19200/_cluster/health

# Kafka 客户端配置
security.protocol=SSL
ssl.truststore.location=/etc/kafka/secrets/truststore.p12
ssl.truststore.password=changeit
ssl.truststore.type=PKCS12
ssl.endpoint.identification.algorithm=HTTPS

# Logstash
docker run --rm --network host \
  -v $PWD/.cache/iboot-b3-spike/logstash/pipeline.conf:/usr/share/logstash/pipeline/logstash.conf:ro \
  -v $PWD/docker/certs/runtime:/certs:ro logstash:9.5.5

# Grafana 数据源健康检查
docker exec b3-grafana curl -u admin:admin \
  http://localhost:13000/api/datasources/uid/b3-es-tls/health
```

Spike 容器（`b3-es`、`b3-kafka`、`b3-logstash`、`b3-kibana`、`b3-grafana`、`b3-es-exporter`、`b3-rmq-proxy`）在报告完成后删除，不进入版本管理。
