# Tasks

## 1. 任务执行与日志

- [x] 1.1 实现 `iboot job run <id>` 与 `iboot job logs --job-id <id>`：复用 `BaseCommand.write` 与现有日志接口；验证单元测试覆盖 `--yes`、`--dry-run`、403 退出码，并在本地全栈创建一条暂停任务，执行 `job run` 后 `job logs --job-id` 能查到记录。
- [x] 1.2 更新 `docs/25-开发者 CLI 操作指南.md` 的任务命令与日志过滤说明；验证文档中的命令在本地全栈按原文执行成功。

## 2. 备份触发与等待

- [x] 2.1 实现 `iboot backup trigger [--wait] [--interval] [--timeout]`：触发后读取记录 ID，按间隔轮询记录详情直到 `SUCCESS`/`FAILED`；验证 stub 测试覆盖 RUNNING→SUCCESS、RUNNING→FAILED、超时三种路径，且没有单个长请求。
- [x] 2.2 更新 `docs/25-开发者 CLI 操作指南.md` 的备份命令与等待语义；验证宿主机/CI 开启备份时命令按原文执行并输出终态，Docker 环境“备份未启用”映射为业务错误退出码。

## 3. 在线会话与 context

- [x] 3.1 实现 `iboot online kick <tokenId>`：使用 `online list` 的脱敏 tokenId；验证本地全栈用测试用户会话强制下线后该令牌请求 401，无 `online:forceLogout` 权限的用户退出码为 4。
- [x] 3.2 实现 `iboot config add/remove`：支持 `--base-url`、`--issuer`、`--client-id`、`--redirect-uri`、`--use`；删除当前 context 拒绝，删除后清理该 context 的本地凭据；验证单元测试覆盖新增、切换、删除非当前、拒绝删除当前与凭据清理。
- [x] 3.3 更新 `docs/25-开发者 CLI 操作指南.md` 的在线会话与 context 命令；验证文档命令在临时 `IBOOT_CONFIG_DIR` 下按原文执行成功。

## 4. 输出、审计与错误映射

- [x] 4.1 验证新写命令的 `--json` 输出业务数据、`--dry-run` 不产生请求；验证单元测试与全栈执行 `--dry-run` 后操作日志无新增。
- [x] 4.2 验证写命令审计：`sys_operate_log` 能检索到任务执行、备份触发与在线强退记录，操作人与方法正确；验证用 `iboot log operate --json` 或 `reg_get /api/log/operate/list` 查询。
- [x] 4.3 验证错误映射：备份禁用返回业务错误、等待超时返回业务错误、无权限返回 403、会话失效返回认证错误；验证每条路径的退出码与提示不含令牌。

## 5. 回归与文档收口

- [x] 5.1 扩展 `scripts/regress/cli.sh` 覆盖新写命令、403、`--dry-run`、备份等待/禁用与 context 增删；验证 Docker（`REG_OIDC_REDIRECT=http://localhost/callback`）与宿主机/CI 环境均通过。
- [x] 5.2 更新 `README.md` 与 `ROADMAP.md` 的开发者 CLI 能力和 v2 判据；验证文档描述与 `openspec/specs/developer-cli/spec.md` 不矛盾。
- [x] 5.3 运行 `openspec validate extend-developer-cli-writes --strict` 与 `mvn -s /home/lam/repo/settings.xml -pl iboot-cli test`；验证 spec/design/tasks 无校验错误、单元测试全部通过。

## Workflow follow-up

- 实现完成后按项目约定走 `feat/cli-writes` 功能分支、PR 与 `gh pr merge`，不要直接提交到 `master`。
- 归档前确认 CLI 指南、README、ROADMAP 与主 spec 表述一致。
