# Spec Delta

## MODIFIED Requirements

### Requirement: 在线会话对管理员可见可控

管理员 SHALL 能查看当前在线会话并对指定用户强制下线。

- 在线列表 MUST 反映当前有效的会话，而不只是登录历史。
- 强制下线 MUST 立即生效于该用户的全部令牌。
- 在线列表与强制下线的判定结果 MUST 与拆分前一致。
- **过渡期例外**：`client_id=iboot-cli` 的会话 MUST NOT 写入 `online_token:<userId>`，因此 MUST NOT 出现在在线列表，MUST NOT 被按用户的强制下线覆盖；此例外 MUST 在 `add-multi-client-sessions` 落地后移除。

#### Scenario: 在线列表可见

- **WHEN** 管理员打开在线用户页面
- **THEN** 能看到当前在线用户及其登录时间、IP 与浏览器信息

#### Scenario: 强制下线

- **WHEN** 管理员对某在线用户执行强制下线
- **THEN** 该用户在该列表中的记录消失
- **AND** 该用户的后续请求被拒绝

#### Scenario: CLI 会话不参与在线名单（过渡期）

- **WHEN** 用户通过 `iboot-cli` 完成授权码登录
- **THEN** 该会话不出现在在线列表
- **AND** 管理员对该用户执行强制下线时，该 CLI 会话不被撤销
- **AND** 该例外在 `add-multi-client-sessions` 落地后失效
