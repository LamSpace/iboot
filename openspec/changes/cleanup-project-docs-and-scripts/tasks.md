# Tasks

## 1. 目录重命名与活跃引用

- [x] 1.1 执行 `git mv doc docs`，并把 `.dockerignore` 中的 `doc` 改为 `docs`；验证：`test -d docs && test ! -d doc` 通过，`grep -n '^docs$' .dockerignore` 有输出。
- [x] 1.2 更新 `README.md`、`ROADMAP.md`、`CLAUDE.md` 中的 `doc/` 引用；验证：`grep -rn 'doc/' README.md ROADMAP.md CLAUDE.md` 无输出。
- [x] 1.3 更新 Java 注释与 `application.yml` 中的 `doc/` 引用（`iboot-ai`、`iboot-file`、`iboot-job`、`iboot-monitor`、`iboot-notify`、`iboot-workflow`）；验证：`grep -rn 'doc/' iboot-*/src --include='*.java' --include='*.yml'` 无输出。
- [x] 1.4 更新 `docs/` 内部相互引用（`docs/02`、`docs/07`、`docs/20`、`docs/23`）；验证：`grep -rn 'doc/' docs/*.md` 无输出。

## 2. Spike 清理

- [x] 2.1 删除 `spike/` 目录；验证：`test ! -d spike` 通过，`grep -rn 'spike/microservice-gate' . --exclude-dir=.git --exclude-dir=openspec` 无输出。
- [x] 2.2 新增 `docs/spike/README.md` 索引，列出 5 份报告、对应 archived 变更、结论与“代码已删除，复现步骤见报告”；验证：索引中的每个链接目标都存在，`ls docs/spike/*.md | wc -l` 为 6。
- [x] 2.3 核对 `docs/02` 与归档设计文档对 spike 报告的引用仍可解析；验证：`grep -rn 'docs/spike/' docs/*.md` 的路径全部存在。

## 3. 一次性脚本与模块 README 清理

- [x] 3.1 删除两份模块 README 前，把仍有效的独有信息并入 `docs/05`：cnpm、`VITE_API_BASE_URL=/iboot/api`、开发端口 3000/8088；验证：`grep -n 'cnpm\|/iboot/api\|3000\|8088' docs/05-快速开始.md` 均有命中。
- [x] 3.2 删除 `iboot-system/README.md`、`iboot-portal/README.md`、`iboot-system/VIRTUAL_THREADS.md`；验证：三个路径均不存在，且 `docs/16-虚拟线程指南.md` 仍覆盖虚拟线程配置。
- [x] 3.3 删除 `docker/deploy.sh` 与 `scripts/install-dependencies.sh`，同步修正 `docker/README.md` 中的兼容入口说明和根 README 的目录树；验证：`grep -rn 'docker/deploy.sh\|install-dependencies.sh' . --exclude-dir=.git --exclude-dir=openspec` 无输出。
- [x] 3.4 删除 `scripts/migrate/` 及其 10 个 SQL；验证：`test ! -d scripts/migrate` 通过。
- [x] 3.5 改写 `docs/23` 中引用搬迁 SQL 的章节：保留搬迁结果与校验记录，删除可执行命令，注明用 `git log -- scripts/migrate` 取回；验证：`grep -rn 'scripts/migrate' docs/` 无输出，相关章节仍说明搬迁日期与结论。
- [x] 3.6 新增 `scripts/README.md`，按开发 / 部署 / 回归分类列出剩余脚本，并注明 migrate 与 install-dependencies 已删除；验证：表中每个路径都存在，且不含已删除文件。

## 4. Docker 与脚本缺陷修复

