# Spec Delta

## ADDED Requirements

### Requirement: 审计记录区分认证方式

操作审计记录 SHALL 包含认证方式字段，至少区分 `SESSION`（交互式登录）与 `PAT`（个人访问令牌）。所有写入方 MUST 在记录中填写该字段；无法判定时 MUST 使用 `SESSION`。字段缺失的旧记录 MUST 按 `SESSION` 解释，且 MUST NOT 使历史查询失败。

#### Scenario: PAT 发起的操作

- **WHEN** 使用 PAT 的请求触发一个带操作日志的写接口
- **THEN** 审计记录中的认证方式为 `PAT`
- **AND** 操作人仍记录 PAT 所属用户

#### Scenario: 交互式会话发起的操作

- **WHEN** 浏览器登录会话触发一个带操作日志的写接口
- **THEN** 审计记录中的认证方式为 `SESSION`

#### Scenario: 旧记录兼容

- **WHEN** 查询本次变更前写入的审计记录
- **THEN** 认证方式按 `SESSION` 返回
- **AND** 查询与导出不因缺少字段而失败

#### Scenario: 认证方式不影响业务

- **WHEN** 认证方式字段写入失败
- **THEN** 业务请求仍然成功
- **AND** 失败按既有审计旁路语义记录
