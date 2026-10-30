#!/bin/bash

# 拉取 Docker 镜像脚本
# 用于拉取 docker-compose.yml 中定义的所有镜像到本地

set -e

echo "======================================"
echo "开始拉取 Docker 镜像..."
echo "======================================"
echo ""


# 拉取 MySQL 镜像
echo "======================================"
echo "正在拉取 MySQL 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/mysql:8.0.45
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/mysql:8.0.45  docker.io/mysql:8.0.45
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/mysql:8.0.45
echo "MySQL 镜像拉取完成！"
echo ""

# 拉取 Redis 镜像
echo "======================================"
echo "正在拉取 Redis 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/redis:7.0.14
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/redis:7.0.14  docker.io/library/redis:7.0.14
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/redis:7.0.14
echo "Redis 镜像拉取完成！"
echo ""

# 拉取 MinIO 镜像
echo "======================================"
echo "正在拉取 MinIO 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z  docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z
echo "MinIO 镜像拉取完成！"
echo ""

# 拉取 Neo4j 镜像
echo "======================================"
echo "正在拉取 Neo4j 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/neo4j:5.26.29-community
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/neo4j:5.26.29-community  docker.io/library/neo4j:5.26.29-community
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/neo4j:5.26.29-community
echo "Neo4j 镜像拉取完成！"
echo ""


# 拉取 Prometheus 镜像
echo "======================================"
echo "正在拉取 Prometheus 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/prometheus:v2.53.4
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/prometheus:v2.53.4  docker.io/prom/prometheus:v2.53.4
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/prometheus:v2.53.4
echo "Prometheus 镜像拉取完成！"
echo ""

# 拉取 Grafana 镜像
echo "======================================"
echo "正在拉取 Grafana 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/grafana/grafana:12.2.0
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/grafana/grafana:12.2.0  docker.io/grafana/grafana:12.2.0
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/grafana/grafana:12.2.0
echo "Grafana 镜像拉取完成！"
echo ""

# 拉取 Alertmanager 镜像
echo "======================================"
echo "正在拉取 Alertmanager 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/alertmanager:v0.29.0
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/alertmanager:v0.29.0  docker.io/prom/alertmanager:v0.29.0
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/alertmanager:v0.29.0
echo "Alertmanager 镜像拉取完成！"
echo ""

# 拉取 MySQL Exporter 镜像
echo "======================================"
echo "正在拉取 MySQL Exporter 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/mysqld-exporter:v0.15.0
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/mysqld-exporter:v0.15.0  docker.io/prom/mysqld-exporter:v0.15.0
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/mysqld-exporter:v0.15.0
echo "MySQL Exporter 镜像拉取完成！"
echo ""

# 拉取 Redis Exporter 镜像
echo "======================================"
echo "正在拉取 Redis Exporter 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/oliver006/redis_exporter:v1.69.0
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/oliver006/redis_exporter:v1.69.0  docker.io/oliver006/redis_exporter:v1.69.0
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/oliver006/redis_exporter:v1.69.0
echo "Redis Exporter 镜像拉取完成！"
echo ""

# 拉取 Node Exporter 镜像
echo "======================================"
echo "正在拉取 Node Exporter 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/node-exporter:v1.10.2
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/node-exporter:v1.10.2  docker.io/prom/node-exporter:v1.10.2
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prom/node-exporter:v1.10.2
echo "Node Exporter 镜像拉取完成！"
echo ""

# 拉取 Elasticsearch 镜像
echo "======================================"
echo "正在拉取 Elasticsearch 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/elasticsearch/elasticsearch:9.2.3
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/elasticsearch/elasticsearch:9.2.3  docker.elastic.co/elasticsearch/elasticsearch:9.2.3
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/elasticsearch/elasticsearch:9.2.3
echo "Elasticsearch 镜像拉取完成！"
echo ""

# 拉取 Logstash 镜像
echo "======================================"
echo "正在拉取 Logstash 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/logstash/logstash:9.2.3
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/logstash/logstash:9.2.3  docker.elastic.co/logstash/logstash:9.2.3
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/logstash/logstash:9.2.3
echo "Logstash 镜像拉取完成！"
echo ""

# 拉取 Kibana 镜像
echo "======================================"
echo "正在拉取 Kibana 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/kibana/kibana:9.2.3
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/kibana/kibana:9.2.3  docker.elastic.co/kibana/kibana:9.2.3
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/kibana/kibana:9.2.3
echo "Kibana 镜像拉取完成！"
echo ""

