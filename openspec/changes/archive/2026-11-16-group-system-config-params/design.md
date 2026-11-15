# Design

## Context

动机见 `proposal.md`。影响实现方式的现状与约束：

- `sys_config` 当前有 53 条未删除配置（含 V11 新增的 4 条治理控制台配置），表结构来自 `V1__baseline.sql`：`id`、`config_name`、`config_key`、`config_value`、`config_type`、审计字段、`deleted`、`remark`，没有分组字段。
- 后端按分层组织：`Config` 领域对象、`ConfigPO`、`ConfigMapper.xml`、`ConfigRepository/ConfigRepositoryImpl`、`ConfigApplicationService`、`ConfigController`、请求/响应/导出 DTO。相关类没有使用 Lombok，构造器、builder、getter/setter、`equals/hashCode/toString` 都是显式代码。
- 前端 `ConfigView.vue` 是平铺分页表，查询参数为 `configName`、`configKey`、`configType`；字典通过 `dictStore.loadDicts` 加载。
- 字典体系由 `sys_dict_type` 与 `sys_dict_data` 承载；`DictApplicationService.getDictLabel(dictType, dictValue)` 可用于导出时把编码转成显示名，找不到时返回空字符串。
- 当前最新迁移是 `V10`。若按“控制台变更先实施”的顺序，本变更使用 `V12`；若本变更先实施，则使用 `V11`，并让控制台变更顺延为 `V12`。
- `ConfigApplicationService` 按配置键缓存配置值。分组是元数据，不得改变缓存键、缓存失效或读取语义。

## Goals / Non-Goals

**Goals:**

- 为每条配置增加稳定、可查询的分组编码。
- 通过迁移把 49 条存量配置回填到 5 个业务分组，未匹配的落到 `other`。
- 分组标签与顺序由字典维护，不新增分组表或分组 CRUD 页面。
- 参数配置页面先选分组再查看；支持分组筛选、分页、导出与新增/编辑。
- 接口保持向后兼容。

**Non-Goals:**

- 不做树形分组、分组层级或分组权限。
- 不改变 `configKey` 唯一性、配置值、缓存键与失效策略。
- 不改变系统内置配置的删除保护。
- 不新增分组管理菜单或页面。

## Decisions

### 1. 扁平分组编码 + 字典，而不是分组表

`sys_config` 增加 `config_group varchar(50) NOT NULL DEFAULT 'other'`，分组编码由字典 `sys_config_group` 提供标签与顺序。当前只有 49 条配置，一级分组足够；字典已经具备标签、排序和启用状态，不需要为分组再做一张表、CRUD 页面、角色菜单和缓存。

考虑过的替代方案：

- 树形 `sys_config_group` 表：支持多级，但需要分组 CRUD、排序、权限和与 `sys_config` 的外键，成本远高于收益。
- 从 `config_key` 前缀实时推导：零迁移，但分组不可配置、新增键容易漂移，导出和接口也拿不到稳定属性。

### 2. 分组编码使用语言无关的稳定值

编码固定为 `basic`、`message`、`storage`、`monitor`、`console`、`other`，显示名放在字典：

| 编码 | 字典标签 | 排序 |
|---|---|---|
| `basic` | 基础与账号安全 | 1 |
| `message` | 公告与消息 | 2 |
| `storage` | 文件存储 | 3 |
| `monitor` | 监控与告警 | 4 |
| `console` | 控制台入口 | 5 |
| `other` | 未分组 | 6 |

`console` 分组覆盖 `sys.*.monitor.url`、`sys.swagger.*`、`sys.druid.monitor.url`、`monitor.sba.*`，以及 V11 新增的 `sys.nacos.console.*` 与 `sys.sentinel.dashboard.*`。不在数据库里存中文标签，避免以后做多语言或改名时批量改数据。

### 3. 列定义与索引

```sql
ALTER TABLE sys_config
  ADD COLUMN config_group varchar(50) NOT NULL DEFAULT 'other' AFTER config_type;

CREATE INDEX idx_config_group ON sys_config (config_group);
```

