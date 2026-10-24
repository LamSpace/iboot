# Tasks

## 1. 配置与数据库迁移

- [x] 1.1 新增 `iboot.backup.*` 配置绑定（enabled、encryption-key、数据库连接、命令模板、演练库前缀、stale 超时），默认 `enabled=false`；在 `.env.example` 增加 `IBOOT_BACKUP_ENCRYPTION_KEY=` 空占位符；验证：配置绑定单测通过，且未配置密钥时应用按默认配置正常启动
- [x] 1.2 编写 Flyway `V21__add_data_backup.sql`：创建 `db_backup_record`、`db_restore_record`，种入 `sys_job`（`backupTask.run`，`0 0 2 * * ?`，`concurrent=0`）、备份管理菜单与权限（列表/立即备份/恢复演练）并绑定管理员角色；验证：全新库 V1→V21 与既有 V20 库增量迁移均成功
- [x] 1.3 按 `ApprovalSchemaMigrationTest` 的 Testcontainers 模式补充迁移测试；验证：断言两张表、任务种子与菜单权限存在，且 `flyway_schema_history` 无失败记录

## 2. 加解密与命令执行

- [x] 2.1 实现 AES-256-GCM 加解密组件（自描述文件头：magic、版本、IV；显式 `update/doFinal` 读取到 EOF）；验证：单测覆盖往返一致、篡改密文失败、错误密钥失败、空输入
- [x] 2.2 实现密钥加载与启动期校验；验证：`enabled=true` 且密钥缺失/无法解码/非 32 字节时上下文启动失败且错误信息含 `IBOOT_BACKUP_ENCRYPTION_KEY`；`enabled=false` 时正常启动
- [x] 2.3 实现 `CommandRunner`（ProcessBuilder 版本）与 `BackupExecutor` 的 dump/restore 命令组装；验证：单测断言参数以列表传递、无 shell 拼接、密码只经 `MYSQL_PWD` 环境变量、包含 `--single-transaction --quick --default-character-set=utf8mb4 --set-gtid-purged=OFF --no-tablespaces`
- [x] 2.4 处理命令不可用、非零退出码与中断；验证：单测模拟失败，断言抛出受控异常、错误信息已脱敏、临时文件被删除

## 3. 备份编排与接口

- [x] 3.1 实现备份记录领域模型、状态机、仓储接口与持久化映射；验证：单测覆盖 `RUNNING→SUCCESS/FAILED` 合法迁移与非法迁移拒绝
- [x] 3.2 实现备份流水线：插入运行中记录 → 导出 → 计算明文 SHA-256 → 加密 → 临时文件 → 存储 → 更新记录；验证：单测使用 FakeExecutor 与内存存储，断言对象键位于 `backup/` 前缀、校验和与大小正确、状态为成功
- [x] 3.3 实现并发闸门与超时运行中记录重置；验证：单测断言第二次触发被拒绝且记录数不增加，超时记录在下一次执行时被置为失败
- [x] 3.4 实现 Quartz `BackupTask` 并接入现有执行器；验证：单测断言功能关闭时 no-op、启用时调用备份服务；启动后 `sys_job` 种子可被调度初始化加载
- [x] 3.5 实现备份管理接口（列表、手动触发、演练记录查询）并加权限校验；验证：Controller 测试覆盖无权限拒绝、功能未启用错误、触发后返回记录标识
- [x] 3.6 为备份流水线补充失败路径单测（导出失败、存储失败、记录更新失败）；验证：单测断言状态为失败、无可用产物、无残留临时文件

## 4. 恢复演练

- [x] 4.1 实现恢复目标守卫；验证：单测拒绝 `iboot_admin`、不以 `iboot_restore_` 开头的库名与非法字符，接受 `iboot_restore_drill`
- [x] 4.2 实现演练服务：取对象 → 解密 → 校验 SHA-256 → 重建演练库 → 还原 → 逐表行数比对；验证：单测断言密文损坏时在 DDL 前失败，行数一致时为成功，存在差异时为不一致并列出差异表
- [x] 4.3 实现演练记录持久化与触发接口；验证：Controller 与仓储测试断言成功/不一致/失败三种状态均可落库并查询
- [x] 4.4 证明演练不触碰业务库；验证：集成测试中演练前后 `iboot_admin` 各表行数与结构摘要保持不变

## 5. Testcontainers 集成测试

- [x] 5.1 使用本地 `mysql:8.0.45` 启动容器并执行 V1–V21，插入可预测的测试数据；验证：迁移完成且测试数据行数符合预期
- [x] 5.2 用容器内 `mysqldump`/`mysql` 实现测试版 `CommandRunner`，配合内存存储跑通备份到演练的完整链路；验证：备份记录成功、对象可解密、演练成功且逐表行数一致
- [x] 5.3 覆盖失败路径（篡改密文、快照不一致、对非成功备份发起演练）；验证：分别得到失败或不一致状态，且演练库无半成品数据
- [x] 5.4 确认集成测试离线可跑；验证：在不拉取新镜像的前提下测试通过（复用本地 MySQL 与 Ryuk 镜像）

## 6. 管理端页面

- [x] 6.1 新增 `api/backup.ts` 与 `views/monitor/BackupView.vue`，接入路由与菜单权限，提供列表、立即备份、恢复演练与详情；验证：`cnpm run build` 通过，有权限账号可打开页面
- [x] 6.2 处理页面状态：功能未启用提示、运行中禁止重复触发、演练结果与差异表展示；验证：浏览器中手工验证三种状态
- [x] 6.3 补齐页面文案的国际化（若沿用项目 i18n 机制）；验证：中英文切换无缺失 key

## 7. 文档

- [x] 7.1 新增 `doc/21-数据备份与恢复操作指南.md`，覆盖启用步骤、密钥生成与保管、执行命令配置、演练安静窗口、差异处置与回滚；验证：按文档命令逐条执行可复现
- [x] 7.2 更新 `.env.example` 说明与文档索引；验证：`.env.example` 不含真实密钥，`git status` 不包含 `.env`

## 8. 端到端验收

- [x] 8.1 启动 `mysql` 与 `minio`，将 `sys.file.storageType` 切为 `minio`，启用备份后手动触发；验证：MinIO `backup/` 前缀下出现对象，备份记录为成功
- [x] 8.2 对成功备份发起恢复演练；验证：`iboot_restore_drill` 被重建、逐表行数一致、演练记录为成功，业务库行数不变
- [x] 8.3 验证定时触发；验证：临时调整 cron 或等待调度后，出现 `trigger_type=SCHEDULED` 的记录
- [x] 8.4 全量回归；验证：`mvn -s /home/lam/repo/settings.xml verify` 通过，portal 构建通过，既有通知/审批/检索测试无新增失败
