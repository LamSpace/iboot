# Tasks

## 1. 证书与 exporter TLS

- [x] 1.1 `tls-init.sh` 增加四个 exporter 服务端证书（serverAuth、SAN 含服务名），并复制到运行期目录；verification: `openspec` 外的 `openssl x509 -text` 可看到 SAN。
- [x] 1.2 新增 exporter web config 并给 mysql/node/es 加 `--web.config.file`，redis 加原生 TLS 参数；verification: 明文请求失败、无客户端证书失败。
- [x] 1.3 Prometheus 两份配置的四个 exporter job 改为 HTTPS + 客户端证书；verification: Prometheus targets 全部 up。

## 2. 轮换脚本

- [x] 2.1 新增 `scripts/tls-rotate.sh` 的 `--check/--rotate/--restart`；verification: `--check` 通过，`--rotate --restart none` 后检查仍通过。
- [x] 2.2 文档登记 cron 示例与重启范围；verification: 文档命令可直接复制执行。

## 3. 回归与文档

- [x] 3.1 新增 `scripts/regress/exporter-mtls.sh`：四个 exporter 的正向、无证书、明文、错误 CA；verification: 本地全栈全部通过。
- [x] 3.2 接入 `regress.sh` 与 `observability-tls.sh` 的汇总；verification: 全量回归出现该步骤。
- [x] 3.3 更新 `docs/27`、`docs/08`、`docker/README.md`、`ROADMAP.md`；verification: 文档登记 exporter mTLS 与轮换入口。
- [x] 3.4 运行本地全量回归；verification: 通过（备份用例按既有边界跳过）。

## Workflow follow-up

- 归档 OpenSpec 变更并同步 `observability-security` 与 `transport-security`。
- 触发 GitHub Actions `full-regress` 并确认通过。
- 新建 `feat/harden-observability-mtls` 分支，提交、推送、创建 PR 并合并到 `master`。
