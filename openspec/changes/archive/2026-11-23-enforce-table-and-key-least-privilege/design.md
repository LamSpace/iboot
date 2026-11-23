# 表级与键级最小权限 — 设计

## 1. MySQL 执行顺序

表级授权要求表已存在，因此把账号创建与授权拆成两步：

1. `mysql-users-init`（建表前）：创建/更新 `iboot_<service>` 与 exporter 账号，只给 `USAGE`；
2. `db-init`：Flyway 建表；
3. `mysql-grants-init`（建表后）：撤销旧授权，按矩阵重新授予；
4. 业务服务启动。

compose 增加一次性服务 `mysql-grants-init`，八个数据服务的 `depends_on` 从 `db-init` 改为
`mysql-grants-init: service_completed_successfully`。宿主机 `dev-init.sh` 在 `iboot-db-init` 之后
显式执行 `docker compose run --rm --no-deps mysql-grants-init`。

## 2. 授权矩阵

共享读：所有数据服务 `GRANT SELECT ON iboot.*`。这是 docs/23 §11.12 的共享只读契约。

写权限按表登记（`scripts/mysql-table-grants.tsv`）：

| 服务 | 写表 |
|---|---|
| system | `sys_config`、`sys_dept`、`sys_dict_data`、`sys_dict_type`、`sys_leave_quota_log`、`sys_leave_quota_transfer`、`sys_login_log`、`sys_menu`、`sys_operate_log`、`sys_post`、`sys_role`、`sys_role_dept`、`sys_role_menu`、`sys_user`、`sys_user_leave_quota`、`sys_user_pat`、`sys_user_post`、`sys_user_role` |
| workflow | `biz_leave_application`、`biz_notification_outbox`、`ACT_*`、`FLW_*`、`sys_operate_log`（INSERT） |
| file | `sys_file`、`sys_operate_log`（INSERT） |
| notify | `sys_message`、`sys_message_receiver`、`sys_message_template`、`sys_notice`、`sys_notice_read`、`sys_notification_delivery`、`sys_operate_log`（INSERT） |
| job | `db_backup_record`、`db_restore_record`、`sys_job`、`sys_job_log`、`QRTZ_*`、`sys_operate_log`（INSERT） |
| monitor | `sys_login_log`、`sys_operate_log` |
| auth | `sys_login_log`（INSERT）、`sys_user`、`sys_user_pat`、`sys_operate_log`（INSERT） |
| ai | `ai_agent`、`ai_agent_run`、`ai_document`、`ai_document_chunk`、`ai_kb_share_dept`、`ai_knowledge_base`、`sys_operate_log`（INSERT） |

- `ACT_*`、`FLW_*`、`QRTZ_*` 由脚本查询 `information_schema.tables` 展开后逐表授权；
  表名 MUST 通过 `[A-Za-z0-9_]+` 校验，禁止拼接未校验输入。
- `sys_operate_log` 由 kernel 审计切面在所有服务直写，因此所有服务都保留 INSERT；
  system/monitor 额外拥有 UPDATE/DELETE 以清理历史记录。
- 授权前执行 `REVOKE ALL PRIVILEGES, GRANT OPTION FROM user`，清除旧库级 DML；脚本可重复执行。

## 3. Redis 键模式

使用 Redis 8 的 ACL 键/频道模式，仍保留 `+@all` 命令集，只收紧键空间：

| 用户 | 键/频道模式 |
|---|---|
| 所有数据服务 | `~login_tokens:*`、`~online_token:*`、`%R~pat_tokens:*`、`~pat_last_used:*` |
| auth | 额外 `~login_retry:*`、`~pat_tokens:*` |
| system | 额外 `~pat_tokens:*`、`~sys_config::*`、`&cache:evict` |
| ai | 额外 `~iboot:ai:*` |
| notify | 额外 `~iboot:notify:*`、`&iboot:push:events` |
| monitor | `~*`、`&*`（键空间浏览治理例外） |
| exporter | 现有只读命令集与 `~*` 读权限不变 |

- `login_tokens:*` 与 `online_token:*` 需要写权限：JWT 过滤器会滑动续期两者的 TTL。
- `pat_tokens:*` 只读：PAT 校验只判断存在；`pat_last_used:*` 单独可写。
- system 的 `pat_tokens:*` 写权限用于撤销时删除键。
- `sys_config::*` 是 Spring Cache 默认前缀；`cache:evict` 是二级缓存失效频道。
- monitor 的宽权限只注入 monitor；回归断言其它用户没有 `~*`。

## 4. 回归

`scripts/regress/middleware-least-privilege.sh`：

1. 对八个服务执行 `SHOW GRANTS FOR CURRENT_USER()`，断言不存在 `ON iboot.*` 的 DML；
2. 负向：服务 A 更新服务 B 的写表，期望 `1142` 权限错误；
3. 正向：服务更新自己的写表（`WHERE 1=0`），期望成功；
4. DDL 负向：`CREATE TABLE` 期望权限错误；
5. Redis 正向：ai 写 `iboot:ai:*`，notify 写 `iboot:notify:*`；
6. Redis 负向：ai 写 `iboot:notify:*`、file 写 `pat_tokens:*`，期望 `NOPERM`；
7. Redis 共享：file 读 `login_tokens:*`、`pat_tokens:*` 成功；
8. 监控例外：只有 monitor 拥有 `~*`。

脚本通过 `docker exec` 在 MySQL/Redis 容器内执行，携带服务账号与 TLS，不读取 root 以外的凭据。

## 5. 回滚

- MySQL：删除 `mysql-grants-init` 服务与 `mysql-table-grants.tsv`，`mysql-users-init.sh` 恢复
  库级 DML；业务服务依赖改回 `db-init`。
- Redis：移除 ACL 键模式，保留命名用户。
- 回滚不改变账号名与口令。
