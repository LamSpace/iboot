# Proposal

## Why

当前 `scripts/pull-docker-images.sh` 将全部镜像统一从华为 SWR 镜像站拉取，镜像版本停留在项目搭建时的时间点。其中 MySQL 8.0 已过 EOL，Redis、Prometheus、Grafana、Kafka、Nacos 等组件也落后多个 minor/major 版本；同时 MinIO 上游自 2025-10 起改为 source-only distribution，Docker Hub 与 Quay 的官方镜像均已不可用。需要把镜像来源与版本基线一次性刷新到可复现的稳定状态，并用完整回归证明系统行为没有漂移。

## What Changes

- 重写 `scripts/pull-docker-images.sh` 的镜像来源：
  - 除 MinIO 外的全部镜像改为直接从 Docker Hub 拉取，并使用各开源组件在 Docker Hub 上的官方仓库。
  - MinIO 继续使用 `https://docker.aityp.com/` 展示的 SWR 镜像路径，拉取后重打为编排引用的标签。
- 按 **LTS / 稳定发行版优先** 升级镜像版本，核心变化包括：
  - MySQL `8.0.45` → `8.4.13`（8.4 LTS；8.0 已 EOL）
  - Redis `7.0.14` → `8.8.4`
  - Neo4j `5.26.29` → `5.26.33`（5.26 LTS）
  - Elasticsearch / Logstash / Kibana `9.2.3` → `9.5.5`（Docker Hub 官方镜像）
  - Prometheus `v2.53.4` → `v3.16.0`；Grafana `12.2.0` → `13.2.6`
  - Nacos `v3.1.1` → `v3.3.0`；Seata `2.5.0` → `2.6.0`；RocketMQ `5.3.1` → `5.5.0`；Kafka `4.1.1` → `4.4.0`
  - SkyWalking OAP `11.0.0` → `11.1.0`、BanyanDB `0.11.0` → `0.11.2`、Horizon UI `1.0.0` → `1.1.0`（三个版本必须同批升级）
  - Tempo `2.10.8` → `2.10.9`（2.11/2.12 尚未发布稳定版）
  - Prometheus 生态 exporter、Thanos `0.36.1` → `0.42.5`、OTel Collector `0.140.0` → `0.165.0`
  - 构建/运行期基础镜像：Maven `3.9.14` → `3.10.0`（Temurin 25）、Node `22.23.5-bookworm-slim`、Temurin `25.0.5_8-jre`、Nginx `1.30.6-alpine3.24`
- 同步更新 `docker/docker-compose.yml`、全部 Dockerfile 与文档中的镜像标签；只改拉取脚本会导致编排仍引用旧标签。
- **BREAKING**：修改 `container-deployment` 与 `apm-comparison` 中“基础镜像不得直连 Docker Hub”的既有约束，改为“非 MinIO 镜像以 Docker Hub 为唯一来源，MinIO 走 aityp/SWR”。
- **BREAKING**：MySQL 8.0→8.4、Redis 7→8、Prometheus 2→3、Grafana 12→13 等跨版本升级不保证兼容旧数据卷；本项目 Docker 环境按“每次测试后删除数据卷”的临时环境处理，变更中会显式清理项目卷。
- 清理范围限定为**本项目相关镜像**与项目数据卷，不删除 `getmeili/meilisearch` 等无关镜像。
- 拉取完成后执行完整回归：全栈启动、`regress.sh`（契约/路径/路由/冒烟/权限）、Seata AT/TCC、日志管道、链路追踪、前端构建。
- **已知风险**：MinIO 官方镜像最后可用版本仍是 `RELEASE.2025-09-07T16-13-09Z`，2025-10-15 安全修复版没有容器镜像；本次保持该版本并在设计中记录风险与后续选项。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `container-deployment`: 修改“干净检出可完成全栈构建”与“基础镜像拉取覆盖全部构建与运行期镜像”中的镜像来源约束，改为 Docker Hub 官方镜像（MinIO 除外），并更新固定标签要求。
- `apm-comparison`: 修改“镜像与版本来源可复现”中的镜像站约束，改为 Docker Hub（MinIO 除外），同时保留 OAP/UI/存储版本必须精确匹配的要求。

## Impact

- 脚本与编排：`scripts/pull-docker-images.sh`、`docker/docker-compose.yml`、`scripts/README.md`。
- 构建镜像：`docker/Dockerfile.backend`、`docker/Dockerfile.backend-builder`、`docker/Dockerfile.dbinit`、`iboot-portal/Dockerfile`。
- RocketMQ：`docker/docker-compose.yml` 与 `docker/rocketmq/init-topics.sh` 中硬编码的 `rocketmq-5.3.1` 路径需随 `5.5.0` 更新。
- 本地 Docker 环境：删除本项目镜像与数据卷后重新拉取；不触碰无关镜像。
- 回归：`scripts/dev-init.sh`、`scripts/regress/regress.sh` 及其分层脚本、`scripts/build.sh`。
- 外部依赖：Docker Hub 可达性（当前 daemon 已配置 registry mirror `hubproxy.docker.internal:5555`）；aityp/SWR 仅用于 MinIO。
