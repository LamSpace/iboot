# Spec Delta

## ADDED Requirements

### Requirement: SVID 刷新周期调度

环境 SHALL 在 SVID 到期前自动刷新，并允许运维巡检调度状态；刷新失败 MUST 留下可观测证据。

- 检查 MUST 覆盖十个服务的 SVID，任一缺失或剩余有效期低于阈值时判定需要刷新。
- 调度 MUST 默认每日执行一次，仅在需要时刷新，MUST NOT 无条件重启服务。
- 宿主机目标 MUST 使用 `spire-svid-sync.sh --refresh`；宿主机服务未运行时 MAY 只刷新文件。
- Compose SVID 栈 MUST 委托 `docker-svid-refresh.sh`，MUST NOT 回退到 mkcert 证书。
- 刷新结果 MUST 写入日志，并暴露最近成功时间、退出码与最小剩余有效期指标。
- 安装、卸载与状态查询 MUST 支持 systemd user timer 与 cron，且重复安装幂等。

#### Scenario: SVID 仍在有效期内

- **WHEN** 调度执行检查且所有 SVID 剩余有效期高于阈值
- **THEN** 不调用刷新脚本
- **AND** 日志记录检查通过，指标退出码为 0

#### Scenario: SVID 即将到期

- **WHEN** 任一 SVID 缺失或剩余有效期低于阈值
- **THEN** 调度按目标调用对应刷新入口
- **AND** 刷新后重新检查，成功才更新最近成功时间

#### Scenario: Compose SVID 栈自动识别

- **WHEN** 存在 `docker-svid-*` sidecar 且未显式指定目标
- **THEN** 调度选择 Compose 目标
- **AND** 通过 `docker-svid-refresh.sh` 刷新并重启，服务继续加载 SVID

#### Scenario: 刷新失败可观测

- **WHEN** 检查或刷新失败
- **THEN** 脚本以非零退出码结束并写入日志
- **AND** 指标退出码为 1，供 Prometheus 告警
