# Tasks

## 1. 模块与包改名

- [x] 1.1 用 `git mv iboot-admin iboot-system` 移动模块目录，更新根 `pom.xml` 的 `<modules>` 与模块自身 `pom.xml` 的 `artifactId` / `name`。验证：`mvn -s /home/lam/repo/settings.xml -q -pl iboot-system -am help:evaluate -Dexpression=project.artifactId` 输出 `iboot-system`。
- [x] 1.2 包名 `com.iboot.admin` → `com.iboot.system`，同步目录、`package` 声明与全部 `import`。验证：`grep -rn "com\.iboot\.admin" --include=*.java .` 无输出。
- [x] 1.3 同步包名相关的配置与元数据：`spring.application.name`、`mybatis.type-aliases-package`、`mapper-locations`、`@MapperScan`、日志级别与 `logback` 的 APP_NAME、10 个 Mapper XML 的 `namespace` 与 `resultType`、指标 `application` 标签、Nacos 配置 data-ID、模块 Dockerfile。验证：`grep -rn "com\.iboot\.admin" --include=*.xml --include=*.yml iboot-system` 无输出。
- [x] 1.4 全量构建与单测。验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-system -am verify` 构建成功，`Tests run: 62, Failures: 0, Errors: 0`；surefire 报告 14 个可执行测试类（抽象基类 1 个、测试辅助类 1 个、`@Tag("cloud")` 按环境变量门控的 OSS 契约测试 1 个不计）。

## 2. 运行时身份与寻址

- [x] 2.1 网关路由改名：`application-local.yml` 与 `application-nacos.yml` 中 `id: admin` → `id: system`，`IBOOT_ADMIN_URI` → `IBOOT_SYSTEM_URI`，`lb://iboot-admin` → `lb://iboot-system`；`order: 100` 与 `Path=/api/**` 保持不变。验证：启动后经网关 `GET /api/user/profile` 返回 200，且 `route-coverage.sh` 通过。
- [x] 2.2 依赖方默认值改寻址：`iboot-job`、`iboot-monitor` 的 `scope-url` 默认值与 `iboot-ai` 的 `admin-base-url` 改为 `iboot-system`；`iboot-file`、`iboot-ai` 中按端口寻址的 `http://localhost:8080/api/auth/scope` 保持不动。验证：`grep -rn "http://iboot-admin\|lb://iboot-admin" --include=*.java --include=*.yml iboot-*` 无输出。
- [x] 2.3 修正 `iboot-monitor` 的 `PermissionScopeClient` 配置键前缀：`iboot.job.permission.scope-url` / `cache-seconds` → `iboot.monitor.permission.*`。验证：新增或修改 `PermissionScopeClientTest`，断言绑定 `iboot.monitor.permission.scope-url` 后客户端使用该值，测试通过。
- [x] 2.4 更新脚本中的服务名：`scripts/dev-init.sh`（`SERVICES` 与 jar 名判断）、`scripts/build.sh`、`docker/deploy.sh`、`scripts/regress/lib.sh`。验证：`bash scripts/dev-init.sh` 拉起九个服务，逐个 `/actuator/health` 返回 UP。

## 3. 回归装置

- [x] 3.1 `scripts/regress/golden/admin.json` → `golden/system.json`，内容不变。验证：`git show HEAD:scripts/regress/golden/admin.json` 与新文件规范化后逐条比对，57 条路径完全相同。
- [x] 3.2 `scripts/regress/routes.txt` 中 `/api` 的目标服务改为 `iboot-system`，并更新文件头注释中的服务名示例。验证：`path_conservation.py` 通过，输出中不出现未解释的路径增减。
- [x] 3.3 核对等价性装置：`equivalence.py` 的采集器只覆盖 file/notify/job/workflow/monitor，**从未采集过 admin**，因此没有需要改名的函数或基线。验证：`grep -rn "admin" scripts/regress/equivalence.py scripts/regress/equivalence.sh` 无服务名命中；`equivalence.sh --verify` 可正常执行（既有基线与当前数据集绑定，环境重建后数值会漂移，不作为本次判据）。
- [x] 3.4 `scripts/regress/smoke.sh` 与 `permission-matrix.sh` 中的服务名、日志文件名、直连基址注释同步。验证：两个脚本各自跑通，退出码为 0。
- [x] 3.5 清理残留快照。验证：`ls scripts/regress/golden` 中不再有 `admin*.json`，且 `route-coverage.sh` 不把已删除的旧服务当成缺失。

## 4. 契约登记与文档

- [x] 4.1 `doc/23` 新增 §11.11「iboot-system 的服务状态」，含端口、承载路径、schema 现状（历史名称）、装配图与退出条件；同步 §7 路由表、§11.5 认证服务的 schema 行。验证：§7 表格中的 system 行与 `application-*.yml` 的路由定义逐字一致。
- [x] 4.2 `doc/23` 新增「跨 schema 只读契约」清单，逐服务登记表名、列名、用途与失败语义；同步改写 §11.3 / §11.7 / §11.8 / §11.9 / §11.10 中「退出条件为 iboot-system 拆分」的表述为「接受为长期模式」。验证：清单每一行都能在对应服务的 reader 源码中定位到同名表列；`grep -c "退出条件.*iboot-system" doc/23*` 的结果为 0。
- [x] 4.3 `doc/07` 补充 schema 归属与「`iboot_admin` 为历史名称」的说明；`doc/11` §11.5.1 的服务清单同步为 `iboot-system`。验证：两份文档中的服务名与 `scripts/dev-init.sh` 的 `SERVICES` 列表一致。
- [x] 4.4 `ROADMAP.md` §4.2 状态表将 `system` 改为已完成并记录变更名；§1.1 模块列表与 §1.2 的能力归属同步；`README.md` 的目录树、服务表与启动命令同步。验证：`grep -rn "iboot-admin" README.md ROADMAP.md` 的命中仅为 schema 名或历史叙述。
- [x] 4.5 全仓残留审计。验证：`grep -rn "iboot-admin\|iboot_admin" --include=*.java --include=*.yml --include=*.xml --include=*.sh --include=*.md . | grep -v openspec/changes/archive` 的每一条命中都能归入以下三类之一：`jwt.secret` 密钥字面量、schema 名 `iboot_admin`、本变更的文档叙述。

## 5. 端到端验收

- [x] 5.1 契约与回归全量通过。验证：`bash scripts/regress/regress.sh`、`smoke.sh`、`permission-matrix.sh`、`equivalence.sh verify` 依次退出码为 0，契约基线为 57 条。
- [x] 5.2 链路追踪。验证：`trace-chain.sh <traceId> --expect iboot-gateway,iboot-auth,iboot-system` 通过，一次登录产生贯穿的 TraceID。
- [x] 5.3 故障隔离演练：停 `iboot-system` 后逐个采样依赖方。验证：已登录会话访问普通业务接口仍 200；job/monitor 在缓存 60 秒内仍可用、过期后返回明确 5xx；auth 登录、notify 与 workflow 的用户读取返回可辨识错误；网关快速失败，无重试风暴；重启后各服务无需重启即自愈。演练前确认注册中心列表只剩 `iboot-system`，避免旧实例造成假象。
- [x] 5.4 回滚演练。验证：`git revert` 后重新构建并启动全部服务，回归全绿；确认过程中不需要任何数据补偿动作。
