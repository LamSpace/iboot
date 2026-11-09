# Tasks

## 1. Spike 门禁（不通过即停止）

- [x] 1.1 在 `iboot-workflow` / `iboot-system` 的最小端点上接入 Seata starter，验证 Java 25 + Boot 4 可启动并完成一次 AT 提交与回滚 → 验证：spike 日志出现全局事务 begin/commit/rollback，两个服务健康检查通过
- [x] 1.2 验证 Druid 1.2.28 是否被 Seata 自动代理；不能则改为手工声明 `DataSourceProxy` → 验证：一条业务 SQL 产生 `undo_log` 行并在回滚后消失
- [x] 1.3 验证 `RestClient` 的 XID 透传与服务端 MVC 绑定 → 验证：调用方与资源方日志具有相同 XID，资源方写入被登记为分支
- [x] 1.4 验证共享库同 URL 的 resourceId 行为，并记录拆库时的唯一化写法 → 验证：TC 日志显示两个分支共享 resourceId 但按 branchId 分别提交/回滚；spike 报告记录 `DataSourceProxy(ds, uniqueId)` 写法
- [x] 1.5 验证 seata-server 镜像的端口、健康检查、file store 目录与控制台可用性 → 验证：TC 8091 可连、会话卷持久化、镜像无 7091 控制台；停 TC 时业务 fail-closed
- [x] 1.6 输出 spike 报告并给出 go/no-go 结论 → 验证：`docs/spike/seata-2.5-spike-report.md` 存在且结论明确

## 2. 基础设施常驻

- [x] 2.1 `docker/docker-compose.yml` 增加 `seata-server`：默认常驻、named volume、内存上限、`7091` 仅绑定 localhost → 验证：`docker compose up -d --wait` 后服务健康，重启后卷仍保留
- [x] 2.2 `scripts/dev-init.sh` 增加 seata-server 启动与 `8091` 健康等待，排在 workflow/system 之前 → 验证：`dev-init.sh --status` 显示 running，冷启动顺序正确
- [x] 2.3 `.env.example` 增加 `IBOOT_SEATA_ENABLED`、`SEATA_SERVER_ADDR`、`IBOOT_SYSTEM_INTERNAL_TOKEN` 占位 → 验证：占位存在且无真实密钥入仓
- [x] 2.4 在 `docs/23` 补充组件、端口、内存、启动顺序与容器计数 → 验证：按文档步骤可从零启动 seata-server

## 3. 数据库迁移

- [x] 3.1 `iboot-db-init` 新增迁移：`undo_log`、`sys_user_leave_quota`、`sys_leave_quota_log` 及唯一索引 → 验证：db-init 启动后三张表与索引存在
- [x] 3.2 迁移测试断言表与索引存在、迁移可重复执行 → 验证：`SchemaMigratorTest` 通过
- [x] 3.3 更新 `docs/07-数据库设计.md` 的表清单与归属 → 验证：文档列出的表与迁移文件一致

## 4. system 额度能力

- [x] 4.1 额度仓储：默认值读取、首次扣减懒创建、条件更新、幂等日志 → 验证：单测覆盖首次创建、重复扣减、额度不足、并发扣减
- [x] 4.2 `POST /internal/leave/quota/deduct`：内部令牌鉴权、失败关闭、携带 XID 但 Seata 未启用时拒绝 → 验证：MockMvc 覆盖 200/401/503/额度不足
- [x] 4.3 `GET /internal/leave/quota`：按 userId + year 只读查询，无行时返回默认值且不写库 → 验证：单测覆盖有行、无行、默认值非法
- [x] 4.4 注册 system 侧 XID MVC 拦截器与 Druid 数据源代理配置 → 验证：集成测试中 Controller 内可见 XID，请求结束后被清理
- [x] 4.5 更新 `docs/23 §8.7` 内部调用清单与 `§11.12` 的 `undo_log` 写入例外 → 验证：文档路径与代码端点一致

## 5. workflow AT 接入

- [x] 5.1 额度客户端 + `ClientHttpRequestInterceptor`：有 XID 写 `TX_XID`，无 XID 不写，超时与错误映射 → 验证：单测覆盖有/无 XID 与下游错误
- [x] 5.2 `ApprovalApplicationService.approve` 改为 `@GlobalTransactional`，顺序为远程扣减 → `completeTask` → 更新申请 → 验证：集成测试中远程失败时本地不变
- [x] 5.3 新增 `GET /api/approval/leave/quota`，以当前用户 ID 代理 system 内部查询 → 验证：单测覆盖登录、年度、下游失败
- [x] 5.4 明确失败关闭的错误码与文案：额度不足、system 不可用、TC 不可用 → 验证：测试覆盖三类错误及响应体
- [x] 5.5 单测：并发审批、重复确认、额度不足时不产生部分写入 → 验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-workflow,iboot-system -am test` 通过
- [x] 5.6 更新 `docs/23 §16` Seata 章节（边界、XID 传播、undo_log、失败语义、AT/TCC 混用预告） → 验证：章节内容与 design.md 决策一致
- [x] 5.7 更新 `ROADMAP.md` §3 判据、§4 版本矩阵、§5 R4/R14/R15、§6 容器数、§7.10/§7.11，以及 `docs/02`、`README.md` → 验证：文档与实测版本、容器数一致

## 6. portal 额度展示

- [x] 6.1 请假表单展示“剩余 x/10”badge；查询失败显示“—”，不阻断提交 → 验证：门户 `vue-tsc && vite build` 通过；额度接口已用真实服务验证
- [x] 6.2 剩余为 0 时给出软提示，不阻断提交 → 验证：组件测试断言提示存在且提交按钮可用
- [x] 6.3 补充中英文 i18n 文案 → 验证：切换语言后 badge 与提示文案正确

## 7. 端到端验证与回归

- [x] 7.1 `scripts/regress/seata.sh`：登录、造额度、提交、审批与 SQL 断言，覆盖正常、额度不足、后置失败回滚、TC 不可用、system 不可用、并发、幂等 → 验证：脚本退出码 0 且逐项输出 PASS
- [x] 7.2 仅测试 profile 激活的故障注入拦截器（`X-Seata-Demo-Fail: after-remote`），并断言默认 profile 不注册 → 验证：默认 profile 下 Bean 不存在，测试 profile 下可稳定触发回滚
- [x] 7.3 全量回归：`mvn test`、`scripts/regress/regress.sh`、`smoke.sh`、`route-coverage.sh`，并检查接口快照零 diff → 验证：全部通过

## Workflow follow-up

- Archive this change after review and a green full-stack regression.
- Create the follow-up change `add-seata-tcc-quota-transfer` only after this change is merged and stable.
