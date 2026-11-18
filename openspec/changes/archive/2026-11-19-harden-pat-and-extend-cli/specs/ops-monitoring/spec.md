# Spec Delta

## ADDED Requirements

### Requirement: 操作日志返回认证方式

操作日志的列表、详情与导出 SHALL 返回认证方式（`SESSION` / `PAT`），取值 MUST 与写入方记录一致。该字段 MUST NOT 改变既有筛选、分页总数或数据范围语义；字段缺失的旧记录 MUST 返回 `SESSION`。

#### Scenario: 列表返回认证方式

- **WHEN** 管理员查询操作日志列表
- **THEN** 每条记录包含认证方式
- **AND** 分页总数与本次变更前同一条件一致

#### Scenario: 详情返回认证方式

- **WHEN** 管理员打开一条由 PAT 产生的操作日志详情
- **THEN** 详情中的认证方式为 `PAT`

#### Scenario: 导出包含认证方式

- **WHEN** 管理员按条件导出操作日志
- **THEN** 每一行包含认证方式
- **AND** 导出范围与列表使用相同筛选条件

#### Scenario: 旧记录返回默认值

- **WHEN** 查询缺少认证方式字段的旧记录
- **THEN** 响应中的认证方式为 `SESSION`
- **AND** 不出现空值或查询错误
