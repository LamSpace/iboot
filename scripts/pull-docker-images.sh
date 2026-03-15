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
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/elasticsearch/elasticsearch:8.19.7
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/elasticsearch/elasticsearch:8.19.7  docker.elastic.co/elasticsearch/elasticsearch:8.19.7
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/elasticsearch/elasticsearch:8.19.7
echo "Elasticsearch 镜像拉取完成！"
echo ""

# 拉取 Logstash 镜像
echo "======================================"
echo "正在拉取 Logstash 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/logstash/logstash:8.17.4
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/logstash/logstash:8.17.4  docker.elastic.co/logstash/logstash:8.17.4
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/logstash/logstash:8.17.4
echo "Logstash 镜像拉取完成！"
echo ""

# 拉取 Kibana 镜像
echo "======================================"
echo "正在拉取 Kibana 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/kibana/kibana:8.18.8
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/kibana/kibana:8.18.8  docker.elastic.co/kibana/kibana:8.18.8
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.elastic.co/kibana/kibana:8.18.8
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

# 统计拉取的镜像数量
IMAGE_COUNT=$(docker images | wc -l)
echo "共拉取 $((IMAGE_COUNT - 1)) 个镜像"
echo "======================================"
