# Proposal

## Why

微服务拆分与 Docker 全栈部署完成后，仓库里的说明性资产没有同步收敛：

- 同一组事实（版本、端口、启动方式、服务拓扑）被复制到根 README、`doc/README.md`、`doc/02/05/08` 和两份模块 README，已经出现确定漂移。典型例子：`com.iboot.admin` 在 `doc/03` 残留 37 处、`IbootAdminApplication`、`Dockerfile.admin`、`docker-compose` v1，以及根 README 仍称“Compose 应用编排块处于注释状态”。
- `doc/` 与常见的 `docs/` 约定不一致，且 `.dockerignore` 仍排除旧的 `doc`，改名后需要同步，否则文档会进入 Docker 构建上下文。
- 一次性资产仍有可执行入口：`spike/microservice-gate` 不在根 `pom.xml` 的 modules 中；`docker/deploy.sh` 只是兼容壳；`scripts/migrate/*.sql` 已完成五次拆分搬迁；`scripts/install-dependencies.sh` 没有任何流程引用。
- 两份模块 README 与 `iboot-system/VIRTUAL_THREADS.md` 长期未维护（例如门户 README 仍写 Node 16、npm、`localhost:8080`；系统 README 仍写 MyBatis-Plus），继续保留只会扩大重复与误导读者的面积。

现在清理的原因：功能进入稳定期，继续让文档、脚本与实现分叉，会让下一次排障和新成员接入付出更高成本。

## What Changes

- **BREAKING**：`doc/` 重命名为 `docs/`，更新全部活跃引用（根 README、`ROADMAP.md`、`CLAUDE.md`、`.dockerignore`、源码注释、docs 内部链接）。`openspec/changes/archive/` 下的历史文本保持原样，并在 `docs/README.md` 注明重命名。
- 删除 `spike/microservice-gate/`（一次性门禁工程）；保留 `doc/spike/` 的 5 份报告并新增索引，说明每份报告对应的变更、结论与代码去向。
- 删除 `iboot-system/README.md`、`iboot-portal/README.md` 与 `iboot-system/VIRTUAL_THREADS.md`；模块事实收敛到根 README、`docs/05` 与 `docs/23`。
- **BREAKING**：删除 `scripts/migrate/`（10 个历史搬迁 SQL）；更新 `docs/23` 中引用这些脚本的回滚步骤，改为历史记录，不再提供可执行回滚。同步修正根 README 的目录树。
- 删除 `docker/deploy.sh`（兼容壳）与 `scripts/install-dependencies.sh`（无引用且不完整的系统级安装脚本）。
- 修复三处实现缺陷：`scripts/docker-deploy.sh` 的 `up` 默认范围与文档不一致；Grafana provisioning 指向不存在的 `springboot` 目录并重复加载 `custom` 面板；`scripts/dev-init.sh` 硬编码 `/home/lam/repo/settings.xml`。
- 修订文档内容：`docs/README.md`、`docs/03`、`docs/05`、`docs/08`、`docs/09`、`docs/10` 的包名、Flyway/Quartz 归属、启动与部署命令；根 README 的模块数、镜像模板与 Compose 现状；`docs/04`、`docs/22` 的“admin”旧称；7 份文档中绑定本机路径的 Maven 命令。
- 新增 `scripts/README.md`，区分开发脚本、运维脚本、回归脚本与历史脚本边界。

## Capabilities

### New Capabilities

无。本变更只调整文档、目录布局与脚本，不改变任何运行时行为。

### Modified Capabilities

无。三处缺陷修复不改变已登记的能力契约；specs 阶段通过 `.openspec.yaml` 的 `skip_specs: true` 跳过。

## Impact

- 文档：`doc/` → `docs/` 及 10 份内容修订；根 README、`ROADMAP.md`、`CLAUDE.md`。
- 脚本：删除 `docker/deploy.sh`、`scripts/install-dependencies.sh`、`scripts/migrate/`；修改 `scripts/docker-deploy.sh`、`scripts/dev-init.sh`；新增 `scripts/README.md`。
- 部署配置：`docker/scripts/grafana/provisioning/dashboards/default.yaml`；不改变 Compose 服务集合与端口。
- 运行时行为：无变更；不改 API、数据库 schema、依赖版本。
- 外部影响：旧文档链接与旧回滚命令失效，属于有意的 BREAKING 清理。
