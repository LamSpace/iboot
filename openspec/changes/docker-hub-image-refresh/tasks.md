# Tasks

## 1. Preflight 与基线记录

- [x] 1.1 记录当前项目镜像清单与 digest：运行 `docker compose -f docker/docker-compose.yml --profile build --profile apm --profile observability --profile graph config --images`，对每个镜像执行 `docker image inspect --format '{{.RepoTags}} {{.RepoDigests}}'`，输出保存到 `/tmp/docker-hub-image-refresh/baseline.txt`；验证文件存在且包含全部当前标签。
- [x] 1.2 验证 Docker Hub 对全部目标镜像可达：对 design.md D2 表中的非 MinIO 镜像逐个执行 `docker manifest inspect <image>`，要求全部退出码为 0；任一失败即停止并记录镜像名。
- [x] 1.3 验证 MinIO 来源可达：检查 `https://docker.aityp.com/image/docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` 页面可访问，并确认页面展示的 `swr.cn-north-4.myhuaweicloud.com/ddn-k8s/...` 拉取路径；验证记录中同时包含 aityp URL 与 SWR 路径。
- [x] 1.4 检查 Docker 资源：执行 `docker system df` 与 `docker info`，确认磁盘与内存余量足够启动全量栈（参照 Docker VM 停容器后仍可能占用约 25GB 的已知情况）；必要时重启 Docker 释放内存，验证 `docker system df` 输出正常。

## 2. 环境清理（仅项目范围）

- [x] 2.1 停止宿主机开发进程与项目容器：执行 `scripts/dev-init.sh --stop`，再执行 `docker compose -f docker/docker-compose.yml --profile build --profile apm --profile observability --profile graph ps`；验证没有本项目容器处于 running 状态。
- [x] 2.2 删除项目数据卷：执行 `docker compose -f docker/docker-compose.yml --profile build --profile apm --profile observability --profile graph down -v --remove-orphans`；验证 `docker volume ls` 中不再存在本项目命名卷。
- [x] 2.3 仅删除本项目镜像：对 1.1 记录的镜像清单与 `iboot-backend-builder:local`、`otel/opentelemetry-collector-contrib:0.165.0` 执行 `docker image rm`（忽略不存在的镜像）；验证 `docker images` 中不再有本项目镜像，且 `getmeili/meilisearch:v1.49.0` 仍然存在。

## 3. 更新镜像引用

- [x] 3.1 重写 `scripts/pull-docker-images.sh`：非 MinIO 分支改为 `docker pull docker.io/<repo>:<tag>`；MinIO 分支保留 aityp/SWR `pull → tag → rmi` 并给 `rmi` 加 `|| true`；运行 `bash -n scripts/pull-docker-images.sh`，并验证脚本中除 MinIO 外不再出现 `swr.cn-north-4.myhuaweicloud.com`。
- [x] 3.2 更新 `docker/docker-compose.yml` 全部 `image:` 标签（含 `docker.elastic.co/...` → `library/...`）；运行 `docker compose -f docker/docker-compose.yml --profile build --profile apm --profile observability --profile graph config --images | sort -u`，验证输出与 design.md D2 矩阵一致且不含旧标签。
- [x] 3.3 更新 Dockerfile 基础镜像：`docker/Dockerfile.backend`、`docker/Dockerfile.dbinit` 改为 `eclipse-temurin:25.0.5_8-jre`；`docker/Dockerfile.backend-builder` 改为 `maven:3.10.0-eclipse-temurin-25`；`iboot-portal/Dockerfile` 改为 `node:22.23.5-bookworm-slim` 与 `nginx:1.30.6-alpine3.24`；用 `grep -rnE '^\s*FROM ' --include='Dockerfile*' .` 验证四处均为新标签。
- [x] 3.4 同步 `scripts/docker-deploy.sh` 的 `BASE_IMAGES` 为新的四个基础镜像标签，并更新脚本头部“SWR 镜像站”注释；用 `grep -n 'BASE_IMAGES' -A6 scripts/docker-deploy.sh` 验证与拉取脚本一致。
- [x] 3.5 更新 RocketMQ 5.5.0 路径：`docker/docker-compose.yml` 的 proxy 挂载路径与 `docker/rocketmq/init-topics.sh` 的 `ROCKETMQ_HOME` 默认值从 `rocketmq-5.3.1` 改为 `rocketmq-5.5.0`；用 `grep -rn 'rocketmq-5\.' docker/ scripts/` 验证不再出现 `5.3.1`。
- [x] 3.6 更新文档：`scripts/README.md` 的镜像来源说明改为 Docker Hub（MinIO 例外），并同步 `scripts/docker-deploy.sh` 头部说明；验证文档中不再把 SWR 描述为全部镜像的来源。

## 4. 拉取与标签核验