# 拉取 Elasticsearch Exporter 镜像
echo "======================================"
echo "正在拉取 Elasticsearch Exporter 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prometheuscommunity/elasticsearch-exporter:v1.5.0
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prometheuscommunity/elasticsearch-exporter:v1.5.0  docker.io/prometheuscommunity/elasticsearch-exporter:v1.5.0
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/prometheuscommunity/elasticsearch-exporter:v1.5.0
echo "Elasticsearch Exporter 镜像拉取完成！"
echo ""

# 拉取 Thanos 镜像
echo "======================================"
echo "正在拉取 Thanos 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/thanosio/thanos:v0.36.1
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/thanosio/thanos:v0.36.1  docker.io/thanosio/thanos:v0.36.1
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/thanosio/thanos:v0.36.1
echo "Thanos 镜像拉取完成！"
echo ""

# ======================================
# 阶段 2 微服务组件（Nacos / Sentinel / Seata / RocketMQ / 链路追踪）
# 版本与 Spring Cloud Alibaba 2025.1.0.0 BOM 保持一致：
# nacos-client 3.1.1、sentinel 1.8.9、seata 2.5.0、rocketmq 5.3.1
# ======================================

# 拉取 Nacos 镜像
echo "======================================"
echo "正在拉取 Nacos 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/nacos/nacos-server:v3.1.1
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/nacos/nacos-server:v3.1.1  docker.io/nacos/nacos-server:v3.1.1
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/nacos/nacos-server:v3.1.1
echo "Nacos 镜像拉取完成！"
echo ""

# 拉取 Sentinel Dashboard 镜像
# 注意：Sentinel 官方不提供 Docker 镜像，此处使用第三方打包的 1.8.9 控制台镜像
echo "======================================"
echo "正在拉取 Sentinel Dashboard 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/bladex/sentinel-dashboard:1.8.9
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/bladex/sentinel-dashboard:1.8.9  docker.io/bladex/sentinel-dashboard:1.8.9
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/bladex/sentinel-dashboard:1.8.9
echo "Sentinel Dashboard 镜像拉取完成！"
echo ""

# 拉取 Seata Server 镜像
echo "======================================"
echo "正在拉取 Seata Server 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apache/seata-server:2.5.0
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apache/seata-server:2.5.0  docker.io/apache/seata-server:2.5.0
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apache/seata-server:2.5.0
echo "Seata Server 镜像拉取完成！"
echo ""

# 拉取 RocketMQ 镜像（NameServer 与 Broker 共用同一镜像，启动命令不同）
echo "======================================"
echo "正在拉取 RocketMQ 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apache/rocketmq:5.3.1
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apache/rocketmq:5.3.1  docker.io/apache/rocketmq:5.3.1
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apache/rocketmq:5.3.1
echo "RocketMQ 镜像拉取完成！"
echo ""

# 拉取 RocketMQ Dashboard 镜像
echo "======================================"
echo "正在拉取 RocketMQ Dashboard 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apacherocketmq/rocketmq-dashboard:2.1.0
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apacherocketmq/rocketmq-dashboard:2.1.0  docker.io/apacherocketmq/rocketmq-dashboard:2.1.0
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/apacherocketmq/rocketmq-dashboard:2.1.0
echo "RocketMQ Dashboard 镜像拉取完成！"
echo ""

# 拉取 Tempo 镜像（链路追踪后端，Grafana 可直接作为查询界面）
echo "======================================"
echo "正在拉取 Tempo 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/grafana/tempo:2.10.8
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/grafana/tempo:2.10.8  docker.io/grafana/tempo:2.10.8
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/grafana/tempo:2.10.8
echo "Tempo 镜像拉取完成！"
echo ""

# 拉取 OpenTelemetry Collector 镜像（可选，应用可直连 Tempo 的 OTLP 端口）
echo "======================================"
echo "正在拉取 OpenTelemetry Collector 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/otel/opentelemetry-collector-contrib:0.140.0
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/otel/opentelemetry-collector-contrib:0.140.0  docker.io/otel/opentelemetry-collector-contrib:0.140.0
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/otel/opentelemetry-collector-contrib:0.140.0
echo "OpenTelemetry Collector 镜像拉取完成！"
echo ""

# 统计拉取的镜像数量
IMAGE_COUNT=$(docker images | wc -l)
echo "共拉取 $((IMAGE_COUNT - 1)) 个镜像"
echo "======================================"
