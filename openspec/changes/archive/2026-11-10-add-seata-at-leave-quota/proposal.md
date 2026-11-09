# Proposal

## Why

阶段 2 的五项验收判据中，只剩「跨服务强一致写具备可回滚的分布式事务」未达标。当前所有跨服务写入都是旁路（通知、审计、操作日志），失败不影响主流程；继续用事件与重试就能覆盖，没有引入 Seata 的真实理由。

本次变更同时补上一个真实的业务约束：请假审批需要按人按年扣减次数额度。审批结论（`iboot-workflow`）与额度（`iboot-system`）分属两个服务、两张表，任何一侧单独提交都会留下「批了但没扣」或「扣了但没批」的脏状态。把这一对写放进同一个可回滚的分布式事务，既满足阶段 2 判据，也让额度成为主线上真实生效的规则，而不是演示端点。

## What Changes

- 新增次数制请假额度：按 `user_id + year` 记录总额度、已用与冻结数量；默认额度由 `sys_config` 提供，首次使用时懒创建。
- 新增系统服务内部扣减接口 `/internal/leave/quota/deduct`：共享密钥鉴权、按申请 ID 幂等、额度不足时失败关闭。
- 审批通过改为 Seata AT 全局事务：审批服务的流程与申请状态、系统服务的额度扣减，任一步失败整体回滚。
- 新增只读额度接口，并在请假表单展示「剩余 x/10」；不做管理员按人调额度。
- 引入并常驻 `seata-server`：配置 XID 传播、`undo_log`、启动顺序、健康检查与失败语义。
- 明确不做的事：不改通知 outbox 与 RocketMQ 链路；不引入 TCC（放到下一个变更做独立实验）；不改变 `/api/approval/**` 的既有契约。
- 版本锁定 Seata **2.5.0**（随 SCA 2025.1.0.0 BOM，groupId 为 `org.apache.seata`）。

## Capabilities

### New Capabilities

- `leave-quota`: 次数制请假额度的默认值、懒创建、只读查询、原子扣减与幂等语义。
- `distributed-transaction`: Seata AT 参与方接入、XID 跨服务传播、`undo_log` 生命周期、TC 不可用时的失败语义与可观测性。

### Modified Capabilities

- `approval-workflow`: 「审批通过」从流程状态与业务记录的本地事务，扩展为流程状态、业务记录与额度扣减的跨服务原子提交；额度不足或下游不可用时审批 MUST NOT 落库。

## Impact

- 服务：`iboot-workflow`（TM）、`iboot-system`（RM）；其余服务不参与。
- 数据库：共用库 `iboot` 新增 `undo_log`、`sys_user_leave_quota`、`sys_leave_quota_log`；结构仍只由 `iboot-db-init` 迁移。
- 基础设施：`docker/docker-compose.yml` 常驻 `seata-server`；`scripts/dev-init.sh` 增加启动顺序与健康等待；`.env.example` 增加占位。
- 前端：`iboot-portal` 请假表单增加额度展示与额度不足提示。
- 依赖：`spring-cloud-starter-alibaba-seata`、`apache/seata-server:2.5.0`（镜像已在 `scripts/pull-docker-images.sh` 中）。
- 运维：审批路径新增对 `seata-server` 的强依赖，TC 不可用时审批 fail-closed；新增容器与约 512MB 常驻内存。
- 文档：`ROADMAP.md`、`docs/23`、`docs/02`、`README.md`，以及 Seata 兼容性 spike 报告。
