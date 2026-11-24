# Proposal

## Why

B6 为 MySQL 服务账号启用了 `REQUIRE SUBJECT` 客户端证书。备份命令仍以 `-hlocalhost` 走 Unix socket，
无法提交客户端证书，CI 的 CLI 备份用例报 `Access denied for user 'iboot_job'@'localhost'`。
本地 `regress.sh` 不覆盖 CLI 备份链，因此该回归在本地未暴露。

## What Changes

- 备份命令强制 `-h127.0.0.1` 走 MySQL 容器内 TCP 回环，并提交 `iboot-job` 的客户端证书、私钥与本地 CA。
- `mysqldump`/`mysql` 追加 `--ssl-mode=VERIFY_IDENTITY`、`--ssl-ca`、`--ssl-cert`、`--ssl-key`。
- 新增可配置项 `iboot.backup.command.mysql-client-cert/key/ca` 与 `command.host`；置空证书时回退到仅口令。
- 集成测试的 Testcontainers 环境显式清空证书路径，继续使用 root 直连。
- 同步更新 `docs/21-数据备份与恢复操作指南.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。该修复让已有备份能力适配 B6 的 MySQL 客户端证书要求，不改变授权/备份语义。

## Impact

- 代码：`BackupProperties`、`BackupCommandFactory` 及备份测试。
- 配置：`iboot-job/src/main/resources/application.yml`。
- 文档：`docs/21-数据备份与恢复操作指南.md`。
