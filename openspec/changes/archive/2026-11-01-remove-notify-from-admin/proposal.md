# Proposal

## Why

`add-notify-service` 已交付并合并：消息中心、公告、模板、通知投递、SSE 与内容检索的外部流量已全部经网关切到 `iboot-notify`。管理端仍保留整套旧实现（88 个 Java 文件、约 1.9 万行，另有 6 个 mapper XML 与 19 个测试类），并由管理端 Quartz 通过两行 `sys_job` 驱动通知重试与索引对账——这是绞杀者过渡期的临时状态：

- 同一份业务存在两套代码、两套测试、两套配置，任何相关改动都要改两处，且极易漏改；
- 六张消息/公告/投递表仍在 `iboot_admin` schema，与「每服务独立 schema」的路线图目标冲突，也让 `iboot-system` 拆分时无法按域划清数据边界；
- 过渡期越长，删除成本越高（旧实现会继续被无意修改）。

现在流量已稳定、回归装置齐备，是收敛这一临时状态的时机。

## What Changes

- **数据独立**：`sys_message`、`sys_message_receiver`、`sys_message_template`、`sys_notice`、`sys_notice_read`、`sys_notification_delivery` 迁到 `iboot_notify` schema；`iboot-notify` 启用 Flyway 并接管 DDL，数据源切库。
- **维护任务接管**：`notificationRetryTask.retry` 与 `contentIndexReconcileTask.reconcile` 迁入 `iboot-notify`，改用 `@Scheduled` + Redis 分布式锁；删除 `sys_job` 中对应的两行。
- **删除管理端旧实现**：88 个 Java 文件、6 个 mapper XML、19 个测试类，以及 `application.yml` 中仅服务于旧实现的渠道/推送配置块。
- **保留跨服务契约件**：`NotificationPort`、`NotificationCommand`、`HttpNotificationPort`、`NotificationEventType` 及其测试不属于旧实现，继续留在管理端。
- **回归登记收敛**：`migrations.txt` 五条从 `transitional` 改为 `done`，刷新 `golden/admin.json`。
- **开发环境与文档**：`dev-init.sh` 增加 `iboot_notify` 建库；更新 `ROADMAP`、`doc/23` §11.7 与相关文档中「旧实现保留」的描述。

**对外契约不变**：`/api/**` 路径、方法、响应结构、权限码与通知投递语义均无变化，前端零改动。

**一处刻意的用户可见变化**：任务管理页不再出现「通知投递重试」「内容索引对账」两条内部维护任务——它们转为通知服务的进程内调度。这不是功能缺失，需要在文档中说明。

**BREAKING（仅内部）**：回滚方式从「改回网关路由」变为 `git revert`。项目尚未对外发布，不存在需要保护的在线旧版本，因此不执行「观察一个发布周期」的收敛。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。本变更是部署归属与数据归属的收敛，不改变任何能力的外部行为；`notification`、`content-search` 与 `api-contract-regression` 的既有需求继续成立（路径守恒与契约基线由回归装置强制）。

## Impact

**代码**

- 删除：`iboot-admin` 中消息/公告/模板/投递的接口、应用、领域与基础设施实现，`infrastructure/push`、`infrastructure/search`、`infrastructure/notification`（除 `HttpNotificationPort`）。
- 保留：`NotificationPort`、`NotificationCommand`、`HttpNotificationPort`、`NotificationEventType`、`HttpNotificationPortTest`。
- `iboot-notify` 新增 `V1__initial_schema.sql` 与两个维护任务。

**数据**

- 六张表搬迁（一次性运维脚本，复制不删除，源表保留一个周期）。
- `iboot_admin.sys_job` 删除两行内部任务。

**依赖与配置**

- `iboot-notify` 启用 Flyway，数据源改为 `iboot_notify`；跨 schema 只读 `iboot_admin.sys_user` / `sys_config` / `sys_dict_data` 的限定名保持不变。
- 管理端移除 `iboot.push.*` 与 `iboot.notification.*` 中的渠道配置，仅保留 `base-url` 与 `internal-token`。

**回归资产**

- `migrations.txt` 五条改 `done`；`golden/admin.json` 刷新；冒烟与权限矩阵不受影响（全部经网关）。
