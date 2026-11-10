# Seata TCC Spike 报告

日期：2026-11-10

## 结论

**GO。** Seata 2.5.0 的 TCC 可以在当前项目落地：非代理数据源、TCC over HTTP、
协调器回调 Confirm/Cancel、只含 TCC 分支的全局事务提交与回滚均已实测通过。

```text
workflow (TM)                    system (RM)
@GlobalTransactional             @LocalTCC @TwoPhaseBusinessAction
  POST /api/approval/...           Try  冻结额度 + 写 TRYING
    ── TX_XID ──────────────────>  Confirm  Release/CONFIRMED
                                   Cancel   Release/CANCELLED
```

## 环境

| 组件 | 版本 |
| --- | --- |
| Java | 25.0.3 |
| Spring Boot | 4.0.3 |
| Seata Server / Client | 2.5.0（`org.apache.seata`） |
| Druid | 1.2.28（`druid-spring-boot-4-starter`） |
| MySQL | 8.0.45 |

system 与 workflow 在宿主机运行，MySQL / Seata / Nacos / Redis 在 Docker 中；
workflow 以 `seata-tcc-demo` profile 启动，用于注入 Try 后失败。

## 关键发现

### 1. `excludes-for-auto-proxying` 按类名匹配，不是 Bean 名

最初配置：

```yaml
seata:
  excludes-for-auto-proxying:
    - tccDataSource
```

启动日志仍出现：

```text
Auto proxy data source 'tccDataSource' by 'AT' mode.
```

反编译确认 `SeataAutoDataSourceProxyCreator.shouldSkip` 用
`beanClass.getName()` 与排除集合比较。主数据源与 TCC 数据源同为 Druid 连接池，
无法用类名区分，因此 TCC 数据源改为独立子类 `TccDruidDataSource`：

```yaml
seata:
  excludes-for-auto-proxying:
    - com.iboot.system.infrastructure.config.TccDruidDataSource
```

修正后日志只剩主数据源：

```text
Auto proxy data source 'dataSource' by 'AT' mode.
```

### 2. 第二个 DataSource 会让 Druid starter 退让

`DruidDataSourceAutoConfigure.dataSource()` 带
`@ConditionalOnMissingBean({DruidDataSourceWrapper, DruidDataSource, DataSource})`。
出现第二个 `DataSource` 后 starter 不再创建主数据源，因此主数据源改为在
`PrimaryDataSourceConfig` 显式声明并保持 `@Primary`。

### 3. TCC 动作必须标 `@LocalTCC`

只写 `@TwoPhaseBusinessAction` 时，`GlobalTransactionScanner` 不会增强该 Bean：

```text
The needed enhancement business beans are : []
```

全局事务仍然提交成功，但**没有任何分支**，Confirm 不会执行。补上
`@LocalTCC` 后：

```text
The needed enhancement business beans are : [leaveQuotaTransferTccAction]
Bean [...LeaveQuotaTransferTccAction] would use interceptor [...TccActionInterceptorHandler]
```

### 4. 分支类型与上下文符合预期

```text
BranchRegisterRequest{xid='...', branchType=TCC, resourceId='leaveQuotaTransfer',
  applicationData='{"actionContext":{"sys::prepare":"tryTransfer","sys::commit":"confirm",
  "sys::rollback":"cancel","transferId":"...","fromUserId":1,"toUserId":2,
  "year":2026,"count":2,...}}'}
Register branch successfully, ... resourceId = leaveQuotaTransfer
```

`transferId`、双方用户、年度与次数都随分支登记，Confirm/Cancel 只依赖
`BusinessActionContext`。

### 5. 只含 TCC 分支的提交与回滚

正常划转：

```text
GlobalCommitRequest{xid='...'} -> Committed
Committing global transaction is successfully done
TCC Confirm 完成：xid=..., transferId=..., from=1, to=2, count=2
```

回滚（`X-Seata-Demo-Fail: after-tcc-try`）：

```text
GlobalRollbackRequest{xid='...'} -> Rollbacked
BranchRollbackResponse{... branchStatus=PhaseTwo_Rollbacked, resultCode=Success}
TCC Cancel 完成：xid=..., transferId=..., from=1, count=3
```

两种情况下 `undo_log` 均为空，TC 日志没有 AT 分支。

### 6. 额度不足会走空回滚

划出方剩余额度为 0 时：

```text
HTTP 400 请假额度不足
TCC Cancel 空回滚，写入取消墓碑：xid=..., transferId=...
```

Try 的本地事务回滚带走了 `TRYING` 记录，协调器随后调用 Cancel，Cancel 写入
`CANCELLED` 墓碑且不改变额度。这是空回滚在真实链路中的一次验证。

### 7. 读操作不会产生 AT 分支

`ConnectionProxy.register()` 只在存在 undo log（DML）或 lock key（`SELECT ... FOR UPDATE`）
时登记分支。Try 内读取默认额度配置不会给全局事务增加 AT 分支；这一点也被上面的
`undo_log = 0` 与 TC 日志印证。

### 8. 第二个 `JdbcTemplate` 会让 Spring Boot 自动配置整体退让

首次跑 AT 回归时发现：正常审批只有 workflow 的分支，system 的额度扣减没有登记 AT
分支，故障回滚后额度未恢复。原因是 `tccJdbcTemplate`（`JdbcTemplate` 类型）已存在，
`JdbcTemplateAutoConfiguration` 的 `@ConditionalOnMissingBean(JdbcOperations)` 整体退让，
容器里没有主 `jdbcTemplate`；`LeaveQuotaRepositoryImpl` 按类型注入，拿到了
**TCC 数据源**上的模板，扣减因此绕开了 AT 代理。

修复：在 `PrimaryDataSourceConfig` 显式声明 `@Primary` 的主 `jdbcTemplate`，并给
`LeaveQuotaRepositoryImpl` 加 `@Qualifier("jdbcTemplate")`。修复后
`/actuator/beans` 显示仓储依赖 `jdbcTemplate`，`scripts/regress/seata.sh` 24/24 通过。

这个问题的教训是：**新增第二个 `JdbcTemplate` 时，必须同时确认主模板仍存在**，
否则所有按类型注入的旧代码会静默切换到新数据源。

## 未覆盖项与后续验证

- **悬挂**：手工制造“Cancel 先于 Try”需要伪造协调器时序，放在动作层单测覆盖。
- **重复 Confirm/Cancel**：动作层单测覆盖；端到端脚本可重复调用内部 Try 验证幂等。
- **协调器 / 系统服务不可用**：留给 `scripts/regress/seata-tcc.sh` 与全量回归。
- **第二个连接池的资源占用**：池大小 1~5，待在 `docker stats` 与长期运行中观察。

## 对变更的修正

- `design.md` D3 已改为“独立子类 + 类名排除”，并记录主数据源显式声明的原因。
- TCC 动作补充 `@LocalTCC`，否则不登记分支。
- 其余设计（TM 不写本地库、手工状态机、幂等/空回滚/悬挂、故障钩子）保持不变。