非空默认值让存量行在 `ALTER` 后自动落到 `other`，新增配置不选分组时也不会失败。索引用于分组筛选与计数。

### 4. 回填顺序与匹配规则

回填按“先具体、后宽泛”的顺序执行，避免 `monitor.sba.*` 被 `monitor.%` 覆盖：

```sql
-- 监控与告警（排除 sba，sba 属于控制台入口）
UPDATE sys_config SET config_group = 'monitor'
WHERE config_key LIKE 'monitor.%'
  AND config_key NOT LIKE 'monitor.sba.%';

-- 控制台入口
UPDATE sys_config SET config_group = 'console'
WHERE config_key LIKE 'sys.%.monitor.url'
   OR config_key = 'sys.thanos.query.url'
   OR config_key LIKE 'sys.swagger.%'
   OR config_key = 'sys.druid.monitor.url'
   OR config_key LIKE 'monitor.sba.%';

-- 文件存储
UPDATE sys_config SET config_group = 'storage'
WHERE config_key LIKE 'sys.file.%'
   OR config_key LIKE 'sys.silo.%';

-- 公告与消息
UPDATE sys_config SET config_group = 'message'
WHERE config_key LIKE 'sys.notice.%'
   OR config_key LIKE 'sys.message.%';

-- 基础与账号安全
UPDATE sys_config SET config_group = 'basic'
WHERE config_key = 'sys.name'
   OR config_key LIKE 'sys.user.%'
   OR config_key LIKE 'sys.account.%'
   OR config_key LIKE 'sys.session.%'
   OR config_key LIKE 'sys.login.%';

-- 监控刷新间隔不在 monitor.* 前缀下，单独归入监控
UPDATE sys_config SET config_group = 'monitor'
WHERE config_key = 'sys.online.refreshInterval';
```

按当前库验证的预期分布：

| 分组 | 数量 |
|---|---|
| `basic` | 8 |
| `message` | 4 |
| `storage` | 13 |
| `monitor` | 13 |
| `console` | 15 |
| `other` | 0 |
| 合计 | 53 |

迁移不修改 `config_key` 与 `config_value`；未匹配的键保持列默认值 `other`。

### 5. 接口保持增量兼容

- `GET /api/config/list` 增加可选 `configGroup` 参数；不传时行为不变。
- `GET /api/config/export` 增加可选 `configGroup` 参数，导出使用同一筛选条件。
- `ConfigResponse` 增加 `configGroup`；`ConfigRequest` 增加可选 `configGroup`；`ConfigExportVO` 增加 `configGroup` 与 `configGroupName`。
- `ConfigApplicationService` 的分页、计数、导出查询增加 `configGroup` 参数；不引入新的后端查询对象，保持与现有位置参数风格一致。

考虑过的替代方案：新增后端 `ConfigQuery` 对象统一承载筛选条件。更整洁，但会牵动所有调用方，超出本次“加一个属性”的范围；后续筛选条件继续增加时再单独重构。

### 6. 分组计数由应用服务聚合，不新增 Mapper 查询

新增 `GET /api/config/group-stats`，返回各分组的配置数量。实现上调用现有 `configRepository.findAll()`（49 条），在 `ConfigApplicationService` 内按 `configGroup` 分组计数；不新增 `GROUP BY` Mapper，也不把配置值传给前端。

这样做的原因是当前数据量很小，避免为一个计数查询新增 DTO、Mapper 方法和 SQL；如果配置表增长到数千条，再把聚合下沉到 SQL。

前端把字典中的 6 个分组与统计结果合并，缺失的分组显示 0；“全部”的数量为各分组之和。

### 7. 导出分组显示名

`ConfigController` 注入 `DictApplicationService`，导出时用 `getDictLabel("sys_config_group", config.getConfigGroup())` 填充 `configGroupName`，标签为空时回退到编码。这样导出文件对运维可读，且分组改名后不需要改代码。

考虑过的替代方案：导出编码。实现更简单，但运维需要对照字典，不如显示名直接。

