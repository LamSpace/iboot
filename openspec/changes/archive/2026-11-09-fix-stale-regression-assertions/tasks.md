# Tasks

## 1. 修正过期断言

- [x] 1.1 把 `DataScopeSqlBuilderTest.deptScopeFiltersJoinedUserDept` 的期望值改为未限定的 `sys_user`，与单库语义一致；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-file -Dtest=DataScopeSqlBuilderTest test` 通过
- [x] 1.2 把 `AgentSchemaMigrationTest.repeatedMigrationIsIdempotent` 改为比较重复迁移前后的历史记录条数，不再硬编码迁移数量；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ai -Dtest=AgentSchemaMigrationTest test` 通过
- [x] 1.3 跑全量回归确认无新增失败；验证：`mvn -s /home/lam/repo/settings.xml -fae test` 的 Reactor Summary 中没有 FAILURE
