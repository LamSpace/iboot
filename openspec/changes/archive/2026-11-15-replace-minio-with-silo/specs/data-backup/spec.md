# Spec Delta

## MODIFIED Requirements

### Requirement: 备份产物复用现有存储能力

备份对象 MUST 通过当前生效的文件存储提供方保存，不得硬编码本地磁盘或 Silo。对象键 SHALL 位于 `backup/` 前缀下，并包含数据库名与日期层级。切换 `sys.file.storageType` 后，新的备份 MUST 写入新的存储提供方，无需修改代码。

#### Scenario: MinIO 生效时落 MinIO

- **WHEN** `sys.file.storageType` 为迁移前的旧值 `minio`
- **THEN** 启动被拒绝并提示把该值改为 `silo`
- **AND** 不会写入任何备份对象

#### Scenario: Silo 生效时落 Silo

- **WHEN** `sys.file.storageType` 为 `silo` 且备份成功
- **THEN** 备份对象出现在 Silo 的 `backup/` 前缀下
- **AND** 备份记录中的存储类型为 `silo`

#### Scenario: 本地存储生效时落本地

- **WHEN** `sys.file.storageType` 为 `local` 且备份成功
- **THEN** 备份对象出现在本地存储目录的 `backup/` 前缀下
- **AND** 备份记录中的存储类型为 `local`

#### Scenario: 备份对象与普通文件隔离命名

- **WHEN** 系统同时保存用户上传文件与备份对象
- **THEN** 两类对象可通过路径前缀区分
- **AND** 删除普通文件不会影响备份对象
