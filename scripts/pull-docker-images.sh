#!/bin/bash

# 拉取 Docker 镜像脚本
# 用于拉取 docker-compose.yml 与 Dockerfile 中定义的镜像到本地。
#
# 来源约定：
# - 除 MinIO 外，全部镜像直接来自 Docker Hub 官方仓库；
# - MinIO 上游已停止发布官方容器镜像，继续使用 docker.aityp.com 展示的
#   SWR 镜像路径，拉取后重打为 docker.io/minio/minio:<tag>。

set -e

echo "======================================"
echo "开始拉取 Docker 镜像..."
echo "======================================"
echo ""


# 拉取 MySQL 镜像
echo "======================================"
echo "正在拉取 MySQL 镜像..."
docker pull docker.io/library/mysql:8.4.13
echo "MySQL 镜像拉取完成！"
echo ""

# 拉取 Redis 镜像
echo "======================================"
echo "正在拉取 Redis 镜像..."
docker pull docker.io/library/redis:8.8.4
echo "Redis 镜像拉取完成！"
echo ""

# 拉取 MinIO 镜像（Docker Hub 官方仓库已不可用，走 aityp/SWR）
echo "======================================"
echo "正在拉取 MinIO 镜像..."
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z  docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z
docker rmi swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z || true
echo "MinIO 镜像拉取完成！"
echo ""

# 拉取 Neo4j 镜像
echo "======================================"
echo "正在拉取 Neo4j 镜像..."
docker pull docker.io/library/neo4j:5.26.33-community
echo "Neo4j 镜像拉取完成！"
echo ""


# 拉取 Prometheus 镜像
echo "======================================"
echo "正在拉取 Prometheus 镜像..."
docker pull docker.io/prom/prometheus:v3.16.0
echo "Prometheus 镜像拉取完成！"
echo ""

# 拉取 Grafana 镜像
echo "======================================"
echo "正在拉取 Grafana 镜像..."
docker pull docker.io/grafana/grafana:13.2.6
echo "Grafana 镜像拉取完成！"
echo ""

# 拉取 Alertmanager 镜像
echo "======================================"
echo "正在拉取 Alertmanager 镜像..."
docker pull docker.io/prom/alertmanager:v0.35.0
echo "Alertmanager 镜像拉取完成！"
echo ""

# 拉取 MySQL Exporter 镜像
echo "======================================"
echo "正在拉取 MySQL Exporter 镜像..."
docker pull docker.io/prom/mysqld-exporter:v0.21.0
echo "MySQL Exporter 镜像拉取完成！"
echo ""

# 拉取 Redis Exporter 镜像
echo "======================================"
echo "正在拉取 Redis Exporter 镜像..."
docker pull docker.io/oliver006/redis_exporter:v1.97.0
echo "Redis Exporter 镜像拉取完成！"
echo ""

# 拉取 Node Exporter 镜像
echo "======================================"
echo "正在拉取 Node Exporter 镜像..."
docker pull docker.io/prom/node-exporter:v1.12.1
echo "Node Exporter 镜像拉取完成！"
echo ""

# 拉取 Elasticsearch 镜像（Docker Hub 官方镜像）
echo "======================================"
echo "正在拉取 Elasticsearch 镜像..."
docker pull docker.io/library/elasticsearch:9.5.5
echo "Elasticsearch 镜像拉取完成！"
echo ""

# 拉取 Logstash 镜像（Docker Hub 官方镜像）
echo "======================================"
echo "正在拉取 Logstash 镜像..."
docker pull docker.io/library/logstash:9.5.5
echo "Logstash 镜像拉取完成！"
echo ""

# 拉取 Kibana 镜像（Docker Hub 官方镜像）
echo "======================================"
echo "正在拉取 Kibana 镜像..."
docker pull docker.io/library/kibana:9.5.5
echo "Kibana 镜像拉取完成！"
echo ""

# 拉取 Elasticsearch Exporter 镜像
echo "======================================"
echo "正在拉取 Elasticsearch Exporter 镜像..."
docker pull docker.io/prometheuscommunity/elasticsearch-exporter:v1.11.0
echo "Elasticsearch Exporter 镜像拉取完成！"
echo ""

# 拉取 Thanos 镜像
echo "======================================"
echo "正在拉取 Thanos 镜像..."
docker pull docker.io/thanosio/thanos:v0.42.5
echo "Thanos 镜像拉取完成！"
echo ""

# ======================================
# 阶段 2 微服务组件（Nacos / Sentinel / Seata / RocketMQ / 链路追踪）
# 服务端选取各组件最新稳定版；客户端仍由 Spring Cloud Alibaba 2025.1.0.0 BOM 管理：
# nacos-client 3.1.1、sentinel 1.8.9、seata 2.5.0、rocketmq 5.2.2。
# 升级服务端前需确认对应客户端协议的向后兼容性。
# ======================================

# 拉取 Nacos 镜像
echo "======================================"
echo "正在拉取 Nacos 镜像..."
docker pull docker.io/nacos/nacos-server:v3.3.0
echo "Nacos 镜像拉取完成！"
echo ""

