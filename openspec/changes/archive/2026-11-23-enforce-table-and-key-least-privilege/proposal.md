# 表级与键级最小权限

## Why

B5.5 已经把 MySQL、Redis、Nacos 与 Silo 拆成每服务身份，但 MySQL 仍是整个 `iboot` 库的 DML，
Redis 仍是整个键空间可读写。任一服务被利用后仍能修改其它服务的数据或键，零信任的“最小权限”
只完成到身份层。本变更把授权收紧到表和键前缀。

## What Changes

- MySQL：
  - `mysql-users-init.sh` 只创建/更新账号与 exporter，不再授予库级 DML。
  - 新增 `scripts/mysql-grants-init.sh`，在 `db-init` 建表后执行：共享只读表授予 `SELECT`，
    本服务写表按登记矩阵授予 `INSERT/UPDATE/DELETE`；Flowable `ACT_*`/`FLW_*` 只授予 workflow，
    Quartz `QRTZ_*` 只授予 job；所有服务只对 `sys_operate_log` 有 INSERT（system/monitor 可清理）。
  - 编排增加 `mysql-grants-init` 一次性服务；业务服务等待它完成后再启动。
- Redis：
  - 每个命名 ACL 用户增加键/频道模式：所有服务共享 `login_tokens:*`、`online_token:*`、
    `pat_tokens:*`（只读）与 `pat_last_used:*`；auth 增加 `login_retry:*`、`pat_tokens:*`；system 增加
    `sys_config::*`、`pat_tokens:*` 写权限与 `cache:evict` 频道；ai/notify 只增加自己的
    `iboot:ai:*` / `iboot:notify:*`，notify 另订阅 `iboot:push:events`。
  - monitor 的 Redis 键空间浏览作为显式治理例外，保留宽权限，但账号只注入 monitor。
- 新增 `scripts/regress/middleware-least-privilege.sh`：按服务验证 `SHOW GRANTS`、越界写失败、
  本表写成功，以及 Redis 越界键被拒绝。
- 更新 `docs/27`、`docs/23`、`docker/README.md`、`ROADMAP.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `middleware-auth`：修改“MySQL 应用账号最小权限”和“Redis 禁用默认用户并使用命名 ACL 账号”，
  把库级/全键空间权限收紧为表级与键前缀。

## Impact

- 修改：`scripts/mysql-users-init.sh`、`docker/docker-compose.yml`、`scripts/dev-init.sh`、
  Redis 启动命令、`scripts/regress/regress.sh`、文档与路线图。
- 新增：`scripts/mysql-grants-init.sh`、`scripts/regress/middleware-least-privilege.sh`。
- 不改变账号名、口令注入方式、TLS 或 Nacos/Silo 权限。
- 非目标：列级、行级权限（行级数据范围仍由应用层执行）、ES/Kafka 授权重做。
- 已知例外：`iboot_monitor` 的 Redis 键空间浏览需要宽权限；仅在 monitor 注入并登记。