- [x] 4.1 执行 `scripts/pull-docker-images.sh`；验证退出码为 0，且输出中每个镜像的拉取步骤都成功。
- [x] 4.2 核验本地标签：对 design.md D2 矩阵中的每个镜像执行 `docker image inspect <image> --format '{{.RepoTags}}'`；验证全部存在且标签与 compose/Dockerfile 引用完全一致。
- [x] 4.3 核验 MinIO：执行 `docker image inspect docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z`，验证镜像存在；执行 `docker image inspect` 确认没有从 Docker Hub `minio/minio` 拉取的额外标签。
- [x] 4.4 核验镜像架构与基础信息：对关键镜像执行 `docker image inspect --format '{{.Os}}/{{.Architecture}}'`，验证均为 `linux/amd64`（与当前运行环境一致）。

## 5. 构建

- [x] 5.1 构建共享 builder 与后端运行期镜像：执行 `docker compose -f docker/docker-compose.yml --profile build build iboot-backend-builder`，再执行 `docker compose -f docker/docker-compose.yml build`；验证两条命令均退出 0。
- [x] 5.2 构建前端：在 `iboot-portal` 执行 `cnpm install && cnpm run build`（或 `scripts/build.sh frontend`）；验证 `iboot-portal/dist/` 存在且非空。
- [x] 5.3 验证运行期镜像不含构建工具：对任一后端运行期镜像执行 `docker run --rm --entrypoint sh <image> -c 'command -v mvn || command -v javac || true'`，验证输出为空；并确认 `java -version` 为 25。

## 6. 启动与健康

- [x] 6.1 启动容器化核心栈：执行 `scripts/docker-deploy.sh up full`（构建并使用新运行期镜像，含 observability 与 graph）；验证 `docker compose -f docker/docker-compose.yml --profile observability --profile graph ps --format '{{.Name}}|{{.State}}|{{.Health}}'` 中全部服务为 `running|healthy`。**注意**：`dev-init.sh` 运行宿主机 fat jar，不用于验证运行期镜像。
- [x] 6.2 启动 APM 栈：执行 `docker compose -f docker/docker-compose.yml --profile apm up -d --wait`；验证 SkyWalking BanyanDB、OAP、Horizon UI 均为 healthy。
- [x] 6.3 验证 RocketMQ 5.5.0 路径与进程：执行 `docker exec iboot-rocketmq-proxy ls /home/rocketmq/rocketmq-5.5.0/conf/rmq-proxy.json`，验证文件存在；并确认 namesrv/broker/proxy 均为 healthy。

## 7. 回归

- [x] 7.1 执行 `scripts/regress/regress.sh`；验证退出码为 0，且契约快照、路径守恒、路由覆盖、冒烟、权限矩阵全部 PASS（禁止使用 `--refresh`）。
- [x] 7.2 执行 Seata AT 回归：按脚本要求以 `SPRING_PROFILES_ACTIVE=seata-demo` 重启 `iboot-workflow`，执行 `scripts/regress/seata.sh`；验证退出码 0，完成后恢复默认 profile 并确认 workflow healthy。
- [x] 7.3 执行 Seata TCC 回归：以 `SPRING_PROFILES_ACTIVE=seata-tcc-demo` 重启 `iboot-workflow`，执行 `scripts/regress/seata-tcc.sh`；验证退出码 0，完成后恢复默认 profile 并确认 workflow healthy。
- [x] 7.4 执行日志管道回归：运行 `scripts/regress/log-pipeline.sh --drills`；验证退出码 0，且演练结束后 Kafka、Logstash、Elasticsearch 均恢复健康。
- [x] 7.5 执行链路追踪回归：发起一次审批请求，从响应头读取 `X-Trace-Id`，执行 `scripts/regress/trace-chain.sh <traceId> --expect iboot-gateway,iboot-workflow`；验证退出码 0。
- [ ] 7.6 执行等价性回归：运行 `scripts/regress/equivalence.sh --verify --service file`（以及 notify/job/workflow/monitor）；验证全部退出码 0。
- [x] 7.7 验证 APM 辅助栈：访问 SkyWalking Horizon UI（`http://localhost:18082`）与 OAP 健康端点，验证 UI 可加载、OAP healthy，且 BanyanDB 无重启。
- [x] 7.8 验证前端：执行 `curl -fsS http://localhost/`，验证返回 200 且页面包含门户入口；登录 `admin/admin123456` 验证可进入系统。

## 8. 收尾与回滚

- [x] 8.1 汇总回归证据：保存每步命令、退出码、`docker compose ps` 与失败日志到 `/tmp/docker-hub-image-refresh/regression.txt`；验证文件包含全部步骤且无未记录的失败。
- [x] 8.2 若任一步失败：按 Migration Plan 回滚（revert 本次变更 → 按旧矩阵重新 pull → 重建 → 重启），验证旧栈恢复 healthy；在变更说明中记录失败组件与证据。
- [ ] 8.3 提交变更：在 `feat/docker-hub-image-refresh` 分支提交（含 OpenSpec 工件与代码/脚本修改），推送并用 `gh pr create --base master --head feat/docker-hub-image-refresh` 创建 PR；验证 `gh pr view` 能查到 PR。

## Workflow follow-up

- 评审通过后使用 `gh pr merge <number> --merge --delete-branch` 合并，并更新本地 `master`。
- 变更归档后评估 MinIO 安全升级路径（社区重建镜像 / 自建镜像 / 替代 S3 实现），作为独立变更处理。
