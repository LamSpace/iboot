# Proposal

## Why

单库合并（`b064a32`，全部服务共用 `iboot` 库并由 `iboot-db-init` 统一迁移）之后，两条测试断言没有同步更新，导致全量 `mvn test` 在 `iboot-file` 与 `iboot-ai` 两个模块失败。它们不影响运行期行为，但会让 `fix/kafka-log-pipeline` 的全量回归无法以「全绿」结案，也会掩盖将来真正的新失败。

## What Changes

- `DataScopeSqlBuilderTest.deptScopeFiltersJoinedUserDept`：期望值从 `iboot_admin.sys_user` 改为未限定的 `sys_user`，与 `DataScopeSqlBuilder` 的单库语义一致。
- `AgentSchemaMigrationTest.repeatedMigrationIsIdempotent`：把「历史表只有 1 条」改为「重复迁移后历史记录的条数与首次迁移后相同」，断言不再依赖具体迁移数量，只验证幂等性本身。

不改动任何生产代码、SQL、配置或对外行为。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无。仅修正测试断言，不改变规格描述的行为。）

## Impact

- 仅两个测试文件：`iboot-file/src/test/java/.../DataScopeSqlBuilderTest.java`、`iboot-ai/src/test/java/.../AgentSchemaMigrationTest.java`。
- 无 API、数据库、前端或部署变更。
