# Tasks

## 1. 模块骨架与隐式契约

- [x] 1.1 根 `pom.xml` 增加 `iboot-monitor` 模块，模块 pom 依赖 `iboot-kernel` 并声明 `spring-boot-maven-plugin`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-monitor -am package -DskipTests` 成功且 jar 含 `Main-Class`
- [x] 1.2 新增 `MonitorApplication`（端口 8087）与 `application.yml`：服务名、`iboot_monitor` 数据源、Redis、`jwt.*`、`iboot.security.whitelist`、Nacos `spring.config.import`、OTLP 追踪；验证：启动后 `/actuator/health` 为 `UP` 且注册中心可见 `iboot-monitor`
- [x] 1.3 逐项落地四个隐式契约：内核 Redis 序列化模板、`CloudEventResponseBodyAdvice`（`basePackages` 指向本服务 controller 包）、`ApiVersionConfig`、`jwt.secret` 与其它服务逐字一致；验证：任一 `/api/**` 响应为 `{type, source, data}` 信封，且用 admin 签发的令牌可访问
- [x] 1.4 复制 `I18nConfig` 与 i18n 资源包，并配置 `iboot.logging.operate-log-table: iboot_monitor.sys_operate_log`；验证：成功响应文案为「操作成功」而非 `result.success`，启动日志无审计表写入错误

## 2. Schema 与数据访问层

- [x] 2.1 编写 `V1__initial_schema.sql`：`sys_login_log`、`sys_operate_log`（含 `dept_id`、`deleted` 列与索引），DDL 由 admin `V1` + `V9` + `V12` 合并；验证：服务启动后两表存在且列与 admin 现有表逐列一致（`SHOW CREATE TABLE` 对比）
- [x] 2.2 搬迁数据访问层：`LoginLog` / `OperateLog` 模型、repository 接口与实现、mapper 接口与 XML、PO；验证：`mvn -pl iboot-monitor test` 通过，mapper XML 在 jar 内 `mapper/` 目录可见
- [x] 2.3 配置 `MyBatisConfig`（手写 `SqlSessionFactory` 的 `@MapperScan`）与 `@DataScope` 拦截器链，别名覆盖 `ll` 与 `ol`；验证：`DataScopeSqlBuilder`/拦截器的切片测试覆盖 DEPT 与 SELF 两种范围

## 3. 操作与登录日志查询

- [x] 3.1 `LogController` 的 9 条端点与对应 application service：登录日志列表/清理/导出、操作日志列表/详情/清理/导出、运行日志列表/详情；验证：controller 测试断言路径、权限码与 CloudEvent 信封，导出返回 `spreadsheetml` 内容类型
- [x] 3.2 数据范围解析改为调用 admin `GET /api/auth/scope`，缓存 60 秒、失败关闭；验证：切片测试覆盖「管理端不可用时拒绝查询」与「受限用户只返回本部门记录」，且分页总数与列表一致
- [x] 3.3 运行日志查询走 Elasticsearch，复制 `ElasticsearchConfig` 与索引名配置，不改变 Logstash 采集链路；验证：切片测试覆盖 ES 不可达时抛出明确异常（而不是返回假空列表），索引名与管理端一致

## 4. 在线会话

- [x] 4.1 `OnlineUserController` 的 4 条端点：列表、计数、强退、导出；列表读取共享 Redis 会话键并通过跨库只读补齐部门/昵称；验证：测试覆盖列表字段完整性与会话存储不可用时的失败关闭
- [x] 4.2 强退删除 `login_tokens:<jti>` 与对应 `online_token:<userId>`，使用内核提供的键前缀常量；验证：集成测试中强退后该用户下一次请求返回 401，其它用户会话不受影响

## 5. 服务与中间件资源检查

- [x] 5.1 `ServerMonitorController`：搬运 Oshi 指标与检查项，`sbaUrl` 仍从配置读取；验证：测试断言响应结构与拆分前字段一致，进程指标来自本服务
- [x] 5.2 `RedisMonitorController` 的 4 条端点与 `MinioMonitorController` 的 2 条端点；验证：测试覆盖中间件不可达时返回该检查项不可用且端点整体仍成功

## 6. 网关与开发环境接入

- [x] 6.1 网关 `application-local.yml` 与 `application-nacos.yml` 增加 `/api/log/**`、`/api/online/**`、`/api/monitor/server/**`、`/api/monitor/redis/**`、`/api/monitor/minio/**` 路由，order 在 `/api/monitor/backup` 之后、admin 兜底之前；验证：经网关访问日志与在线端点返回 200，备份端点仍由 job 承接
- [x] 6.2 `scripts/regress/routes.txt` 增加对应行，`scripts/dev-init.sh` 增加 `iboot-monitor:8087` 与 `iboot_monitor` schema 创建；验证：`route-coverage.sh` 通过，`dev-init.sh` 可拉起服务

## 7. 数据迁移与写侧切换

- [x] 7.1 编写 `scripts/migrate/monitor_to_iboot_monitor.sql`（复制两表）、`monitor_final_check.sql`（行数与 `max(id)` 比对、旧表不再增长）、`monitor_back_to_iboot_admin.sql`（回滚复制）；验证：在本地库执行后两表行数与 `max(id)` 与源表一致
- [x] 7.2 切换 7 个服务的 `iboot.logging.operate-log-table`（admin / ai / file / notify / job / workflow / auth 中实际带 `@Log` 的服务）与 `iboot-auth` 的登录日志 INSERT 目标；验证：全仓 `grep operate-log-table` 无残留 `iboot_admin.sys_operate_log`，auth 的 INSERT 指向 `iboot_monitor`
- [x] 7.3 执行一次停机迁移演练并记录到 `doc/23`：停服务 → 复制 → 校验 → 改配置 → 启动 → 冒烟；验证：切换后旧 schema 两表行数不再增长，新 schema 收到 file 与 monitor 的新记录

## 8. 回归资产

- [x] 8.1 生成 `golden/monitor.json` 契约快照；`migrations.txt` 增加 3 条 `transitional` 登记；验证：`regress.sh` 的契约、路径守恒、路由覆盖三项通过
- [x] 8.2 `smoke.sh` 把审计落库断言改为 `iboot_monitor.sys_operate_log`，新增登录日志列表与在线计数经网关的断言；验证：冒烟全绿且断言覆盖 file 与 monitor 两处写入
- [x] 8.3 `permission-matrix.sh` 增加监控域用例：无权限拒绝、本部门范围只见本部门日志；验证：权限矩阵全绿
- [x] 8.4 `equivalence.py` 增加 `monitor` profile（只覆盖日志列表这类稳定只读请求，不含资源检查这类活数据）；验证：迁移前录制、迁移后经网关重放，差异为空
- [x] 8.5 `trace-chain.sh` 增加 `gateway -> monitor` 的示例与用法说明；验证：一次日志查询的链路包含两个服务

## 9. 文档

- [x] 9.1 `doc/23` 新增 §11.10「`iboot-monitor` 的服务状态」：表结构、跨库读写清单、切换窗口、回滚方式、验收记录；验证：章节包含表格与验收动作
- [x] 9.2 `doc/23` §11.2 第 5 条与 §7.3 路由表、§8.1 服务端口表同步；验证：审计写入目标改指 `iboot_monitor`，端口表含 8087
- [x] 9.3 `doc/11-可观测性设计.md` 写清「每服务自监控埋点」与「监控服务读侧」的区别；`doc/07-数据库设计.md` 增加 `iboot_monitor` schema 一行；验证：两处文档与实现一致
- [x] 9.4 `ROADMAP.md` §4.2 状态表与拆分顺序、`README.md` 模块列表更新；验证：monitor 行标记为已完成并关联变更名

## 10. 集成验收

- [x] 10.1 全量回归：`bash scripts/regress/regress.sh` 全绿，冒烟与权限矩阵条目数较拆分前增加
- [x] 10.2 故障隔离双向验证：停 monitor 时日志接口返回明确降级错误而用户接口正常；停 admin 时日志接口仍正常
- [x] 10.3 审计写入的可用性与失败边界验证：停 monitor 时上传仍成功且审计记录照常写入（跨 schema 写不依赖读侧服务）；临时重命名审计表后上传仍成功、写入方只出现可定位的错误日志，恢复后写入正常
