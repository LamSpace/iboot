# Proposal

## Why

底座（`add-microservice-foundation`）与共享内核（`add-shared-kernel`）就绪后，阶段 2 需要拆出第一个业务服务。选 `iboot-file` 不是因为它重要，而是因为它最干净：`/api/file` 只有 7 个接口、只碰 `sys_file` 一张表、`FileMapper.xml` 里没有任何 `sys_user` JOIN，是唯一不会触发蓝图 D9 那两处跨域 JOIN 的上下文。

这一步真正的产出不是「多一个服务」，而是把拆分方法固化成一条可复制的流程，并用它第一次验证 D2「路由表即回滚开关」是否真能回滚——**包括回滚后数据仍然对得上**。

## What Changes

- 新增 Maven 模块 `iboot-file`（端口 `8082`，`spring.application.name=iboot-file`），承载 `/api/file/**` 的 7 个接口。
- 迁入文件域的完整链路：`FileController`、`FileApplicationService`、`FileInfo` / `StorageObject` / `FileRepository` / `FileStorageStrategy`、仓储与 Mapper、本地 / MinIO / OSS 三种存储实现及其配置。
- **过渡期共用表**：`iboot-file` 与管理端读写同一个 `sys_file`，Flyway 以 `baseline` 方式不接管该表 DDL。独立 schema 留到删除旧实现的那一刀。
- **过渡期只读共享 `sys_config`**：`sys.file.maxSize` 与 `sys.file.allowedExtensions` 继续从共享库读取，不改成 Nacos，避免管理端配置页对这两项失效。
- 鉴权与操作日志复用 `iboot-kernel`，不复制。
- 网关新增 `/api/file` → `lb://iboot-file` 路由，`scripts/regress/routes.txt` 同步。
- 注册进 Nacos，接入链路追踪（与既有服务同一套配置）。
- 回归工具扩展：契约快照支持多服务、新增「路径守恒」检查与「等价性回放」、新增 file 的故障演练与回滚演练。
- 管理端**保留** `/api/file` 的旧实现一个发布周期（蓝图 D2），**不删除任何代码**。

明确不做（非目标）：

- 不给 file 建独立数据库 schema、不迁表数据——这属于下一刀 `remove-file-from-admin`。
- 不删 admin 的 `FileController` / `FileApplicationService`。
- 不改前端任何代码（接口路径不变，走网关）。
- 不引入 RocketMQ、Seata，不把操作日志改成事件投递。
- 不抽 `FileStorageStrategy` 到内核：备份功能（未来归 `iboot-job`）也用它，但这一刀两边各留一份，共享问题留到 job 拆分时解决。

## Capabilities

### New Capabilities

（无。拆分本身不改变任何外部可见行为。）

### Modified Capabilities

- `api-contract-regression`: 新增「服务迁移的路径守恒与响应等价」要求——拆分后接口在服务之间的归属变化必须可被自动证明为纯搬家，而不只是「冒烟通过」。

## Impact

**构建**

- 根 `pom.xml` 新增 `iboot-file` 模块；模块依赖 `iboot-kernel` 与既有存储 SDK。
- `iboot-admin` 本变更**不减依赖**（旧实现保留），删除动作在 `remove-file-from-admin`。

**运行时**

- 新增第 4 个 JVM（8082），本机内存再增 512MB 量级（R4）。
- 新增 Nacos 实例注册与网关路由。
- 数据库连接指向现有实例；`sys_file` 与管理端共用。

**数据**

- 无数据迁移。过渡期两个实现读写同一张表，这是回滚可用的前提。

**测试与质量**

- 契约快照从 `admin` / `ai` 两份扩展为三份，`regress.sh` 按服务循环。
- 新增三项检查：路径守恒、等价性回放、故障与回滚演练。
- 管理端现有测试中涉及文件域的部分，随代码迁入 `iboot-file`；其余测试文件不改。

**风险**

- 过渡期存在两份同路径实现，路由写错会把流量打到错误实现——由路由覆盖检查与 `routes.txt` 单一事实来源兜住。
- 管理端与 file 共用 `sys_file` 与 `sys_config`，是**有意违反**「每服务独立 schema」的临时状态，必须写明退出条件与检查方式，否则会被当成常态。

**文档**

- `doc/23` 增加「新增服务接入清单」与 file 服务的专用说明。
- `ROADMAP.md` §4.2 更新拆分状态表；`doc/03-架构设计.md` 更新服务拓扑。

**实现中补充的内容（拆分时暴露的隐式契约）**

- **数据权限**：`FileMapper` 的查询带 `@DataScope`，且旧实现在 `sys_file` 上是坏的（表无 `dept_id`）。文件服务改为调 `/api/auth/scope` 获取范围、在 `LEFT JOIN sys_user` 的别名上过滤，并让统计查询同样受控；权限矩阵新增三条用例。
- **内核补充**：新增 `KernelRedisAutoConfiguration`，提供与管理端序列化一致的 `redisTemplate`。否则新服务「令牌有效但一直 401」——这是拆分时实际踩到的坑。
- **文件服务内置**：CloudEvent 响应信封、`ApiVersionConfig`、`I18nConfig` 与消息文件（后三项为复制，后续评估收敛进内核）。
- **本地环境**：`scripts/dev-init.sh` 的服务清单加入 `iboot-file:8082`。