- [x] 4.1 修正 `scripts/docker-deploy.sh` 的 `up` 默认范围为 `observability`，与脚本头注释和 `docker/README.md` 一致；验证：`bash -n scripts/docker-deploy.sh` 通过，`grep -n 'up "${1:-observability}"' scripts/docker-deploy.sh` 有命中。
- [x] 4.2 修正 `scripts/dev-init.sh` 的 `MAVEN_SETTINGS`：优先环境变量，其次 `$HOME/repo/settings.xml`（存在时），否则不加 `-s`；验证：`bash -n scripts/dev-init.sh` 通过，`grep -n '/home/lam' scripts/dev-init.sh` 无输出。
- [x] 4.3 重组 Grafana 面板：新建 `docker/scripts/grafana/provisioning/dashboards/vendor/`，移入 10 个社区面板，删除无 exporter 的 `14900_rev2.json`，更新 `default.yaml` 为 vendor + custom 两个 provider 并移除不存在的 `springboot` provider；验证：Python YAML 解析通过，`grep -n 'springboot' docker/scripts/grafana/provisioning/dashboards/default.yaml` 无输出。
- [x] 4.4 启动 observability profile 校验 Grafana provisioning；验证：`docker compose -f docker/docker-compose.yml --profile observability up -d grafana` 后，Grafana 日志无 provisioning 错误，且 custom 面板各只出现一次（可用 `/api/search` 或日志核对）。
- [x] 4.5 校验 Compose 文件未因清理产生语法或引用问题；验证：`docker compose -f docker/docker-compose.yml config -q` 退出码为 0。

## 5. 文档内容修订

- [x] 5.1 重写 `docs/README.md`：修复 `19-虚拟线程指南.md` 坏链、去除 16/19 重复、更新项目结构树与启动入口、加入 `doc/` → `docs/` 改名说明；验证：链接检查脚本对该文件报 0 个坏链，且文中包含重命名说明。
- [x] 5.2 修订 `docs/03-架构设计.md`：`com.iboot.admin` → `com.iboot.system`，§3.5/§3.6 更新为当前 12 模块拓扑与 db-init 迁移归属；验证：`grep -c 'com.iboot.admin' docs/03-架构设计.md` 输出 0，结构章节列出 12 个模块。
- [x] 5.3 修订 `docs/05-快速开始.md`：以 `scripts/dev-init.sh` 为主启动入口，移除 `IbootAdminApplication`、`Dockerfile.admin`、`Dockerfile.portal`、`docker-compose` v1，统一 cnpm 口径；验证：`grep -n 'IbootAdminApplication\|Dockerfile.admin\|Dockerfile.portal\|docker-compose '` 无输出。
- [x] 5.4 修订 `docs/08-部署指南.md`：删除“启动时自动 Flyway”，用指向 `docker/README.md` 的说明替代旧 Compose/Dockerfile 全文，CI 章节改用共享镜像模板；验证：文中不再出现 Java 17 Dockerfile、`iboot-system/Dockerfile` 与 `database/init.sql`。
- [x] 5.5 修订 `docs/09-后端技术架构详解.md` 与 `docs/10-后端配置详解.md`：包名、Flyway（禁用，迁移归 db-init）、Quartz（归 iboot-job）、文件/ES 服务归属；验证：两份文档 `grep -c 'com.iboot.admin'` 均为 0，且 `flyway.enabled` 描述为 `false`。
- [x] 5.6 修订根 `README.md`：模块数 12、eclipse-temurin 共享镜像模板、Compose 已全量编排、目录树去掉 migrate/install-dependencies；验证：文中不再出现“十一个后端模块”“openjdk:25-jdk-slim”“注释状态”。
- [x] 5.7 修订 `docs/04-功能模块.md` 与 `docs/22-AI 工作台操作指南.md` 中的“admin”旧称；验证：两文中指代服务的 `admin` 均已替换为 `iboot-system` 或“系统服务”。
- [x] 5.8 清理 7 份操作文档中的本机路径（`docs/05/08/19/20/21/22/23`）；验证：`grep -rn '/home/lam' docs/` 无输出，命令仍可直接执行（`mvn` 或 `$MAVEN_SETTINGS`）。

## 6. 集成验收

- [x] 6.1 对全部 Markdown 运行相对链接检查；验证：`docs/`、`README.md`、`ROADMAP.md` 的坏链数为 0。
- [x] 6.2 确认活跃文件中无文档路径残留，且 archive 未被改写；验证：`grep -rnE 'doc/([0-9]{2}|spike)' README.md ROADMAP.md CLAUDE.md docs docker scripts iboot-*/src` 无输出（MinIO 键 `kb/.../doc/...` 不是文档引用），`git diff --stat -- openspec/changes/archive` 为空。
- [x] 6.3 确认脚本语法与 Compose 配置有效；验证：`bash -n` 覆盖 `scripts/*.sh`，`docker compose -f docker/docker-compose.yml config -q` 退出码为 0。
- [x] 6.4 复核删除项与变更清单；验证：`git status --short` 中每个删除项都在 proposal 范围内，`openspec validate cleanup-project-docs-and-scripts` 通过。
