# Tasks

## 1. 数据搬迁与独立 schema

- [x] 1.1 `iboot-notify` 启用 Flyway，编写 `V1__initial_schema.sql`：六张表结构与现网一致（含 `V16` 给 `sys_message_receiver` 加的列、`V20` 给 `sys_message` 加的 `business_type`、`V18` 的投递表与唯一键 `uk_event_channel_recipient`）；验证：空库启动后六表存在，`SHOW CREATE TABLE` 与 `iboot_admin` 逐表对比无差异
- [x] 1.2 `scripts/dev-init.sh` 增加 `iboot_notify` 建库（与 `iboot_file` 同格式）；验证：全新数据卷执行 `dev-init.sh` 一键拉起，六个服务健康
- [x] 1.3 编写 `scripts/migrate/sys_*_to_iboot_notify.sql`：建库幂等、六表显式 id 复制、不删源表、附行数与 `max(id)` 校验查询；验证：停写窗口执行后两侧行数与 `max(id)` 完全一致
- [x] 1.4 通知服务数据源切到 `iboot_notify` 并启动；验证：`equivalence.sh --verify --service notify` 通过，列表/收件箱/公告/检索结果与源库一致

## 2. 维护任务迁移

- [x] 2.1 把通知重试与索引对账迁入 `iboot-notify`：`@Scheduled(fixedDelay)` + Redis 锁（`iboot:notify:task:retry` / `iboot:notify:task:reconcile`）；验证：日志可见按间隔执行；同时起两个实例时同一轮只有一个执行者
- [x] 2.2 新增管理端迁移 `V28` 删除 `sys_job` 两行内部任务；验证：任务管理页不再显示这两条，且 notify 日志仍按间隔执行
- [x] 2.3 更新 `doc/20-多渠道通知操作指南.md` 与 `doc/23` §11.7：任务归属由管理端 Quartz 改为通知服务进程内调度；验证：文档可查到该说明与任务列表的变化

## 3. 删除管理端旧实现

- [x] 3.1 删除管理端消息/公告/模板/投递的 `interfaces` / `application` / `domain` / `infrastructure` 实现，以及 6 个 mapper XML 与 19 个测试类；保留 `NotificationPort`、`NotificationCommand`、`HttpNotificationPort`、`NotificationEventType`；验证：`mvn -pl iboot-admin -am compile` 通过且无残留引用
- [x] 3.2 清理删除造成的孤儿：`BusinessMetricsService` 的推送指标方法，`application.yml` 中 `iboot.push.*` 与 `iboot.notification.*` 的渠道配置（仅保留 `base-url`、`internal-token`）；验证：管理端启动正常，`/actuator/health` 为 `UP`
- [x] 3.3 验证保留件仍工作：`HttpNotificationPortTest` 与审批集成测试通过；实际提交一条请假，站点/钉钉投递落库；验证：测试全绿 + 投递记录存在
- [x] 3.4 跑 `iboot-admin` 与 `iboot-notify` 全量单测；验证：`mvn -pl iboot-admin,iboot-notify -am test` 全绿

## 4. 回归登记与基线

- [x] 4.1 `scripts/regress/migrations.txt` 五条由 `transitional` 改为 `done`；验证：`path-conservation.sh` 通过（管理端快照中确实不再出现这五组路径）
- [x] 4.2 刷新 `scripts/regress/golden/admin.json`；验证：与旧基线的 diff 仅包含这五组路径消失，无其它漂移
- [x] 4.3 重新录制 notify 只读等价性基线（录制源改为 `iboot-notify` 自身，管理端旧实现已不存在）；验证：`equivalence.sh --record/--verify --service notify` 一致
- [x] 4.4 全量 `regress.sh`；验证：契约快照、路径守恒、路由覆盖、冒烟、权限矩阵全部 PASS
- [x] 4.5 复核链路追踪与故障隔离仍成立：一次消息请求覆盖 `iboot-gateway → iboot-notify`；停掉 notify 后审批提交仍成功且 `notification_trigger_failed_total` 增加；验证：两条结论均复现

## 5. 文档与状态收敛

- [x] 5.1 更新 `doc/23` §11.7：临时状态表改为「已收敛」，补搬迁与任务的验收记录；验证：§11.7 不再出现「旧实现保留」「仍由管理端调度」
- [x] 5.2 更新 `ROADMAP.md` §4.2：`remove-notify-from-admin` 标记已完成，服务状态表与 §4.2 叙述同步
- [x] 5.3 更新 `doc/20`、`doc/04`、`doc/10`、`doc/15` 中「管理端保留副本」的表述与代码路径；验证：全文检索 `com.iboot.admin.*notification*`、`infrastructure/push`（管理端）无残留引用

## 6. 集成验收与回滚演练

- [x] 6.1 端到端验收：登录 → 建消息 → 发送 → SSE 收到 → 已读 → 公告发布/撤回 → 检索命中，全部经网关；验证：手工走查记录
- [x] 6.2 回滚演练：把 notify 数据源指回 `iboot_admin`，服务可读到源表数据；恢复后再次验证；验证：两次查询结果一致
- [x] 6.3 记录验收结果到 `doc/23` §11.7（执行日期、行数校验、回归结论）；验证：文档包含完整记录
