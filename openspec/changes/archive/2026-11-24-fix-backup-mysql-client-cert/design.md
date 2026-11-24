# Design

## Context

B6 将 `iboot_job` 等 MySQL 服务账号绑定 `REQUIRE SUBJECT '/CN=iboot-job'`。备份命令默认
`docker exec ... iboot-mysql mysqldump -hlocalhost`，客户端通过 Unix socket 连接，无法提交 TLS 客户端证书，
因此被 MySQL 拒绝。JDBC 统计与恢复演练使用 `MYSQL_SSL_PARAMS` 的客户端 keystore，不受影响。

## Decisions

- 备份命令固定 `-h127.0.0.1`：在 MySQL 容器内走 TCP 回环，才能完成 TLS 与客户端证书交换；
  服务端证书 SAN 已包含 127.0.0.1。
- 命令追加 `--ssl-mode=VERIFY_IDENTITY`、`--ssl-ca`、`--ssl-cert`、`--ssl-key`；默认使用
  `/certs/services/iboot-job.pem` 与私钥。路径由 `iboot.backup.command.mysql-client-*` 覆盖，
  置空时回退到仅口令认证（适用于非 mTLS 的备份账号）。
- 集成测试的 Testcontainers 未挂载 `/certs`，显式清空证书路径并继续以 root 走 Testcontainers 连接。
- 不修改 MySQL 账号策略；不把迁移账号暴露给备份。

## Risks / Trade-offs

- [生产备份命令不在 MySQL 容器内] → `command.host`/证书路径均可配置；默认值只适用于本地 Compose。
- [证书路径错误导致备份失败] → 备份启动自检与 CLI 回归覆盖；失败时错误信息包含 mysqldump 退出码。
- [仅口令回退] → 仅在显式清空证书路径时发生，默认保持 mTLS。

## Migration Plan

1. 合并后重新触发 `full-regress`，确认 CLI 备份用例通过。
2. 回滚：把 `command.host` 改回 `localhost` 并清空证书路径，需同时把备份账号移出 `REQUIRE SUBJECT`。

## Open Questions

无。
