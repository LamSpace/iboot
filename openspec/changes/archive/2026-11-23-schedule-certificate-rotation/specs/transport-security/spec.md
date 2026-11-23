# transport-security 增量规格

## ADDED Requirements

### Requirement: 证书轮换可周期调度

证书检查与重签 SHALL 有一个可安装的周期调度入口，避免环境依赖人工记忆证书到期时间。

- 周期调度 MUST 由一个幂等安装命令创建，优先使用 systemd user timer，无法使用时回退到当前用户 crontab。
- 调度任务 MUST 先执行检查；检查通过时 MUST NOT 修改证书或重启服务，检查失败时才重签，并按安装时选择的重启范围执行重启。
- 安装 MUST NOT 覆盖或删除非本项目的定时任务；卸载 MUST 只移除本项目创建的单元与标记块。
- 调度任务 MUST 记录每次运行的时间、检查结果与重签结果，并使用锁避免并发重签。
- `--print` MUST 只输出将安装的调度内容，不写入系统或修改证书。
- 调度 MUST NOT 改变“普通启动不重签”的既有语义。

#### Scenario: 安装周期调度

- **WHEN** 执行调度安装命令并指定重启范围
- **THEN** 安装一个每日运行的 systemd user timer 或 crontab 标记块
- **AND** 重复执行安装不产生重复任务
- **AND** `--status` 报告当前调度器与下次运行时间

#### Scenario: 证书有效时调度任务不重签

- **WHEN** 周期任务运行时证书剩余有效期不少于 30 天
- **THEN** 任务记录检查通过
- **AND** 不重签证书、不重启任何服务

#### Scenario: 证书即将过期时自动重签

- **WHEN** 周期任务运行时检查失败且证书缺失或剩余有效期不足
- **THEN** 任务执行显式重签
- **AND** 按安装时选择的重启范围重启运行时
- **AND** 日志包含重签与重启结果

#### Scenario: 卸载只移除本项目调度

- **WHEN** 执行卸载命令
- **THEN** 只删除本项目创建的 systemd 单元或 crontab 标记块
- **AND** 其它定时任务保持不变

#### Scenario: 无 systemd 时回退 cron

- **WHEN** 当前会话不可用 systemd user manager
- **THEN** 安装命令回退到当前用户 crontab
- **AND** `--status` 明确报告调度器类型为 cron
