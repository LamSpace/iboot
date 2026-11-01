# Proposal

## Why

阶段 2 的拆分顺序原为 `file -> notify -> job -> monitor -> workflow -> system`。按「依赖少的先拆」的实际度量，`workflow` 应排在 `monitor` 之前：

- `workflow` 独占 33 张表（30 张 `ACT_*`、2 张 `FLW_*`、`biz_leave_application`），没有任何其它服务写入；外部只依赖 `sys_user` 的一次只读，以及 notify 的内部通知端点（已完成）。
- `monitor` 是**读侧扇入**：登录日志由 auth 写、操作日志由内核写、在线会话由所有服务写，再叠加系统域的 dashboard/statistics 聚合。先拆 monitor，等于把多条跨服务写路径压到一个尚未定义的边界上。

`workflow` 自身也符合蓝图给出的理由：Flowable 是管理端最重的依赖（引擎 + 1100 行 DDL），独立部署边界最清晰。拆出后 admin 不再启动流程引擎，审批故障不再与管理功能共命运。本变更前先由 `fix-operate-log-write-path` 修好审计日志洞，避免新服务的 3 个 `@Log` 端点从上线第一天就丢日志。

## What Changes

- **新建 `iboot-workflow` 服务（端口 8086）**，承载 `/api/approval/**` 的 7 个接口：发起、待办、已办、我的申请、详情、通过、驳回。
- **独立 schema `iboot_workflow`**：33 张表由本服务 Flyway V1 管理。DDL 从 admin V17 复制，**不复制菜单与角色种子**（菜单属于系统域，仍留在 admin）。
- **停机窗口数据搬迁**：停 admin → 复制 33 张表（含 `ACT_GE_PROPERTY` 的 schema 版本、运行时表与历史表）→ 校验逐表行数与 `max(id)` → 启动 workflow。在途流程实例必须能在新服务继续审批；重启不得重复部署 BPMN。
- **代码搬迁**：`ApprovalController`、`ApprovalApplicationService`/`ApprovalDetail`/`ApprovalItem`、`domain/workflow`、`infrastructure/workflow`（`FlowableWorkflowAdapter`）、`LeaveApplicationMapper` 与 XML、`processes/leave-approval.bpmn20.xml`、通知端口与 5 个 DTO。
- **用户读取改为跨 schema 只读**：`iboot_admin.sys_user` 的 `id/status/username/nickname`，行为与当前一致；退出条件：`iboot-system` 拆分后改调系统服务。
- **通知调用**：沿用 `POST /internal/notifications`（`X-Internal-Token`）；新服务按 Nacos 服务名寻址 notify，避免继续引入静态地址债务。
- **网关新增路由**：`/api/approval` → `lb://iboot-workflow`，order 26，排在 admin 兜底（order 100）之前。
- **回归资产**：`golden/workflow.json`、`routes.txt`、`migrations.txt`（过渡期 `transitional`）、只读等价性、冒烟补通过/驳回/详情、权限矩阵补审批三组、`trace-chain` 增加 `gateway -> workflow`、`dev-init.sh` 增加服务与 schema。
- **管理端旧实现保留**（绞杀者过渡期）：改回网关路由即可回滚；删除旧实现由 `remove-workflow-from-admin` 承担。
- **对外契约不变**：路径、方法、响应结构与权限码保持原样，前端零改动。无 **BREAKING**。

不在本变更范围内（显式记录，避免被当成遗漏）：

- 审批超时升级、催办、多级会签、BPMN 设计器。
- 把 `sys_user` 读取改成系统服务 API（等 `iboot-system`）。
- `monitor` / `system` 的拆分，以及 dashboard/statistics 的归属调整。
- 操作日志的长期方案（由 `fix-operate-log-write-path` 先止血）。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `approval-workflow`: 新增「审批服务的独立启停与数据归属」需求——审批能力由独立服务提供、审批不可用不拖累其它管理功能、审批表只由本服务写、对外契约不变；并在「发起请假申请」下补充「用户目录不可用时失败关闭」的场景。

## Impact

**代码**

- 新增模块 `iboot-workflow`：application / domain / infrastructure / interfaces 四层，Flowable 配置（`database-schema-update=false`、`check-process-definitions=true`、异步执行器关闭）。
- `iboot-admin` 保留旧实现，本变更不删文件；`remove-workflow-from-admin` 负责删除。

**接口**

- 对外路径零变化，前端零改动。
- 内部新增：`iboot-workflow -> iboot-notify`（通知登记，失败不阻断审批）。

**依赖与配置**

- 新增第 8 个 JVM（本机内存按 512MB–1GB 估，受 R4 约束；本地按需启动）。
- 新增 Flowable、MyBatis-Plus、MySQL、Redis 会话、Nacos 注册与 OTLP 追踪。
- 四个隐式契约（Redis 序列化、CloudEvent 信封、API 版本策略、`jwt.secret` 逐字一致）必须逐项核对。
- `IBOOT_NOTIFY_INTERNAL_TOKEN` 必须与 notify 配置一致；未配置时审批仍可用，但通知失败关闭。

**数据**

- 新 schema `iboot_workflow`，33 张表；`iboot_admin` 中的同名表保留一个发布周期供回退对照。
- 搬迁脚本放 `scripts/migrate/`，包含逐表校验 SQL。

**回归资产**

- 契约基线、路径守恒、路由覆盖、冒烟、权限矩阵、只读等价性、链路追踪各登记一处；`regress.sh` 增加 `REG_WORKFLOW_BASE`。

**已知遗留**

- 跨库读 `sys_user` 与跨服务写审计表都是临时约定，退出条件写在 `doc/23` §11.9；不随本变更解决。
- 管理端在删除旧实现前仍会启动 Flowable；内存收益要到 `remove-workflow-from-admin` 才兑现。
