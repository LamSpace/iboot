# Tasks

## 1. RocketMQ 服务端 ACL

- [x] 1.1 新增 Broker/Proxy 入口脚本与配置生成：ACL 2.0 开关、Provider、运维凭据、RocksDB 卷；verification: 容器启动后 `mqadmin listUser` 只显示运维账号。
- [x] 1.2 新增 `rocketmq-acl-init`：幂等创建 `iboot_workflow`、`iboot_notify` 与最小 ACL；verification: 连续执行两次退出码为 0，`mqadmin getAcl` 内容一致。
- [x] 1.3 topic 初始化与 `mqadmin` 改用环境生成的 `tools.yml`；verification: 默认 `rocketmq2/12345678` 无法执行管理命令。

## 2. 客户端凭据

- [x] 2.1 `iboot-workflow`/`iboot-notify` 的 RocketMQ 属性与 `ClientConfiguration` 注入 access key/secret key；verification: 单元测试覆盖有/无凭据两种构造。
- [x] 2.2 compose、`dev-init.sh`、`.env.example` 与 CI 注入各自凭据，并清空其它服务与运维凭据；verification: 进程环境只含自身凭据。
## 3. 回归与文档

- [x] 3.1 新增 `scripts/regress/rocketmq-acl.sh`：无凭据/错误凭据拒绝、workflow 可发布、notify 可消费、notify 不可发布、跨 topic 拒绝；verification: 本地全栈运行全部通过。
- [x] 3.2 接入 `regress.sh`；verification: 全量回归出现该步骤且通过。
- [x] 3.3 更新 `docs/27` §19、`docs/08`、`docs/23`、`docker/README.md`、`ROADMAP.md`；verification: 文档不再写“无 ACL”。
- [x] 3.4 运行本地全量回归；verification: `scripts/regress/regress.sh` 通过（仅保留 Docker 备份禁用这一既有边界）。

## Workflow follow-up

- 归档 OpenSpec 变更并同步 `transport-security` 主规格。
- 触发 GitHub Actions `full-regress` 全栈回归并确认通过。
- 新建 `feat/enable-rocketmq-acl` 分支，提交、推送、创建 PR 并合并到 `master`。
