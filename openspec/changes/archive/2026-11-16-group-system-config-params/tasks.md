# Tasks

## 1. 数据迁移

- [x] 1.1 新增 `V12__add_config_group.sql`：为 `sys_config` 加 `config_group` 列与索引；按设计顺序回填；插入 `sys_config_group` 字典类型与 6 个字典项；验证：在 Testcontainers MySQL 或本地库执行 Flyway，`SELECT config_group, COUNT(*) FROM sys_config WHERE deleted = 0 GROUP BY config_group` 得到 `basic=8、message=4、storage=13、monitor=13、console=15、other=0`（共 53 条，含 V11 的 4 条治理控制台配置），且 `config_key`/`config_value` 与迁移前一致。
- [x] 1.2 在 `iboot-db-init` 的迁移测试中增加断言：`sys_config` 有 `config_group` 列、字典类型 `sys_config_group` 存在且包含 6 项、已知键落在预期分组；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-db-init test` 通过。
- [x] 1.3 更新 `docs/07-数据库设计.md` 的 `sys_config` 说明，加入 `config_group` 与 `sys_config_group` 字典；验证：文档中的列名、默认值与迁移一致。

## 2. 后端

- [x] 2.1 在 `Config`、`ConfigPO` 中增加 `configGroup` 字段，并同步构造器、builder、getter/setter、`equals/hashCode/toString`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-system -am compile` 通过。
- [x] 2.2 在 `ConfigRequest`、`ConfigResponse`、`ConfigExportVO` 中增加分组字段（导出 VO 另加 `configGroupName`），同步样板方法；验证：编译通过，序列化/反序列化测试覆盖新字段。
- [x] 2.3 更新 `ConfigMapper.java`、`ConfigMapper.xml`、`ConfigRepository`、`ConfigRepositoryImpl`：resultMap、insert、update、分页/计数/导出查询支持 `configGroup`；验证：Mapper/仓储测试覆盖按分组筛选与计数一致。
- [x] 2.4 更新 `ConfigApplicationService`：分页、计数、导出查询增加 `configGroup`；新增配置空分组归一为 `other`；新增 `getGroupCounts()` 基于 `findAll()` 聚合；验证：单元测试覆盖默认分组、空分组更新不覆盖原值、计数结果。
- [x] 2.5 更新 `ConfigController`：list/export 增加可选 `configGroup`；新增 `GET /api/config/group-stats`；导出时用 `DictApplicationService.getDictLabel` 填充 `configGroupName`，空标签回退编码；验证：MockMvc/集成测试覆盖接口参数、权限、分组统计与导出列。
- [x] 2.6 补充后端测试：分组筛选后的总数与列表一致、无 `configGroup` 的旧请求行为不变、新增默认 `other`、更新不传分组保持原值；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-system test` 通过。
- [x] 2.7 更新 `docs/10-后端配置详解.md` 或参数配置相关文档，说明分组属性、字典与接口可选参数；验证：文档中的接口参数与实际 Controller 一致。

## 3. 前端

- [x] 3.1 更新 `iboot-portal/src/api/system.ts`：`Config` 与 `ConfigQuery` 增加 `configGroup`，新增 `getConfigGroupStats()`；验证：`cnpm run type-check` 通过。
- [x] 3.2 改造 `ConfigView.vue`：筛选栏增加分组下拉（字典分组 + 计数，清空表示全部）、表格分组列、表单分组下拉、查询参数与导出参数、选择分组重置分页；验证：开发环境页面按分组筛选与分页正确。
- [x] 3.3 新增/补齐 `locales/zh-CN/system/config.json` 与英文对应文案（分组、全部、未分组提示等）；验证：中英文切换无缺失 key。
- [x] 3.4 处理窄屏布局：筛选栏在窄屏下自动换行，表格保持可滚动；验证：在 768px、1024px 宽度下无页面级水平溢出。
- [x] 3.5 更新 `docs/04-功能模块.md` 或门户使用文档，说明参数配置的分组筛选；验证：文档描述与页面一致。

## 4. 集成验证

- [x] 4.1 端到端：执行迁移、启动后端与门户，确认默认“全部”、分组计数、切换分组、分页、新增/编辑分组、按分组导出均正常；验证：手工走查或浏览器自动化记录。
- [x] 4.2 兼容性：不传 `configGroup` 调用 `/api/config/list` 与 `/api/config/export`，确认返回与改造前一致且响应包含 `configGroup`；验证：curl 或接口测试。
- [x] 4.3 迁移回滚演练：在测试库应用迁移后回退后端/前端版本，确认旧版本忽略 `config_group` 仍可运行；验证：旧版本列表与配置读取正常。
- [x] 4.4 默认值验证：新增一条不选分组的配置，确认落到 `other` 且“未分组”计数增加；验证：页面与数据库查询一致。

## Workflow follow-up

- Archive the change after review and merge.
- If this change is implemented before `add-microservice-governance-consoles`, swap V11/V12 and update both migration numbers.
