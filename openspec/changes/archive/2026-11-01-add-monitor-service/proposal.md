# Proposal

## Why

阶段 2 剩 `monitor` 与 `system` 两个模块。`iboot-admin` 当前仍有 76 条对外路径，其中监控域 19 条（`/api/log` 9、`/api/online` 4、`/api/monitor/{server,redis,minio}` 6），系统域 57 条。

先拆 `monitor`，不是因为它更小，而是因为它是**第一次搬「别人也在写」的表**：

- `sys_operate_log` 现在由 7 个服务的 `OperateLogRecorder` 跨 schema 写入，`sys_login_log` 由 `iboot-auth` 写入，两者只有 admin 在读。前四次拆分搬的表（`sys_file`、`sys_message`、`sys_job`、33 张 workflow 表）都只有 admin 自己写，可以在服务运行时迁移；这次不行，必须停机切换，且漏改任何一份 `iboot.logging.operate-log-table` 都会**静默丢审计记录**——服务照常启动、健康检查照常通过。
- 反过来先拆 `system`，这两张表会先落到 `iboot_system`，等 monitor 拆分时再搬一次，同一份数据做两次停机迁移。
- `system` 拆完 `iboot-admin` 只剩 19 条路径，本质上是「改名收编 + 删模块」的终局动作，应当一次做完，不该先做。

`doc/23` §11.2 第 5 条把审计写入路径的退出条件写成「`iboot-monitor` 拆分后改为内部写入接口或 MQ」，本变更让该条件到期。

## What Changes

- **新建 `iboot-monitor` 服务（端口 8087）**，承载 19 条路径：
  - `/api/log/login/{list,clean,export}`、`/api/log/operate/{list,{id},clean,export}`、`/api/log/run/{list,{id}}`
  - `/api/online/{list,count,{tokenId},export}`
  - `/api/monitor/server`、`/api/monitor/redis{,/keys,/key/{key}}`、`/api/monitor/minio{,/ping}`
- **独立 schema `iboot_monitor`**，本服务 Flyway 管理两张表：`sys_login_log`、`sys_operate_log`（含 `dept_id`、`deleted` 列）。审计表归属转移到本服务。
- **写入目标切换**：7 个服务的 `iboot.logging.operate-log-table` 改为 `iboot_monitor.sys_operate_log`；`iboot-auth` 的登录日志 INSERT 改为 `iboot_monitor.sys_login_log`。
- **停机窗口数据搬迁**：停全部服务 → 复制两张表并校验行数/`max(id)` → 改配置 → 启动 monitor → 启动其余服务。
- **审计表的写侧契约保持不变**：仍由内核异步写入，仍跨 schema，仍失败不阻断业务。本变更只转移所有权，不改通信方式；内部写入端点与 MQ 化留给后续变更。
- **自监控组件不搬迁**：`MonitorFilter`、`MonitoringInterceptor`、`BusinessMetricsService`、`DruidMetricsBinder`、`CustomHealthIndicator` 是**每个服务对自己做的埋点**，留在原服务，不进入 `iboot-monitor`。
- **dashboard / statistics 留在 admin**：两者跨系统域与日志域聚合，随本次拆分登记为临时债务（跨库读 `iboot_monitor.sys_operate_log` / `sys_login_log`），退出条件为 `iboot-system` 拆分。
- **服务监控语义收敛为「监控服务自身资源」**：`/api/monitor/server` 的 Oshi 数据由新服务上报，页面提示同步调整；按服务聚合的 Prometheus 视图另立变更。
- **网关与回归资产**：路由、`routes.txt`、`migrations.txt`（过渡期 `transitional`）、`golden/monitor.json`、冒烟补日志页与在线计数、权限矩阵补监控域、只读等价性新增 `monitor` profile、`dev-init.sh` 增加服务与 schema。
- **管理端旧实现保留**（绞杀者过渡期）：改回网关路由即可回滚；删除由 `remove-monitor-from-admin` 承担。
- **对外契约不变**：路径、方法、响应结构与权限码保持原样，前端零改动。无 **BREAKING**。

不在本变更范围内（显式记录，避免被当成遗漏）：

- 按服务聚合的多实例资源监控（Prometheus 查询）、Spring Boot Admin 归属迁移、Druid 控制台归属。
- 审计写入改为内部 HTTP 端点或 RocketMQ。
- `iboot-system` 拆分，以及 file/notify/job/workflow 的 `sys_user` / `sys_config` / `sys_dict_data` 跨库读取收敛。

## Capabilities

### New Capabilities

- `ops-monitoring`: 监控与日志查询作为独立可部署单元的对外契约——登录/操作/运行日志查询、在线会话查看与强退、服务与中间件资源检查；审计表的归属与写入目标；监控服务不可用时的退化行为。

### Modified Capabilities

- `operation-logging`: 共享审计表的归属从管理端库转移到 `iboot_monitor`；「跨服务审计记录的可见性」由管理端查询改为监控服务查询，且写入目标的新旧切换不得造成记录丢失。
- `auth-service`: 认证服务的写边界新增「登录日志写入 `iboot_monitor.sys_login_log`」，与之并列的 `sys_user` 读写仍直连 `iboot_admin`。

## Impact

**代码**

- 新增模块 `iboot-monitor`：5 个 controller、7 个 application service、登录日志与操作日志的 model/repository/mapper/XML、在线用户的 Redis 读取与强退、服务/Redis/MinIO 检查、运行日志的 ES 查询。
- `iboot-admin` 保留旧实现，本变更不删文件；`remove-monitor-from-admin` 负责删除。

**接口**

- 对外路径零变化，前端零改动。
- 内部无新增服务间调用；数据权限仍走 admin `GET /api/auth/scope`（失败关闭，与 file/job 一致）。

**依赖与配置**

- 新增第 9 个 JVM（本机内存受 R4 约束，按需启动）。
- 新增 MySQL schema `iboot_monitor`、Flyway、MyBatis、Redis 会话、ES 客户端（运行日志）、Nacos 注册与 OTLP 追踪。
- 四个隐式契约（Redis 序列化、CloudEvent 信封、API 版本策略、`jwt.secret` 逐字一致）必须逐项核对；i18n 资源须一并搬迁，否则响应文案会从「操作成功」漂移为 `result.success`。
- 7 个服务的 `iboot.logging.operate-log-table` 与 auth 的登录日志 INSERT 必须与 monitor 上线同一窗口切换，否则审计记录分裂到两个 schema。
