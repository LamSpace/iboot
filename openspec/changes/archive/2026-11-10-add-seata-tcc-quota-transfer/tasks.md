# Tasks

## 1. Spike 门禁（不通过即停止）

- [x] 1.1 为 system 增加 `tccDataSource` 与 `tccTransactionManager`，按类名排除 AT 自动代理（`TccDruidDataSource`） → 验证：启动日志显示主数据源被 AT 代理、TCC 数据源未被代理
- [x] 1.2 验证 TCC Try 经 HTTP 进入 system 且 TC 能回调 Confirm/Cancel → 验证：TC 日志出现 `branchType=TCC` 的 Try/Confirm/Cancel，system 方法被调用
- [x] 1.3 验证 TCC 动作的幂等、空回滚与悬挂 → 验证：动作层单测/手工调用重复 Confirm/Cancel、先 Cancel 后 Try 均符合状态机
- [x] 1.4 验证只含 TCC 分支的全局事务可正常提交与回滚 → 验证：TC 日志无 AT 分支，TCC 回滚只执行 Cancel
- [x] 1.5 输出 spike 报告并给出 go/no-go → 验证：`docs/spike/seata-tcc-spike-report.md` 存在且结论明确

## 2. 数据库与权限

- [x] 2.1 `iboot-db-init` 新增 V6：`sys_leave_quota_transfer` 表与 `approval:quota:transfer` 菜单/管理员授权 → 验证：db-init 启动后表与权限存在，重复执行幂等
- [x] 2.2 更新 `SchemaMigratorTest` 的迁移数、表数与索引断言 → 验证：`SchemaMigratorTest` 通过
- [x] 2.3 更新 `docs/07-数据库设计.md` 的表清单与归属 → 验证：文档列出的表与 V6 一致

## 3. system TCC 能力

- [x] 3.1 实现 TCC 仓储与状态机：Try 冻结、Confirm 划转、Cancel 解冻，使用 `tccDataSource` + `tccTransactionManager` → 验证：单测覆盖状态迁移与条件 SQL
- [x] 3.2 实现 `@TwoPhaseBusinessAction` 的 TCC 动作与内部 Try 端点 → 验证：MockMvc 覆盖内部令牌、XID 缺失与成功路径
- [x] 3.3 实现幂等、空回滚与悬挂：`transfer_id`/`xid` 唯一键、`CANCELLED` 墓碑、迟到 Try 拦截 → 验证：单测覆盖重复 Confirm/Cancel、空回滚、悬挂
- [x] 3.4 补 Testcontainers 集成测试：真实 MySQL 上验证冻结、确认、取消与并发 → 验证：`mvn -pl iboot-system -am test` 通过
- [x] 3.5 更新 `docs/23` TCC 章节与 `docs/10` 的 `tccDataSource` 配置 → 验证：文档字段与代码配置一致

## 4. workflow TM 接入

- [x] 4.1 新增 TCC 划转客户端：复用带 XID 拦截器的 `RestClient` 与内部令牌 → 验证：单测覆盖有/无 XID 与下游错误
- [x] 4.2 新增 `POST /api/approval/leave/quota/transfer`，`@GlobalTransactional` 内只调用 TCC Try，不写本地库 → 验证：接口单测覆盖权限、参数校验与成功路径
- [x] 4.3 新增 `seata-tcc-demo` profile 故障切面：`after-tcc-try` 时抛异常驱动 Cancel → 验证：默认 profile 不注册，测试 profile 可触发回滚
- [x] 4.4 更新 `ROADMAP`、`docs/02`、`README` 的 TCC 能力索引与说明 → 验证：文档与实现一致

## 5. 端到端验证与回归

- [x] 5.1 `scripts/regress/seata-tcc.sh`：正常划转、Try 后失败取消、TC 不可用、system 不可用、并发、权限与重复调用 → 验证：脚本退出码 0 且逐项 PASS
- [x] 5.2 全量回归：`mvn test`、`scripts/regress/regress.sh`、AT 的 `scripts/regress/seata.sh` → 验证：全部通过，AT 路径无回归
- [x] 5.3 更新接口快照（新增划转接口）并检查其余服务零 diff → 验证：`regress.sh` 快照比对通过
- [x] 5.4 完成 `docs/23 §17` 验收与排障、spike 报告与风险登记 → 验证：文档步骤可复现

## Workflow follow-up

- Archive this change after review and a green full-stack regression.
- Keep the TCC experiment independent from the AT approval path; do not merge TCC branches into the mainline transaction.
