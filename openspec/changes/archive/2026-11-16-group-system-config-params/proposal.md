# Proposal

## Why

`sys_config` 目前有 50 多项配置，门户“系统管理 > 参数配置”把它们放在一张平铺表格里。`config_type` 只表达“默认/系统内置”，不是业务分类，无法帮助定位；随着微服务接入，配置项还会继续增加。需要给每个参数一个稳定的分组属性，并让页面按分组先导航、再查看和筛选，避免几十行混在一起。

## What Changes

- `sys_config` 新增 `config_group` 字段（`varchar(50)`，非空，默认 `other`，带索引）。
- 新增字典类型 `sys_config_group`，包含 6 个分组：基础与账号安全、公告与消息、文件存储、监控与告警、控制台入口、未分组。
- 通过迁移把现有配置按 `config_key` 前缀回填到前 5 个分组；新增配置未选择时落到 `other`。
- 参数配置页面在筛选栏增加“参数分组”下拉（选项带配置数量，清空表示全部），与配置名称、配置键、配置类型筛选并列；表格增加分组列；新增/编辑表单增加分组下拉；导出包含分组显示名。
- 后端列表与导出接口增加可选的 `configGroup` 参数，保持向后兼容；分页总数与分组筛选一致。
- 不新增分组 CRUD 页面，分组标签与排序由字典维护；不改变配置值、配置键、缓存与读取语义。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `system-management`: 参数配置新增分组属性、字典化分组、按分组筛选/分页/导出与迁移回填要求；配置读取与缓存语义不变。

## Impact

- 数据：`iboot-db-init` 新增迁移（字段、索引、回填、字典类型与字典项）。
- 后端：`iboot-system` 的 Config 领域对象、PO、Mapper XML、仓储、应用服务、Controller、请求/响应/导出 DTO 与 MapStruct 映射。
- 前端：`iboot-portal` 的 `Config`/`ConfigQuery` 类型、`ConfigView.vue`、中英文文案。
- 接口：`/api/config/list` 与 `/api/config/export` 增加可选 `configGroup` 参数；响应增加 `configGroup`，兼容旧客户端。
- 测试：迁移回填校验、分组筛选与总数一致性、导出列、前端 type-check/build。
- 不改变：配置缓存键与失效策略、`configKey` 唯一性、系统内置配置的删除保护。
