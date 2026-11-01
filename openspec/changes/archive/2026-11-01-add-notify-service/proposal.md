# Proposal

## Why

阶段 2 的拆分顺序里，`iboot-notify` 排在 `iboot-file` 之后。选它不是因为"依赖少"，而是因为它是整条拆分线上**第一个同时具备两种新形态的限界上下文**：有状态长连接（SSE）与被其他服务触发的入站事件（审批待办通知）。`iboot-file` 验证了「无状态 HTTP 服务怎么拆」，`iboot-auth` 验证了「协议级服务怎么拆」，而「别的服务怎么触发它」这个问题在后续 `job`、`monitor`、`workflow` 上会以同样形态反复出现，必须先在 notify 上解决一次。

当前消息中心、公告、多渠道通知与内容检索的写入全部留在 `iboot-admin`，与审批流处于同一进程：一次通知模板配置错误会连带审批提交失败，通知服务不可用与业务主流程不可用是同义词。故障半径无法收敛，这正是微服务拆分要解决的问题。

## What Changes

- **新建 `iboot-notify` 服务（端口 8084）**，承载四个能力域：
  - 站内消息与消息模板（`/api/message/**`，含模板管理）
  - 公告（`/api/notice/**`）
  - 通知投递编排与多渠道 SPI（`/api/notification/delivery/**`）
  - SSE 实时推送（`/api/push/**`）与内容检索（`/api/search`，含 `iboot-content-v1` 索引写入）
- **审批触发通知改为跨服务 HTTP**：`ApprovalApplicationService` 只依赖一个 `NotificationPort` 端口，由 HTTP 适配器调用 notify 的内部端点 `POST /internal/notifications`（不经网关）。触发在业务事务提交后执行，失败按退避重试并记录指标，**不阻断审批提交**。
- **网关新增路由**：`/api/push/**`（继承 `response-timeout: -1` 长连接参数）、`/api/message/**`、`/api/notice/**`、`/api/notification/**`、`/api/search` 指向 `iboot-notify`，均排在 admin 兜底之前。
- **过渡期共享现有表**：`sys_message`、`sys_message_receiver`、`sys_message_template`、`sys_notice`、`sys_notice_read`、`sys_notification_delivery` 继续留在 `iboot_admin` schema，notify 直接读写；`iboot-admin` 旧实现保留，改回路由即可回滚。
- **对外契约不变**：`/api/**` 路径、方法、响应结构与权限码全部保持原样，前端零改动。本变更不含 **BREAKING** 变更。

## Capabilities

### New Capabilities

无。本变更不引入用户可见的新能力，只改变能力的部署归属与触发方式。

### Modified Capabilities

- `notification`: 新增「通知触发与业务主流程解耦」约束。原规格的登记语义是同步的——模板不存在或停用时 `enqueue` 抛出业务异常，而它运行在审批提交的同一事务里，等于审批会因通知配置问题失败。拆分后审批提交 MUST NOT 因通知登记失败而失败，失败 MUST 可观测并可按退避重试。

## Impact

**代码**

- 新增模块 `iboot-notify`：消息 / 公告 / 模板 / 投递 4 组 Controller、`PushEndpoint`、`infrastructure/search` 全部内容，以及通知编排与三渠道 SPI。
- `iboot-admin` 保留旧实现（绞杀者过渡期），仅把审批的触发方式改为端口调用；第二个变更 `remove-notify-from-admin` 负责删除旧实现与迁移数据。
- 18 个既有测试类（13 个通知/消息 + 5 个内容检索）随代码迁移。

**接口**

- 对外路径零变化。
- 新增内部端点 `POST /internal/notifications`，不经网关暴露，仅服务间调用。

**依赖**

- 新增 1 个 JVM（本机内存约束 R4 再 +512MB）。
- Redis（SSE 跨实例广播、分布式锁）、Elasticsearch（`iboot-content-v1`）、SMTP / 钉钉 / 阿里云短信配置随服务迁移；Nacos 注册与 OTLP 追踪按 `doc/23` §11.1 接入。

**数据**

- 过渡期共享 6 张表；独立 schema `iboot_notify` 与 Flyway 在第二个变更中启用。

**回归资产**

- `scripts/regress/golden/notify.json`、`routes.txt`、`migrations.txt` 五条登记、`equivalence` 扩展到 notify、权限矩阵两条、`trace-chain` 增加 `gateway → notify`。