# 拉取 Sentinel Dashboard 镜像
# 注意：Sentinel 官方不提供 Docker 镜像，此处使用第三方打包的 1.8.10 控制台镜像
echo "======================================"
echo "正在拉取 Sentinel Dashboard 镜像..."
docker pull docker.io/bladex/sentinel-dashboard:1.8.10
echo "Sentinel Dashboard 镜像拉取完成！"
echo ""

# 拉取 Seata Server 镜像
echo "======================================"
echo "正在拉取 Seata Server 镜像..."
docker pull docker.io/apache/seata-server:2.6.0
echo "Seata Server 镜像拉取完成！"
echo ""

# 拉取 RocketMQ 镜像（NameServer 与 Broker 共用同一镜像，启动命令不同）
echo "======================================"
echo "正在拉取 RocketMQ 镜像..."
docker pull docker.io/apache/rocketmq:5.5.0
echo "RocketMQ 镜像拉取完成！"
echo ""

# 拉取 RocketMQ Dashboard 镜像
echo "======================================"
echo "正在拉取 RocketMQ Dashboard 镜像..."
docker pull docker.io/apacherocketmq/rocketmq-dashboard:2.1.0
echo "RocketMQ Dashboard 镜像拉取完成！"
echo ""

# 拉取 Kafka 镜像（日志缓冲层，KRaft 单进程；服务端 4.x 与 Boot 4.0.3 托管的 kafka-clients 4.1.1 保持同主版本）
echo "======================================"
echo "正在拉取 Kafka 镜像..."
docker pull docker.io/apache/kafka:4.4.0
echo "Kafka 镜像拉取完成！"
echo ""

# 拉取 Tempo 镜像（链路追踪后端，Grafana 可直接作为查询界面）
echo "======================================"
echo "正在拉取 Tempo 镜像..."
docker pull docker.io/grafana/tempo:2.10.9
echo "Tempo 镜像拉取完成！"
echo ""

# ======================================
# 阶段 2 收尾：SkyWalking APM 对比实验
# OAP 11.1.0 + Horizon UI 1.1.0 + BanyanDB 0.11.2 是官方配套组合；
# OAP 11 只接受 BanyanDB server API 0.11，版本不可自行混搭。
# ======================================

# 拉取 SkyWalking BanyanDB 镜像
echo "======================================"
echo "正在拉取 SkyWalking BanyanDB 镜像..."
docker pull docker.io/apache/skywalking-banyandb:0.11.2
echo "SkyWalking BanyanDB 镜像拉取完成！"
echo ""

# 拉取 SkyWalking OAP 镜像
echo "======================================"
echo "正在拉取 SkyWalking OAP 镜像..."
docker pull docker.io/apache/skywalking-oap-server:11.1.0
echo "SkyWalking OAP 镜像拉取完成！"
echo ""

# 拉取 SkyWalking Horizon UI 镜像（11.x 起 UI 使用独立的 horizon-* 标签）
echo "======================================"
echo "正在拉取 SkyWalking Horizon UI 镜像..."
docker pull docker.io/apache/skywalking-ui:horizon-1.1.0
echo "SkyWalking Horizon UI 镜像拉取完成！"
echo ""

# 拉取 OpenTelemetry Collector 镜像（可选，应用可直连 Tempo 的 OTLP 端口）
echo "======================================"
echo "正在拉取 OpenTelemetry Collector 镜像..."
docker pull docker.io/otel/opentelemetry-collector-contrib:0.165.0
echo "OpenTelemetry Collector 镜像拉取完成！"
echo ""

# ======================================
# 构建与运行期基础镜像（多阶段构建）
# ======================================

# Maven 构建阶段：与运行期保持 JDK 25 主版本一致
echo "======================================"
echo "正在拉取 Maven 构建镜像..."
docker pull docker.io/library/maven:3.10.0-eclipse-temurin-25
echo "Maven 构建镜像拉取完成！"
echo ""

# 前端构建阶段：Node.js 22 LTS（glibc，兼容 sass-embedded）
echo "======================================"
echo "正在拉取 Node.js 构建镜像..."
docker pull docker.io/library/node:22.23.5-bookworm-slim
echo "Node.js 构建镜像拉取完成！"
echo ""

# 后端运行期：JRE 25（固定补丁版本）
echo "======================================"
echo "正在拉取 Eclipse Temurin JRE 镜像..."
docker pull docker.io/library/eclipse-temurin:25.0.5_8-jre
echo "Eclipse Temurin JRE 镜像拉取完成！"
echo ""

# 前端运行期：nginx stable
echo "======================================"
echo "正在拉取 Nginx 运行期镜像..."
docker pull docker.io/library/nginx:1.30.6-alpine3.24
echo "Nginx 运行期镜像拉取完成！"
echo ""

# 可选回退：openjdk:25-jdk-slim。当前运行期镜像统一使用 eclipse-temurin:25.0.5_8-jre，
# 仅当需要切回 openjdk 时取消下面的注释。
# echo "======================================"
# echo "正在拉取 OpenJDK 回退镜像..."
# docker pull docker.io/library/openjdk:25-jdk-slim
# echo "OpenJDK 回退镜像拉取完成！"
# echo ""

# 统计本地镜像数量
IMAGE_COUNT=$(docker images | wc -l)
echo "本地镜像总数：$((IMAGE_COUNT - 1))"
echo "======================================"
