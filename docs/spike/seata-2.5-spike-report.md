# Seata 2.5.0 兼容性 Spike 报告

> 日期：2026-11-09
> 范围：`add-seata-at-leave-quota` 阶段 2 收尾的 spike 门禁
> 结论：**GO**。AT 跨服务提交与回滚在 Java 25 + Boot 4 + Druid + 虚拟线程下可用；共享库的 resourceId 复用需要记录，拆库时必须唯一化。

## 1. 环境

| 项 | 版本 |
|---|---|
| JDK | 25.0.3 LTS |
| Spring Boot | 4.0.3 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Seata 客户端 | 2.5.0（`org.apache.seata`） |
| Seata Server | `apache/seata-server:2.5.0` |
| 数据库 | MySQL 8.0.45，单库 `iboot` |
| 连接池 | Druid 1.2.28（system / workflow） |
| 参与方 | `iboot-system`（RM）、`iboot-workflow`（TM + RM） |

## 2. 实测结论

### 2.1 启动与代理

- 两个服务在 `seata.enabled=true` 下正常启动，TM 与 RM 均注册到 `127.0.0.1:8091`。
- Druid 数据源被 Seata 自动代理，日志：
  `Auto proxy data source 'dataSource' by 'AT' mode.`
- `@GlobalTransactional` 被正确织入 `ApprovalApplicationService`，日志出现：
  `Bean [approvalApplicationService] would use interceptor [GlobalTransactionalInterceptorHandler]`。
- 虚拟线程未影响请求线程内的 XID：全局事务在同步调用链上完成。

### 2.2 XID 传播

- workflow 的 `ClientHttpRequestInterceptor` 在 `RootContext.getXID()` 非空时写入 `TX_XID`。
- system 的 `JakartaSeataWebMvcConfigurer` 在 Controller 前绑定 XID。
- 证据：system 日志在 workflow 的 XID 下登记分支：
  `branch register success, xid:172.18.0.2:8091:..., branchId:..., lockKeys:sys_leave_quota_log:...;sys_user_leave_quota:...`

### 2.3 成功提交

一次审批通过产生两个 AT 分支：

| 分支 | 持有方 | 锁键 |
|---|---|---|
| quota | `iboot-system` | `sys_leave_quota_log`、`sys_user_leave_quota` |
| approval | `iboot-workflow` | `biz_leave_application`、`ACT_*` / `ACT_HI_*` |

TC 日志出现 `GlobalCommitRequest` → 两个 `BranchCommitResponse(PhaseTwo_Committed)` → `Committing global transaction is successfully done`。成功后 `undo_log` 先出现 2 行，约 3 秒后异步清理为 0。

### 2.4 回滚（关键场景）

通过仅 `seata-demo` profile 激活的故障切面，在远程额度扣减成功返回后抛出异常：

- 远程 quota 分支已提交，`used_count` 从 0 变为 1；
- workflow 的本地写入尚未执行，因此只有 quota 分支；
- TC 收到 `GlobalRollbackRequest`，system 执行 `BranchRollbackResponse(PhaseTwo_Rollbacked)`；
- 最终 `used_count=0`、申请仍为 `PENDING`、`undo_log` 为空。

结论：**AT 能撤销已提交的远程分支**，满足阶段 2 判据。

### 2.5 失败关闭

| 场景 | 结果 |
|---|---|
| 停 seata-server | 审批返回失败（`FrameworkException: can not register RM`），申请 `PENDING`，额度不变 |
| 暂停 system | 额度调用 5 秒读取超时，审批返回 503「额度服务不可用，审批未完成」，申请 `PENDING`，额度不变 |
| 额度不足 | 审批返回 400「请假额度不足」，申请 `PENDING`，`used_count` 不变 |
| 并发（剩余 1 次，两个审批） | 一个 `APPROVED`，一个 `PENDING`，`used_count` 只增加 1 |

TC 不可用时的原始异常是 `org.apache.seata.common.exception.FrameworkException`。已新增 `SeataExceptionHandler` 将其映射为 503「分布式事务协调器不可用，审批未完成」，避免以 500 技术错误返回。

### 2.6 共享库 resourceId

两个服务使用相同的 JDBC URL，因此自动代理得到相同的 `resourceId`：

```
resourceId = jdbc:mysql://localhost:3306/iboot
```

TC 仍按 `branchId` 分别登记与提交；回滚时由持有通道的 RM 读取共享 `undo_log` 执行。**单库下可用**，原因是 undo_log 与业务表在同一个库，任一 RM 都能执行回滚。

风险与后续：

- 服务拆分到独立 schema 后，resourceId 必须唯一，否则 TC 的通道映射可能覆盖；
- 唯一化写法是手工声明 `new DataSourceProxy(dataSource, "<service-name>")`，并关闭自动数据源代理；
- 本次不在主线实施手工代理：Druid starter 的 `@ConditionalOnMissingBean` 与自定义代理存在启动次序耦合，且单库场景未出现回滚失败。该事项在拆库变更中处理。

### 2.7 Seata Server 镜像

- TC 监听 `8091`；
- `apache/seata-server:2.5.0` 镜像**不提供** 7091 控制台（`curl localhost:7091` 连接失败），排障以日志与数据库为准；
- file store 会话目录：`/seata-server/sessionStore`，compose 以 named volume 挂载；
- 默认堆 2g，通过 `JVM_XMS` / `JVM_XMX` / `JVM_MaxDirectMemorySize` 覆盖为 256m / 512m / 256m；
- 镜像无 HTTP 健康端点，compose 使用 TCP `/dev/tcp/127.0.0.1/8091` 探测。

## 3. Go / No-Go

**GO**。需要带入主线的约束：

1. 审批路径强依赖 TC，保持 fail-closed，不提供静默降级。
2. 全局事务内只放纯 DB 写；通知继续走 outbox。
3. 参与方必须同时启用 Seata；RM 收到 `TX_XID` 但自身未启用时拒绝请求。
4. 拆库时必须为每个服务设置唯一 resourceId。
5. 不使用 7091 控制台做验收，改用 TC 日志、`undo_log` 与业务表断言。
