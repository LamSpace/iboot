# Design

## Context

见 `proposal.md - Why`。影响实现方式的现状事实：

- 文件域共 7 个接口（`/api/file/upload|list|{id}|download-url/{id}|download/{id}|export` + `PUT /api/file`），权限码为 `file:upload|list|download|remove|export`。
- `FileApplicationService` 只有三个依赖：`FileRepository`、`ConfigApplicationService`（读 `sys.file.maxSize` / `sys.file.allowedExtensions`）、`FileStorageStrategy`。
- `FileStorageStrategy` 有 Local / MinIO / OSS 三个实现，**同时被 `BackupApplicationService` 与 `RestoreDrillService` 使用**。
- `sys_file` 由 `V1__initial_schema.sql` 创建，没有任何外键，`FileMapper.xml` 不含 `sys_user` JOIN。
- 网关全局响应超时为 10s，长连接路由单独设为 `-1`。
- 回归脚本目前把 `admin` / `ai` 两份快照写死为两个循环项。

## Goals / Non-Goals

**Goals**

- 用最小的业务域跑通「模块化 → 路由切换 → 回归证明 → 可回滚」的完整流程，形成可复制的拆分清单。
- 拆分前后 `/api/file/**` 的外部行为完全一致。

**Non-Goals**

- 不做数据迁移，不建独立 schema。
- 不删除管理端旧实现。
- 不为备份功能改造存储 SPI。
- 不引入新的中间件。

## Decisions

### D1. 服务身份：`iboot-file` / `8082` / `/api/file`

端口按既有顺序递增（8080 admin、8081 ai、8088 gateway），服务名与 `spring.application.name` 一致，`routes.txt` 增加 `/api/file iboot-file`。

**为什么**：路径前缀与限界上下文一一对应，路由表才能持续作为「唯一事实来源」。

### D2. 过渡期共用 `sys_file` 与管理端同一 schema

`iboot-file` 的 datasource 指向现有数据库，直接读写 `sys_file`；本变更**不启用 Flyway**，不接管任何 DDL。

**为什么**：D2（绞杀者）要求「改回路由即可回滚」。若 file 写到新 schema 而 admin 旧实现读旧表，回滚后看不到拆分后写入的数据——回滚就是假的。共用一张表是让回滚成立的前提。

**退出条件**：`remove-file-from-admin` 删除旧实现时，把 `sys_file` 迁到 `iboot_file` schema 并启用 Flyway；在此之前「每服务独立 schema」这条原则在本服务上**有意不成立**，必须在 `doc/23` 登记。

**备选**：本变更直接建 `iboot_file` schema 并把 admin 的旧实现也指过去——可行，但会把一次路由切换变成一次数据迁移，回滚面从「改一行」扩大到「改一行 + 迁回去」。

### D3. `sys_config` 只读读取，不改 Nacos

文件的两个约束项继续从共享库的 `sys_config` 读取，`iboot-file` 内置一个只读查询。

**为什么**：改成 Nacos 会让管理端「参数配置」页对这两项失效，属于用户可见的行为变化，与「等价」目标冲突。

**退出条件**：`iboot-system` 拆分时改为调用 system 服务的配置查询接口。届时管理端读本地、file 读 system，仍然同源。

### D4. 存储实现随域迁移，admin 保留副本

三个 `FileStorageStrategy` 实现与 `FileStorageConfig` 迁入 `iboot-file`；管理端因保留旧实现而保留一份。两边指向同一个 bucket / 本地目录，配置项同名同值。

**为什么**：把 SPI 提到内核意味着内核拖入 MinIO 与 OSS 两个 SDK，而目前只有 file 与备份需要它。共享问题在 `iboot-job` 拆分时一并解决（那时才出现第三个消费者之外的第二个真实使用方）。

**约束**：迁移后必须验证两边写出的对象路径规则一致，否则回滚期间上传的文件在新服务里可能找不到。

### D5. 鉴权与操作日志复用内核

`iboot-file` 只声明依赖与白名单，不实现过滤器、不实现日志切面。白名单只有 Swagger、`/actuator/**`、`/error`（文件接口全部需要登录）。

### D6. 回归工具扩展三项

1. **多服务快照**：`api-snapshot.sh` 与 `regress.sh` 的服务清单从写死改为读取配置，`golden/` 增加 `file.json`。
2. **路径守恒**：新增 `path-conservation.sh`，比较「本次导出并集」与「基线并集」，差异必须与 `migrations.txt` 中登记的迁移逐条对应。
3. **等价性回放**：新增 `equivalence.sh`，对一组**只读**请求（`list` / `{id}` / `download-url/{id}` / `export`）录下状态码与归一化响应体，迁移后经网关重放并 diff。写操作（上传 / 删除）由冒烟覆盖，因为它们的响应含自增 ID 与时间戳，不适合逐字节比对。

