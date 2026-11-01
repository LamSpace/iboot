# Tasks

## 1. 内核写入目标

- [x] 1.1 在 `KernelLoggingAutoConfiguration` 增加 `iboot.logging.operate-log-table`（默认 `sys_operate_log`）与 `^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)?$` 校验，非法值使启动失败；验证：单测覆盖合法默认值、合法 `schema.table`、非法标识符三种情况，`mvn -s /home/lam/repo/settings.xml -pl iboot-kernel test` 通过
- [x] 1.2 `OperateLogRecorder` 改用配置的表名生成 INSERT 并更新类级/方法 Javadoc；验证：单测断言生成的 SQL 使用 `iboot_admin.sys_operate_log` 这类限定名，且参数绑定数量不变
- [x] 1.3 写入失败改为每进程只打一次 `error`，并累加 `operate_log_write_failed_total{application=...}` 计数；验证：单测模拟 `JdbcTemplate.update` 抛异常，断言计数器增加且重复失败不重复打错误日志
- [x] 1.4 启动后做一次 `SELECT 1 FROM <table> LIMIT 1` 可达性检查，失败只打 `error` 不阻止启动；验证：单测或集成测试以不存在的表启动，断言上下文正常且日志出现一次错误

## 2. 拆分服务配置

- [x] 2.1 `iboot-file`、`iboot-notify`、`iboot-job`、`iboot-ai` 的 `application.yml` 增加 `iboot.logging.operate-log-table: iboot_admin.sys_operate_log`；验证：四个服务启动日志不再出现「记录操作日志失败」，且启动自检无错误（AI 没有 `@Log`，配置它是为了让启动自检不误报）
- [x] 2.2 在 `doc/23` §11.1 的接入步骤与 §11.2 的隐式契约各增加「操作日志写入目标」，写明跨库授权要求与退出条件；验证：按文档步骤能解释为什么新服务必须配置该项，且 §11.2 的条目编号连续

## 3. 回归与验收

- [x] 3.1 在 `scripts/regress/smoke.sh` 的文件上传/删除链路后增加数据库断言：`docker exec iboot-mysql mysql --default-character-set=utf8mb4 -uroot -proot -N -e "SELECT COUNT(*) FROM iboot_admin.sys_operate_log WHERE url LIKE '/api/file/%' AND operate_time >= NOW() - INTERVAL 5 MINUTE"` 大于 0（用 URL 而不是中文标题，避免客户端字符集影响比较）；验证：`bash scripts/regress/smoke.sh` 通过
- [x] 3.2 人工触发 notify 与 job 各一个 `@Log` 端点，确认审计表记录可查；验证：`SELECT` 结果包含对应操作标题，管理端操作日志页可见
- [x] 3.3 执行全量回归；验证：`bash scripts/regress/regress.sh` 全绿（契约零漂移、路径守恒、路由覆盖、冒烟、权限矩阵）
