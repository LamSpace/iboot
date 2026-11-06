# Tasks

## 1. CI full-regress

- [x] 1.1 重写 `.github/workflows/regress.yml` 的 `full-regress`：保留 Maven 构建，随后执行 `scripts/dev-init.sh` 与 `scripts/regress/regress.sh`，删除手写的中间件启动、MySQL 等待、db-init、三个服务启动与健康等待；验证：`python3 -c "import yaml; yaml.safe_load(open('.github/workflows/regress.yml'))"` 通过，`grep -n 'dev-init.sh\|regress.sh' .github/workflows/regress.yml` 有命中。
- [x] 1.2 为 `full-regress` 注入 CI 环境变量（AI dummy key、关闭 mail health、关闭 tracing、开启 backup、`IBOOT_OPS_ENABLED=false`、`JAVA_TOOL_OPTIONS=-Xms128m -Xmx384m`），并设置 `timeout-minutes: 45`；验证：`grep -n 'DASHSCOPE_CHAT_API_KEY\|MANAGEMENT_HEALTH_MAIL_ENABLED\|JAVA_TOOL_OPTIONS\|timeout-minutes' .github/workflows/regress.yml` 均有命中。
- [x] 1.3 更新失败日志步骤：输出 `logs/*.log` 与关键容器日志；验证：`grep -n 'logs/\*.log\|docker compose.*logs' .github/workflows/regress.yml` 有命中。
- [x] 1.4 确认 PR 任务仍只跑路由覆盖、构建与网关测试；验证：`grep -n 'route-coverage.sh\|iboot-gateway test' .github/workflows/regress.yml` 有命中，且 PR job 不包含 `dev-init.sh`。

## 2. 文档同步

- [x] 2.1 更新 `docs/23-微服务底座操作指南.md` §1/§2：补充 `path-conservation.sh` 行、把 smoke 依赖改为全栈、用 `dev-init.sh` 作为前置条件、说明备份能力由 `iboot-job` 承担；验证：`grep -n '两个后端服务\|8080/8081' docs/23-微服务底座操作指南.md` 无输出，`grep -n 'path-conservation.sh\|dev-init.sh' docs/23-微服务底座操作指南.md` 有命中。
- [x] 2.2 更新 `scripts/README.md`：增加“回归分层”小节，说明契约快照、路径守恒、路由覆盖、行为回归四层及 PR / 定时任务分工；验证：`grep -n '回归分层\|契约快照\|路径守恒\|路由覆盖\|行为回归' scripts/README.md` 均有命中。

## 3. 验证

- [x] 3.1 静态检查：`bash -n scripts/dev-init.sh`、`bash scripts/regress/route-coverage.sh`、YAML 解析全部通过；验证：三条命令退出码均为 0。
- [x] 3.2 在本地按 CI 环境变量执行一次完整链路：`JAVA_TOOL_OPTIONS='-Xms128m -Xmx384m' DASHSCOPE_CHAT_API_KEY=ci-placeholder DASHSCOPE_EMBEDDING_API_KEY=ci-placeholder IBOOT_OPS_ENABLED=false IBOOT_TRACING_ENABLED=false IBOOT_BACKUP_ENABLED=true IBOOT_BACKUP_ENCRYPTION_KEY=ci-only-key MANAGEMENT_HEALTH_MAIL_ENABLED=false bash scripts/dev-init.sh`，随后执行 `bash scripts/regress/regress.sh`；验证：两条命令退出码为 0，`regress.sh` 输出“回归全部通过”。
- [x] 3.3 验证结束后停止服务：`bash scripts/dev-init.sh --stop`；验证：`pgrep -f 'java -jar iboot-'` 无输出。