**为什么**：冒烟只断言关键字段，证明不了「搬家没搬错」；等价性是拆分唯一直接的证据。

### D7. 路由切换与故障演练

切换后执行：停 `iboot-file` → `/api/file/**` 返回 503、其余前缀正常；重启 → 恢复，网关不需要重启。

**为什么**：验收判据 2.6 第 2 条的实质是「拆出来的东西真的是独立故障单元」。

### D8. 数据权限：走 scope 接口，并修正文件域的过滤语义

`FileMapper` 的三个查询带 `@DataScope`，实现时发现两层问题：

- 旧实现把过滤条件落在文件表别名 `f` 上，而 `sys_file` 没有 `dept_id` 列：DEPT 范围直接 SQL 报错，SELF 范围按文件主键过滤。只有超管（ALL）能正常使用；权限矩阵此前没覆盖文件列表，所以一直没暴露。
- 数据范围的计算依赖 `sys_user` / `sys_role` / `sys_dept`，而登录会话里没有 `dataScope` 与 `visibleDeptIds`，新服务无法只靠会话算出范围。

**决策**：按 D10 的方向，文件服务带调用者令牌调管理端 `/api/auth/scope`，本地缓存 60 秒，失败关闭（拿不到范围就拒绝查询，绝不放行未过滤的 SQL）。过滤落在 `LEFT JOIN sys_user u ON u.username = f.upload_by` 的别名 `u` 上：DEPT 用 `u.dept_id`、SELF 用 `u.id`、自定义/部门及以下用 `u.dept_id IN (...)`。统计查询同样加注解，保证分页总数与列表一致。

**备选**：把数据权限链路移入内核（需要内核携带 system 域 SQL）；或把 admin 的解析器与三个 Mapper 复制进文件服务（每个服务再复制一遍）。两者都与「内核不含数据权限」或「不复制 system 数据访问」冲突。

**代价**：文件列表对受限用户从「报错」变为「正确过滤」，这是有意的行为修正，已在权限矩阵新增三条用例钉住。

### D9. 四个「不报错但会静默出错」的共享契约

拆分时逐个踩到，均已处理并写入 `doc/23 §11.2`：

| 契约 | 漏掉的后果 | 归属 |
|---|---|---|
| 会话 Redis 序列化 | 令牌有效但一直 401 | 新增 `KernelRedisAutoConfiguration`，提供与管理端一致的模板 |
| CloudEvent 响应信封 | 前端与回归脚本取不到 `data.data` | 文件服务内置 `CloudEventResponseBodyAdvice`（`basePackages` 指向自身） |
| API 版本策略 | 启动失败：`no ApiVersionStrategy configured` | 复制 `ApiVersionConfig` |
| 国际化消息源 | 响应 `message` 变成消息码 | 复制 `I18nConfig` 与 `i18n/messages*` |

后三项目前是「复制」，后续应评估收敛进内核；第一项已经收敛。

## Risks / Trade-offs

| 风险 | 处置 |
|---|---|
| 过渡期两份同路径实现，路由写错打到旧实现 | 路由覆盖检查作为 CI 门禁；`routes.txt` 与网关配置逐行对照 |
| 共用 schema 被当成长期方案 | 在 `doc/23` 与 `ROADMAP.md` 写明退出条件与负责变更 |
| 大文件下载经网关受全局 10s 响应超时约束 | 现状已如此，非本次引入；等价性用例用小文件，压测若发现超时再为 `/api/file/download/**` 单独放宽超时 |
| 两边存储实现的对象路径规则不一致，回滚期间文件失联 | 迁移后做一次「旧实现上传 → 新服务下载」与反向的交叉验证 |
| 管理端旧实现与 file 同时运行导致定时/缓存行为重复 | file 不含定时任务；管理端文件域无缓存，风险为空 |
| 本机内存再增一个 JVM | 按 R4，本变更期间只启动 admin / file / gateway，ai 可停 |

## Migration Plan

1. 建立 `iboot-file` 模块并迁入文件域，**先不切路由**；此时管理端行为不变。
2. 网关增加路由并把 `/api/file` 指向新服务；管理端旧实现仍在，回滚即改回这一行。
3. 全量回归：快照、路径守恒、等价性、冒烟、权限矩阵、追踪。
4. 故障演练与回滚演练（回退路由 → 上传 → 切回 → 能查到）。
5. 文档与 `routes.txt` 同步提交。

回滚：把网关路由的 `uri` 改回 `lb://iboot-admin`（或 `local` profile 下的静态地址），无需改代码、无需回迁数据。

## Open Questions

- 管理端旧实现保留多久（一个发布周期）没有客观判据，建议以「至少一次完整回归 + 一次真实使用」为准，在 `remove-file-from-admin` 里再定，不影响本变更的任务拆分。
