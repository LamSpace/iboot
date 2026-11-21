# Tasks

## 1. Neo4j 凭据收口

- [x] 1.1 把编排中的 `NEO4J_AUTH` 改为 `NEO4J_PASSWORD` 非空守卫，并由该变量派生 `IBOOT_AI_GRAPH_PASSWORD`；验证：未设置变量时 `docker compose config` 报错，设置后通过。
- [x] 1.2 更新 `.env.example` 与本地 `.env`，移除 `iboot123456` 可用默认值，保留明显的本地占位值；验证：`grep -R "iboot123456" .env.example docker/` 无命中。
- [x] 1.3 更新宿主机开发启动逻辑，使 graph profile 与 AI 服务使用同一组受管口令；验证：启用 graph profile 后 AI 服务以 `bolt+s://` 连接成功，错误口令连接失败。
- [x] 1.4 在 `scripts/regress/middleware-auth.sh` 增加 Neo4j 项：旧默认口令被拒绝、受管口令可连接、TLS 校验生效；验证：脚本在 graph profile 下全绿，缺少 graph profile 时明确跳过。
- [x] 1.5 在 `docs/27` 与 `docker/README.md` 登记凭据来源、已有数据卷的改口令/重建路径与社区版单账号边界；验证：文档命令与实际脚本一致。

## 2. Elasticsearch 最小权限账号

- [x] 2.1 新增 `scripts/es-users-init.sh`，幂等创建 `iboot_ai`、`iboot_notify`、`iboot_monitor`、`iboot_logstash`、`iboot_grafana`、`iboot_exporter` 及对应角色；验证：连续执行两次成功，`_security/user` 中六个账号存在。
- [x] 2.2 编排新增 `es-users-init` 一次性服务，在 ES 健康后、依赖 ES 的业务服务之前完成；验证：`docker compose up` 时服务等待该任务成功退出，失败时下游不启动。
- [x] 2.3 按服务注入 ES 账号：`x-app-env`、共享 `iboot-tls.yml`、`logstash.conf`、Grafana 数据源与 elasticsearch-exporter；验证：`docker inspect` 中业务容器无 `elastic` 口令，Logstash/Grafana/exporter 各用自己的账号。
- [x] 2.4 更新 `.env.example`、本地 `.env` 与 CI 工作流，补齐六个账号口令；验证：`docker compose config` 与 CI 启动步骤均无缺失变量。
- [x] 2.5 扩展 `scripts/regress/middleware-auth.sh`：各账号只读/只写边界、AI 与通知索引写入、`elastic` 在业务容器中不可见；验证：越权请求返回 403，正常读写成功。
- [x] 2.6 在 `docs/27`、`docs/08`、`docs/11` 与 `docker/README.md` 更新 ES 账号矩阵与凭据来源；验证：账号表与脚本实际创建的角色一致。

## 3. Grafana 控制台

- [x] 3.1 把 `GF_AUTH_ANONYMOUS_ORG_ROLE` 改为 `Viewer`，管理员口令改为 `GRAFANA_ADMIN_PASSWORD` 非空守卫，并在 `.env.example`/本地 `.env`/CI 中登记；验证：缺少变量时编排失败，匿名访问 `/api/admin/users` 返回未授权。
- [x] 3.2 确认 `docker-compose.prod.yml` 继承管理员口令守卫且不恢复匿名 Admin；验证：`docker compose -f ... -f ... config` 中匿名角色仍为 Viewer。
- [x] 3.3 在门户中验证内嵌看板与 Tempo Explore 的匿名只读可用性；验证：未登录浏览器可打开看板与追踪页，管理入口不可用。
- [x] 3.4 更新 `docs/27`、`docs/11` 与 `docker/README.md` 的控制台账号说明；验证：文档描述与 compose 实际值一致。

## 4. 证书过期告警

- [x] 4.1 新增 `blackbox-exporter` 服务与探针配置，挂载本地 CA，加入 `app`/`obs` 网络且不发布宿主端口；验证：`curl` 探针接口返回 `probe_ssl_earliest_cert_expiry` 且 `probe_success 1`。
- [x] 4.2 在宿主机模式与 Docker 模式的 Prometheus 配置中加入证书 job；验证：Prometheus targets 中该 job 为 UP，指标可查询。
- [x] 4.3 新增 `alerts/tls-alerts.yml`，包含 30 天 warning、7 天 critical 与探针失败告警；验证：`promtool check rules` 通过，临时把阈值改到未来时间可观察到告警触发，随后还原。
- [x] 4.4 在 `scripts/regress/` 增加证书探针检查并接入 `regress.sh`；验证：探针正常时通过，停止探针后回归失败并指出探针不可达。
- [x] 4.5 在 `docs/27` 与 `docs/11` 记录告警阈值、探针目标与 B5 拆分证书后的扩展要求；验证：文档中的查询语句能在 Prometheus 返回数据。

## 5. 明文默认地址审计

- [x] 5.1 把 `IBOOT_AI_SILO_ENDPOINT` 默认值改为 `https://localhost:9000`；验证：宿主机模式下 AI 服务访问 Silo 不再因明文默认值失败。
- [x] 5.2 新增 `scripts/regress/plaintext-audit.sh` 与 `plaintext-allowlist.txt`，扫描应用配置与 Grafana 数据源；验证：人为加入一条 `http://` 配置时脚本失败并指出文件与键，恢复后通过。
- [x] 5.3 把审计接入 `scripts/regress/regress.sh`，并在允许清单中登记 Tempo OTLP、Grafana Tempo 数据源与 CLI 回环回调；验证：全量回归输出列出例外且不阻塞通过。
- [x] 5.4 在 `docs/27` 登记允许清单的含义、当前例外与“只允许缩小”的约定；验证：文档条目与 `plaintext-allowlist.txt` 一一对应。

## 6. 集成验收

- [x] 6.1 宿主机模式执行完整回归，覆盖账号、TLS、证书、明文审计与既有业务用例；验证：`scripts/regress/regress.sh` 全部通过。
- [x] 6.2 Docker 全量部署执行完整回归；验证：容器内账号、ES 写读、Grafana 只读、证书探针全部通过。
- [x] 6.3 更新 `docs/27` 的 B4.4 小节与后续批次关系，说明 Tempo 明文链路仍是已登记边界；验证：文档章节与 proposal/design 的 Non-Goals 一致。
- [ ] 6.4 更新 `.github/workflows/regress.yml` 的新增凭据与服务启动等待；验证：PR 的 CI 回归通过。

## Workflow follow-up

- 同步规格并归档变更。
- 提交功能分支、创建 PR、通过 CI 后合并到 `master`。
