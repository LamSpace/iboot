# Design

## Context

动机见 `proposal.md`。本设计只回答“怎么清理”，并记录已确认的取舍。

当前需要约束实现的三类事实：

1. **文档事实已经分叉**：`doc/03` 有 37 处 `com.iboot.admin`，`doc/05` 仍以 `IbootAdminApplication` 和单模块启动为主，`doc/08` 的 Docker 章节还是 Java 17 + 每模块 Dockerfile，`doc/09`/`doc/10` 仍写 Flyway 由服务启动时执行。根 README 同时存在“十一个后端模块”“openjdk:25-jdk-slim”“Compose 应用编排仍注释”三处过期描述。
2. **活跃引用与历史引用必须分开处理**：活跃文件约 36 处 `doc/` 引用（README、ROADMAP、CLAUDE.md、`.dockerignore`、源码注释、docs 内部链接）；`openspec/changes/archive/` 有 84 个文件引用旧路径，但它们是历史记录。
3. **一次性资产已有替代物**：`spike/microservice-gate` 不在 reactor；`docker/deploy.sh` 已被 `scripts/docker-deploy.sh` 取代；`scripts/migrate/*.sql` 已完成五次拆分搬迁；`scripts/install-dependencies.sh` 无调用方。

## Goals / Non-Goals

**Goals:**

- 让 `docs/` 成为唯一文档根目录，活跃引用零残留。
- 让每类事实只有一个维护位置：版本 → `docs/02`；开发启动 → `docs/05`；容器部署 → `docker/README.md`；服务拓扑与 schema → `docs/23`/`docs/07`；根 README 只保留上手与导航。
- 删除不再有执行价值的一次性资产，同时保留其决策证据。
- 修复清理过程中确认的三处小缺陷，不扩大为重构。

**Non-Goals:**

- 不重写 `docs/12`–`docs/18`、`docs/20`–`docs/23` 的正文；本次只做重命名、旧称修正和链接修复。
- 不调整 Compose 的服务集合、端口、镜像版本或运行时配置。
- 不改任何 Java 业务代码、API、数据库 schema。
- 不把 `docs/spike/` 的报告迁入 archive，也不改写 archive 里的历史路径。

## Decisions

### D1. 用 `git mv` 重命名，只改活跃引用

`git mv doc docs` 保留历史；随后只更新活跃文件。archive 保持原样，并在 `docs/README.md` 顶部注明“本目录于 2026-11-06 由 `doc/` 重命名，archive 中的旧路径是历史记录”。

**备选**：全仓机械替换 84 个 archive 文件。**不采用**：会改写已归档的变更文本，收益只是让 `grep` 干净，代价是历史失真。

### D2. spike 删代码、留报告

删除 `spike/microservice-gate/`，保留 `docs/spike/` 的 5 份报告，新增 `docs/spike/README.md` 索引，列出报告、对应变更、结论与“代码已删除，复现步骤见报告”。

**理由**：报告被 5 个 archived design/proposal 引用，且记录了版本矩阵与踩坑；代码是版本锁死的一次性复现工程，已无维护价值。

### D3. 模块 README 收敛，而不是扩张

删除 `iboot-system/README.md`、`iboot-portal/README.md`、`iboot-system/VIRTUAL_THREADS.md`。删除前先把其中仍然正确的独有信息并入 `docs/05`（前端 cnpm、环境变量、开发端口）与 `docs/16`（虚拟线程），避免信息净损失。

**备选**：给 10 个后端模块各加一份模板 README。**不采用**：这些事实在 `docs/23` 已有权威版本，新增 10 份只会制造新的漂移点；两份现有 README 的过期程度已经证明了这一点。

### D4. 删除 `scripts/migrate/`，把回滚步骤改为历史记录

删除 10 个搬迁 SQL，并修改 `docs/23` 中引用它们的章节：保留“何时搬迁、如何校验、结果如何”的文字记录，删除可执行命令，改为指向 `git log -- scripts/migrate` 取回历史脚本。

**理由**：项目尚未对外发布，没有需要在线回滚的旧版本；保留可执行 SQL 反而会让读者误以为仍属常规运维流程。**代价**：旧 runbook 不再可直接执行，这是有意的 BREAKING。

### D5. Docker 清理与三处缺陷修复

