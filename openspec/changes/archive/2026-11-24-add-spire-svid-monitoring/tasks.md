# Tasks

- [x] 1. blackbox `http_2xx` + Prometheus `spire-health` job；SPIRE 容器加入 `obs` 网络；verification: 探针 `probe_success=1`。
- [x] 2. node-exporter 文本文件采集 + `svid-alerts` 规则；verification: `promtool check rules` 通过且指标可见。
- [x] 3. Alertmanager `team="security"` 路由与 receiver；verification: `amtool check-config` 通过，状态含 security 路由。
- [x] 4. 新增 `scripts/regress/spire-svid-monitor.sh` 并纳入 `regress.sh` 7a；verification: 规则、探针、指标、故障注入与路由断言通过。
- [x] 5. 更新 `docs/11` §11.16、`docs/27` §28、`docs/28`、`scripts/README.md`；verification: 文档与配置一致。
- [x] 6. 运行本地全量回归；verification: `scripts/regress/regress.sh` 通过。
- [x] 7. 归档 OpenSpec 变更并同步 `observability-security` 主规格。
- [x] 8. 新建分支，提交、推送、创建 PR；本地与 GitHub 回归通过后合并到 `master`。
