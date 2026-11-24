# Tasks

- [x] 1. 备份命令改用 `-h127.0.0.1` 并追加客户端证书/CA 参数；verification: 单元测试断言 `--ssl-mode`/`--ssl-cert`/`--ssl-key`。
- [x] 2. 新增 `iboot.backup.command.mysql-client-cert/key/ca` 与 `command.host` 配置；verification: `application.yml` 默认指向 `/certs/services/iboot-job.pem`。
- [x] 3. 集成测试清空证书路径以适配 Testcontainers；verification: `BackupCommandFactoryTest` 通过。
- [ ] 4. 重新触发 GitHub `full-regress`；verification: CLI 备份用例通过、全栈回归通过。

## Workflow follow-up

- [ ] 归档 OpenSpec 变更。
- [ ] 新建 `fix/backup-mysql-client-cert` 分支，提交、推送、创建 PR；CI 通过后合并到 `master`。