### 8. 页面采用筛选栏分组下拉 + 原有列表

`ConfigView.vue` 保持现有单列布局，在 `FilterBar` 中“配置类型”与“搜索/重置”之间增加“参数分组”下拉：

```text
+--------------------------------------------------------------------------+
| 参数配置                                                                  |
| [配置名称] [配置键] [配置类型] [参数分组 v] [搜索] [重置]                 |
| [新增] [导出] [刷新缓存]                                                  |
+--------------------------------------------------------------------------+
| el-table（增加“分组”列）                                                  |
| 分页                                                                      |
+--------------------------------------------------------------------------+
```

- 下拉选项来自 `sys_config_group` 字典，标签带数量（如“文件存储 (13)”）；清空表示“全部”。
- 选择分组后把 `pageNum` 重置为 1，再加载。
- 表格的“分组”列用 `dictStore.getDictLabel("sys_config_group", row.configGroup)` 显示。
- 新增/编辑表单增加分组下拉，默认 `other`；提交时始终带上 `configGroup`。
- 导出时把当前 `queryParams.configGroup` 传给 `/config/export`。
- 窄屏下筛选栏自动换行，表格保持可滚动，不出现页面级水平溢出。

### 9. 新增与更新的分组语义

- 新增：`configGroup` 为空时由 `ConfigApplicationService.createConfig` 归一为 `other`，不拒绝保存。
- 更新：`configGroup` 为空时 Mapper 的 `<if>` 条件不更新该列，保持原值；前端表单始终发送值，正常路径不会为空。
- 删除、系统内置保护、缓存刷新等现有逻辑不变。

### 10. 迁移编号

本变更使用 `V12__add_config_group.sql`。如果实施顺序调整为先做本变更，则改用 `V11`，控制台变更顺延为 `V12`；两个变更不得使用同一个版本号。

## Risks / Trade-offs

- [回填规则漏掉某个配置键] → 未匹配的键落到 `other`，不会丢失；迁移后用 `SELECT config_group, COUNT(*) ... GROUP BY config_group` 核对预期分布，发现漏项再补一条迁移或用参数配置调整。
- [导出依赖字典服务，字典缺失时显示空白] → 标签为空时回退到分组编码；导出功能不因字典缺失失败。
- [新增字段牵动大量显式样板代码] → `Config`、`ConfigPO` 与 DTO 都是手写 boilerplate，需要同步更新构造器、builder、getter/setter、`equals/hashCode/toString`；任务中单独列出，避免只改一半导致比较或序列化遗漏。
- [分组计数在应用层加载全部配置] → 当前 49 条可接受；若配置表显著增长，改为 SQL 聚合，接口保持不变。
- [筛选栏在窄屏下拥挤] → `FilterBar` 自动换行，表格保持可滚动；在 768px 宽度下验收。
- [两个变更的 Flyway 版本号冲突] → 按约定分配 `V11`/`V12`，实施前检查 `db/migration` 最新版本。
- [旧客户端不传 `configGroup`] → 参数可选、响应字段增量，旧客户端行为不变。
- [存量自定义配置没有明确分组] → 默认 `other`，管理员可在页面上重新分组，不需要改代码。

## Migration Plan

1. 新增 `V12__add_config_group.sql`：加列、加索引、按上述顺序回填、插入 `sys_config_group` 字典类型与 6 个字典项。
2. 执行 `iboot-db-init`，用分组计数 SQL 核对 `basic=8、message=4、storage=13、monitor=13、console=15、other=0`。
3. 部署后端：先发布带 `configGroup` 的接口与 DTO，再发布前端；旧前端在过渡期仍可工作。
4. 前端验收：分组下拉清空时显示全部、选择分组重置分页、分组列显示字典标签、新增/编辑分组、按分组导出、窄屏布局。
5. 回滚：回退后端与前端版本；`config_group` 是增量列，旧版本会忽略它，不需要立即删列。若要彻底回滚，在确认没有消费方后新增迁移删除索引与列。
6. 新配置：未选择分组时落到 `other`，由管理员在参数配置页面调整。
