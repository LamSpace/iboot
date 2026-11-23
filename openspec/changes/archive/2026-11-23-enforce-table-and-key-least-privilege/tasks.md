# 任务

## 1. MySQL 表级授权

- [x] 1.1 `mysql-users-init.sh` 只创建账号与 exporter，不再授予库级 DML。
- [x] 1.2 新增 `scripts/mysql-table-grants.tsv` 与 `scripts/mysql-grants-init.sh`；支持 `ACT_*`/`FLW_*`/`QRTZ_*` 展开与重复执行。
- [x] 1.3 compose 增加 `mysql-grants-init`，八个数据服务改为等待它完成；`dev-init.sh` 在建表后执行。

## 2. Redis 键级授权

- [x] 2.1 Redis 命令为每个命名用户增加键/频道模式；monitor 保留 `~* &*` 例外。
- [x] 2.2 更新 `/health` 与回归使用的账号说明，确认 exporter 不受影响。

## 3. 回归与文档

- [x] 3.1 新增 `scripts/regress/middleware-least-privilege.sh`：SHOW GRANTS、越界写、本表写、DDL、Redis 键模式。
- [x] 3.2 `regress.sh` 接入新检查。
- [x] 3.3 更新 `docs/27` §13.1/§21.4、`docs/23` §11.12/§14.6、`docker/README.md`、`ROADMAP.md`。

## 4. 验证

- [x] 4.1 `mysql-grants-init.sh` 连续执行两次结果一致。
- [x] 4.2 `middleware-least-privilege.sh` 通过。
- [x] 4.3 完整本地回归通过（8a 32/32、8c 6/6）；仅剩 Docker 备份关闭导致的已知冒烟用例失败。
- [x] 4.4 八个服务在收紧后仍能完成登录、用户/文件/审批/通知/任务流程。
