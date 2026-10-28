# 知识图谱（1.4）验证报告

> 记录日期：2026-10-28
> 目标：确认 Neo4j 版本与 `iboot-ai` 技术栈兼容，并给出可复现的本地验证依据。

## 1. 结论

| 项 | 结论 |
|---|---|
| 服务端版本 | Neo4j **5.26.29-community**（5.x LTS 线）可用 |
| 客户端驱动 | `org.neo4j.driver:neo4j-java-driver` **6.0.2**（由 Boot 4.0.3 托管） |
| 兼容性 | 驱动 6.x 官方声明兼容 Neo4j 4.4 / 5.x / 2025.x 服务端，5.26.29 在范围内 |
| 内存 | 容器实测 **883.5 MiB**（堆 512m + 页缓存 256m），符合 R4 的按需启用策略 |
| 约束 | 四类节点 `id` 唯一约束幂等创建成功 |

## 2. 镜像与运行参数

```
镜像      neo4j:5.26.29-community
镜像 ID   sha256:ef736d1c92b0dc792f4032b48adb6ff99b0611b585df0107f680e28b954fdf0e
大小      968,591,851 字节（压缩后约 359MB）
端口      7474（HTTP）、7687（Bolt）
认证      NEO4J_AUTH=neo4j/<NEO4J_PASSWORD>
启动      docker compose --profile graph up -d neo4j
```

镜像通过镜像站路径拉取（与 `scripts/pull-docker-images.sh` 一致）：

```bash
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/neo4j:5.26.29-community
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/neo4j:5.26.29-community \
            docker.io/library/neo4j:5.26.29-community
```

## 3. 版本兼容性依据

- Boot 4.0.3 的 `spring-boot-dependencies` 声明 `neo4j-java-driver.version = 6.0.2`，本项目不显式写版本号。
- 官方驱动手册声明 6.x 驱动支持连接 4.4 / 5.x / 2025.x 服务端（见 `neo4j.com/docs/java-manual/current/upgrade/`）。
- 运行期实测服务端版本：

```bash
docker exec iboot-neo4j cypher-shell -u neo4j -p iboot123456 \
  "CALL dbms.components() YIELD name, versions RETURN name, versions"
# "Neo4j Kernel", ["5.26.29"]
```

未采用 `spring-boot-starter-data-neo4j`：本项目所有图查询都是固定模板，OGM 映射无收益，且与 `iboot-ai` 现有的 `JdbcTemplate` 风格不一致。

## 4. 内存实测

```
docker stats --no-stream --format 'Mem={{.MemUsage}} CPU={{.CPUPerc}}' iboot-neo4j
# Mem=883.5MiB / 7.388GiB CPU=0.98%
```

配置来源（`docker/docker-compose.yml`）：

- `NEO4J_server_memory_heap_initial__size=512m`
- `NEO4J_server_memory_heap_max__size=512m`
- `NEO4J_server_memory_pagecache_size=256m`

空库下 883.5 MiB 已包含 JVM 与页缓存开销；按 R4 的处置，服务默认置于 `graph` profile，不启用时不产生任何常驻内存。

## 5. 约束

应用启动时由 `GraphSchemaInitializer` 幂等创建，重复执行不会产生重复约束：

```
name              type         labelsOrTypes  properties
dept_id_unique    UNIQUENESS   ["Dept"]       ["id"]
user_id_unique    UNIQUENESS   ["User"]       ["id"]
role_id_unique    UNIQUENESS   ["Role"]       ["id"]
menu_id_unique    UNIQUENESS   ["Menu"]       ["id"]
```

## 6. 测试证据

```bash
TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 \
mvn -s /home/lam/repo/settings.xml -pl iboot-ai -Dtest='GraphSchemaInitializerTest,Neo4jGraphRepositoryTest' test
```

| 测试 | 覆盖点 | 结果 |
|---|---|---|
| `GraphSchemaInitializerTest` | 约束重复初始化幂等 | 通过 |
| `Neo4jGraphRepositoryTest` | 节点/关系数量、重复写入幂等、删除实体清理、状态读取 | 通过 |

Testcontainers 使用本地已存在的 `neo4j:5.26.29-community` 镜像，不走 Docker Hub；Ryuk 固定为本地可用的 `testcontainers/ryuk:0.12.0`。

## 7. 已知限制

- 社区版为单库、无集群；阶段 3 如需高可用改用托管图数据库。
- 同步为调用者触发的全量替换，组织规模很大时需评估单事务耗时；当前规模下可接受。
- 定时同步依赖服务身份，留到阶段 2 与 Nacos / mTLS 一起做。