- 删除 `docker/deploy.sh`：无调用方，且 archived 任务 5.3 的验收标准就是“仓库中不再有指向旧流程的可执行入口”。保留 `init.sql`（被 mysql 容器挂载）。
- `scripts/docker-deploy.sh` 的 `up` 默认值改为 `observability`，与脚本头注释和 `docker/README.md` 一致；`up core` 仍是轻量入口。
- Grafana provisioning 收敛为两个 provider：新建 `dashboards/vendor/` 存放 10 个社区面板（删除无 exporter 的 Nginx 面板 14900），provider 1 指向 `vendor/`，provider 3 保留 `custom/`；删除指向不存在目录的 provider 2。这样既避免 `custom/` 被递归加载两次，也避免不存在的 `springboot/` 目录产生 provisioning 错误。
- `scripts/dev-init.sh` 的 `MAVEN_SETTINGS` 改为：优先取环境变量；未设置时，若 `$HOME/repo/settings.xml` 存在则使用；否则不加 `-s`。保持本机行为不变，同时去掉公共仓库里的 `/home/lam` 硬编码。

### D6. 文档内容按“事实归属”修订

本次修订范围限定为已确认的 P0/P1：

| 文档 | 修订内容 |
|---|---|
| `docs/README.md` | 修坏链、去掉重复的 19/16 条目、更新结构树与启动入口、加改名说明 |
| `docs/03` | `com.iboot.admin` → `com.iboot.system`；§3.5/§3.6 更新为当前 12 模块拓扑与 db-init 迁移归属 |
| `docs/05` | 启动流程改为 `scripts/dev-init.sh` 为主；删除 `IbootAdminApplication`、`docker-compose` v1、`Dockerfile.admin/portal`；npm/cnpm 口径统一 |
| `docs/08` | §8.9 删除“启动时自动 Flyway”；§8.10–8.13 用指向 `docker/README.md` 的说明替代旧 Compose/Dockerfile 全文；CI 章节改用共享镜像模板 |
| `docs/09` | 包名、Flyway（`enabled: false`，迁移归 db-init）、Quartz（归 iboot-job）、文件存储/ES 的服务归属 |
| `docs/10` | 包名、Flyway、Quartz 同上 |
| 根 README | 12 个模块、eclipse-temurin 共享模板、Compose 已全量编排、目录树去掉 migrate/install-dependencies |
| `docs/04`、`docs/22` | “admin”旧称改为“系统服务 / iboot-system” |
| 7 份操作文档 | `/home/lam/...` 改为 `mvn` 或 `$MAVEN_SETTINGS`，并注明可自行追加 `-s` |

`docs/12`–`18`、`docs/20`–`23` 只做重命名、链接与明显旧称修正，不逐行校对。

### D7. 新增 `scripts/README.md`

用一张表区分：开发（`dev-init.sh`、`build.sh`）、部署（`docker-deploy.sh`、`pull-docker-images.sh`）、回归（`regress/`）。明确说明 `migrate/` 已删除、`install-dependencies.sh` 已删除。

## Risks / Trade-offs

- **[重命名漏改引用]** → 用 `grep -rn 'doc/'`（排除 archive）和 Markdown 链接检查双重验收；`.dockerignore`、源码注释、docs 内部链接都在检查范围内。
- **[删除 migrate 后 `docs/23` 出现悬空命令]** → 逐个章节改写并保留搬迁结果；`git log -- scripts/migrate` 作为取回路径写入文档。
- **[删除模块 README 丢失前端独有说明]** → 删除前把 cnpm、`VITE_API_BASE_URL=/iboot/api`、开发端口 3000/8088 并入 `docs/05`。
- **[Grafana 面板重复加载的判断有误]** → 修改前后各启动一次 observability profile，用 Grafana `/api/search` 或 provisioning 日志核对面板数量；若递归假设不成立，退化为只删除 provider 2 与 Nginx 面板。
- **[`up` 默认改为 observability 增加资源占用]** → `docker/README.md` 已有 16GB 提示和 `up core` 入口；不改默认资源上限。
- **[archive 中的旧链接失效]** → 已接受；`docs/README.md` 顶部说明重命名事实，避免读者误判。
- **[删除 `install-dependencies.sh` 后新成员缺少引导]** → 根 README 的“前置要求”已列 JDK 25、Node、Maven、Docker；`scripts/README.md` 再指向该节。

## Migration Plan

1. `git mv doc docs`，同步 `.dockerignore` 的 `doc` → `docs`。
2. 批量替换活跃引用；跑一次链接检查，确认没有遗漏。
3. 删除 spike 代码、模块 README、`VIRTUAL_THREADS.md`、`deploy.sh`、`install-dependencies.sh`、`scripts/migrate/`。
4. 修复三处缺陷与 Grafana provisioning 布局。
5. 按 D6 修订文档内容，新增 `scripts/README.md`。
6. 全量验收：链接、`doc/` 残留、包名残留、`bash -n`、`docker compose config -q`、Grafana 面板数量、`git status` 复核。

回滚：本次不涉及运行时状态，`git revert` 单个变更即可；archive 与数据库均未改动。

## Open Questions

无。
