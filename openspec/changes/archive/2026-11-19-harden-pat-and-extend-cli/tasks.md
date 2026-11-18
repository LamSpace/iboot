# Tasks

## 1. Schema 与 PAT scope

- [x] 1.1 新增 `V14__personal_access_token_scopes.sql` 为 `sys_user_pat` 增加 `scopes`（可空，逗号分隔），并在 `SchemaMigratorTest` 增加列存在性断言，验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-db-init package` 通过
- [x] 1.2 扩展 PAT 请求 DTO、`PersonalAccessTokenService`、`PersonalAccessTokenRepository`、`PersonalAccessTokenRecord`/`Summary` 与响应：支持 `scopes`、校验用户权限子集、去重与数量上限、写入元数据与 JWT 声明，验证服务测试覆盖“有效 scope”“非子集拒绝”“空 scope 继承全部”“去重与上限”
- [x] 1.3 在 `token list` 响应中返回 scopes，验证控制器测试断言列表包含 scope 且不含明文

## 2. 内核权限与审计写入

- [x] 2.1 在 `JwtAuthenticationFilter` 的 PAT 分支中按 `scopes` 与 `permissions` 取交集，验证新增测试覆盖“声明超出 scope 时只授予交集”和“无 scopes 的旧令牌行为不变”
- [x] 2.2 为 `OperateLog`、`OperateLogRecorder` 与 `LogAspect` 增加 `authType`：PAT 主体写 `PAT`，其余写 `SESSION`，验证内核测试覆盖两种主体与默认值

## 3. 审计查询与门户

- [x] 3.1 新增 `V15__operate_log_auth_type.sql` 为 `sys_operate_log` 增加 `auth_type`（默认 `SESSION`），验证 `SchemaMigratorTest` 的列断言通过
- [x] 3.2 更新监控服务 `OperateLogPO`、Mapper XML、仓储转换、`OperateLogResponse`、导出 VO 与控制器，使列表/详情/导出返回 `authType`，验证监控日志测试覆盖字段透传与旧记录默认值
- [x] 3.3 更新系统域仪表盘 `OperateLogPO`、Mapper 结果映射与 `OperateLogResponse`，验证仪表盘最近操作日志测试通过且不改变原有分页与数据范围
- [x] 3.4 在门户 `OperateLogView.vue` 增加认证方式列与中英文文案，验证 `cnpm run build` 通过

## 4. CLI 翻页、任务等待与展示

- [x] 4.1 在 `PageOptions` 增加 `--all`，在 `BaseCommand` 增加分页合并逻辑（固定 100/页、10,000 上限、保持 `total`+`data` 结构），验证单元测试覆盖多页合并、上限拒绝与默认分页不变
- [x] 4.2 为 `job run` 增加 `--wait`、`--interval`、`--timeout`：触发前记录该任务最大日志 ID，触发后轮询新日志的终态，验证单元测试覆盖成功、失败、超时与“不误用旧日志”
- [x] 4.3 为 `token create` 增加可重复 `--scope`，并在 `token list` 与 `log operate` 中展示 scope 与 `authType`，验证命令测试覆盖参数传递与输出字段
- [x] 4.4 更新 `docs/25`、`docs/26` 与 `.claude/skills/iboot-cli/SKILL.md`，说明 scope 示例、`--all` 上限、`job run --wait` 与审计字段，验证文档中的命令可被 CLI 帮助实际接受

## 5. 回归与集成

- [x] 5.1 在 `scripts/regress/cli.sh` 增加用例：受限 scope 的 PAT 可执行允许接口、越界接口返回 403、审计显示 `PAT`、`--all` 合并、`job run --wait` 成功/失败，验证脚本在 Docker 全栈上通过
- [x] 5.2 运行 `mvn -s /home/lam/repo/settings.xml -pl iboot-kernel,iboot-auth,iboot-monitor,iboot-system,iboot-cli -am test`，确认全部单元测试通过
- [x] 5.3 重新构建 Docker 镜像并执行 CLI、冒烟与权限矩阵回归，验证 V14/V15 已应用且既有用例未退化

## Workflow follow-up

- 推送功能分支并创建 PR，合并到 `master` 后更新本地分支。
- 归档变更并把 delta 同步到主 spec。
