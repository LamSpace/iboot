# Tasks

## 1. 收敛宿主机端口绑定

- [x] 1.1 把九个服务（8080-8089）、MySQL、Redis、Silo、ES、Nacos、Sentinel、Seata、RocketMQ Proxy、Kafka、Tempo、SkyWalking、Prometheus、Grafana、Alertmanager、Kibana、Neo4j 的宿主端口改为 `127.0.0.1:<host>:<container>`；验证：`docker compose -f docker/docker-compose.yml config` 中这些端口均带 `127.0.0.1` 前缀，且门户仍为 `80:80` / `443:443`。
- [x] 1.2 移除 Logstash 5044、SkyWalking Banyandb 17913、SkyWalking OAP 17128、Thanos 19191/19192/19193/19194、四个 exporter 9100/9104/9114/9121 的宿主映射；验证：`docker compose config` 中不再出现这些宿主端口。
- [x] 1.3 更新 `docs/08-部署指南.md` 的宿主机端口表，标注公开端口与仅回环端口；验证：表中每一项与 `docker compose config` 的端口映射一致，且不再把已移除端口列为可用。

## 2. 按角色划分编排网络

- [x] 2.1 在 `docker/docker-compose.yml` 定义 `edge`、`app`、`data`、`obs` 四个网络，并把 `x-app-common` 锚点的默认网络改为 `app` + `data`；验证：`docker compose config` 的 `networks` 段包含四个网络，业务服务默认同时属于 `app` 与 `data`。
- [x] 2.2 门户只加入 `edge`；网关、系统服务、运维控制台覆盖为 `edge` + `app` + `data`；验证：三个服务与门户的 `networks` 列表符合设计表，门户不含 `data`。
- [x] 2.3 中间件只加入 `data`；初始化任务加入 `data`；Prometheus 加入 `app` + `obs`；Grafana、Kibana、Logstash、Thanos 按设计加入 `data` 与 `obs`；验证：`docker compose config` 中每个服务的网络归属与设计表一致。
- [x] 2.4 更新 `docs/03-架构设计.md` 的网络分区说明与 `docs/08-部署指南.md` 的网络表；验证：文档中的成员列表与 `docker compose config` 一致，并明确「门户不能直达数据面」。

## 3. 适配 Prometheus 的两种运行模式

- [x] 3.1 新增 `docker/scripts/prometheus/prometheus.docker.yml`，把九个服务的目标改为 `iboot-system:8080` 等服务名；保持 `prometheus.yml` 为宿主机开发目标；验证：用 Prometheus 镜像的 `promtool check config` 校验两份配置均通过。
- [x] 3.2 修改 compose 的 Prometheus `--config.file` 默认指向 `prometheus.docker.yml`，并在 `scripts/dev-init.sh` 启动可观测组件前导出 `IBOOT_PROMETHEUS_CONFIG=prometheus.yml`；验证：`bash -n scripts/dev-init.sh scripts/docker-deploy.sh` 通过，`docker compose config` 默认指向 Docker 配置，宿主机模式指向宿主配置。
- [x] 3.3 在 `docs/08-部署指南.md` 说明两种模式各用哪份 Prometheus 配置；验证：按文档分别启动宿主机开发与 Docker 全量后，Prometheus 的服务 target 均为 up。

## 4. 暴露面回归门禁

- [x] 4.1 新增 `scripts/regress/exposure.sh`：用 `docker inspect` 校验端口绑定、用宿主机非回环地址校验不可达、用容器执行门户到数据面的负向与业务到依赖的正向连通性；容器不存在时明确跳过矩阵。验证：`bash -n` 通过；在宿主机开发栈上运行输出端口检查通过并说明矩阵跳过。
- [x] 4.2 为 `exposure.sh` 增加失败用例验证：临时启动一个绑定 `0.0.0.0` 的容器，确认脚本失败并指出端口，移除后恢复通过；验证：两次运行的退出码与输出符合预期。
- [x] 4.3 把 `exposure.sh` 接入 `scripts/regress/regress.sh`（TLS 检查之后、冒烟之前）；验证：全量回归输出包含「暴露面」步骤，且任一步失败时总退出码非零。
- [x] 4.4 在 `docs/27-TLS 与零信任操作指南.md` 新增「暴露面与网络分段」章节，更新范围边界与后续批次状态；验证：文档中的验收命令可原样执行，且明确「TLS 加密不等于零信任身份」。

## 5. 集成验证

- [x] 5.1 宿主机开发模式：执行 `scripts/dev-init.sh` 后运行完整 `scripts/regress/regress.sh`；验证：暴露面检查、内部 TLS、日志检索 TLS、冒烟与权限矩阵全部通过，宿主机仍可通过 `localhost` 使用各中间件。
- [x] 5.2 Docker 全量模式：执行 `scripts/docker-deploy.sh up full` 后运行 `scripts/regress/exposure.sh` 与完整回归；验证：门户只能访问网关/系统/控制台，门户到 MySQL/Redis/ES/Nacos 全部失败，业务到依赖全部成功，Prometheus 十个服务 job 全部为 up。Docker 下备份按设计关闭，冒烟的备份用例为既有预期失败（43/44），其余全部通过。
- [x] 5.3 回滚演练：恢复上一版 compose 与脚本后重新启动；验证：容器恢复为上一版端口与网络，数据卷与数据不变。

## Workflow follow-up

- 同步规格并归档本变更。
- 提交功能分支、创建 PR、合并到 `master` 后手动触发一次全栈回归。
