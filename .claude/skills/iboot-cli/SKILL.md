---
name: iboot-cli
description: Use the iBoot developer CLI (bin/iboot) to inspect or operate a running iBoot environment from the terminal. Use when the user asks to query users, roles, departments, dictionaries, jobs, backups, logs or online sessions, or to trigger a job, backup or session logout through the project's CLI.
allowed-tools: Bash(./bin/iboot:*), Bash(printf:*)
---

# iBoot CLI

`bin/iboot` 是仓库自带的开发者命令行工具，只经网关 `/api/**` 调用既有接口，复用服务端的
功能权限、数据范围与审计。它不是门户的替代品。

## Before running

1. 确认 CLI 已构建：`mvn -s /home/lam/repo/settings.xml -pl iboot-cli package`
2. 确认网关可达：`curl -fsS http://localhost:8088/actuator/health`
3. 交互式环境使用 `./bin/iboot login`；无浏览器环境使用 PAT 从标准输入注入：

```bash
printf '%s' "$IBOOT_PAT" | ./bin/iboot user list --token-stdin
```

令牌来源优先级：`--token-stdin` > `IBOOT_TOKEN` > 本地凭据。

## Safety rules

- 先只读后写。写命令默认拒绝，必须 `--yes`；先用 `--dry-run` 查看方法与路径。
- 不要在命令行参数中传递令牌，不要打印令牌原文，不要把令牌写入仓库文件。
- 不要用 `iboot api` 绕过已封装的权限或写保护；越权由服务端返回 403。
- 退出码：`0` 成功、`2` 用法、`3` 认证失效、`4` 越权、`5` 网络/5xx、`6` 业务、
  `7` 缺少 `--yes`。

## Read-only recipes

```bash
./bin/iboot status
./bin/iboot user list --username admin --json
./bin/iboot role list
./bin/iboot dept tree
./bin/iboot dict types
./bin/iboot param list --json
./bin/iboot job list --json
./bin/iboot job logs --job-id 12
./bin/iboot backup list
./bin/iboot log operate --size 20
./bin/iboot online list --json

# 取全量：固定每页 100，超过 10000 条会拒绝
./bin/iboot user list --all --json
```

`--json` 输出解包后的业务数据，适合继续用 `jq` 处理；默认输出表格供人阅读。

## Write recipes

```bash
./bin/iboot job run 12 --dry-run
./bin/iboot job run 12 --yes
./bin/iboot job run 12 --wait --yes
./bin/iboot backup trigger --wait --yes
./bin/iboot online kick <tokenId> --yes
```

`job run --wait` 只等待本次触发后新产生的执行日志，失败以业务错误退出。

`backup trigger --wait` 轮询到 `SUCCESS` / `FAILED`，默认每 2 秒、最多 300 秒；
Docker 全量部署默认关闭备份，会返回“备份功能未启用”。

## Personal access tokens

`token create/list/revoke` 只能用交互式登录会话（PAT 不能管理 PAT）：

```bash
./bin/iboot token create ci --expires-days 30 --yes
./bin/iboot token create ops --scope job:run --scope backup:record:create --yes
./bin/iboot token list
./bin/iboot token revoke 3 --yes
```

`--scope` 是权限码子集，不指定时继承全部权限。PAT 的权限是签发时快照；角色调整后
撤销并重建。用户停用或删除时全部 PAT 失效。PAT 不出现在在线列表，也不被会话级强退
覆盖。操作日志的 `authType` 区分 `SESSION` 与 `PAT`。

## References

- `docs/25-开发者 CLI 操作指南.md`
- `docs/26-CI 与个人访问令牌.md`
