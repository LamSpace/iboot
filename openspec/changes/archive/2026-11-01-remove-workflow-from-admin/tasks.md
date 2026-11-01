# Tasks

## 1. 最终增量校验与搬迁

- [x] 1.1 新增 `scripts/migrate/workflow_final_check.sql`：逐表比较 `iboot_admin` 与 `iboot_workflow` 的行数、`max(id)`，列出旧库中 id 大于新库最大 id 的残留行；验证：演练库执行后输出「无残留」或完整残留清单
- [x] 1.2 对残留行执行按 id 复制，不覆盖 `iboot_workflow` 已有行；同 id 内容冲突时中止并输出差异；验证：演练库中人为插入一条旧库新行，复制后新库可见且既有行未被改写

## 2. 删除旧实现与 Flowable

- [x] 2.1 删除审批相关 20 个 Java 文件（含 6 个 DTO）、8 个测试类、`LeaveApplicationMapper.xml` 与 `processes/leave-approval.bpmn20.xml`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am test` 编译与单测通过，`rg "Approval|LeaveApplication" iboot-admin/src` 无残留
- [x] 2.2 删除仅被审批使用的 `NotificationPort`、`NotificationCommand`、`HttpNotificationPort`、`NotificationEventType`；验证：`rg "NotificationPort|NotificationCommand|NotificationEventType" iboot-admin/src` 无结果
- [x] 2.3 从 `iboot-admin/pom.xml` 移除 `flowable-spring-boot-starter-process`，删除 `flowable.*` 配置与测试中的 Flowable 自动配置排除项；验证：admin 启动日志不再出现 Flowable，启动时间与堆占用记录在案
- [x] 2.4 保留 `iboot_admin` 的 33 张表与审批菜单/权限种子；验证：启动后表仍存在，管理员侧边栏仍显示审批中心，四个权限码可用

## 3. 回归收敛

- [x] 3.1 `migrations.txt` 的 `/api/approval` 从 `transitional` 改为 `done`，刷新 `golden/admin.json`；验证：`bash scripts/regress/path-conservation.sh` 通过，admin 快照恰好多出 7 条 `/api/approval/**` 差异而无其它差异
- [x] 3.2 执行全量回归；验证：`bash scripts/regress/regress.sh` 全绿（含冒烟、权限矩阵、链路追踪）
- [x] 3.3 记录 admin 删除 Flowable 前后的启动时间与堆占用；验证：数值写入 `doc/23` §11.9 的收敛验收记录

## 4. 文档与回滚演练

- [x] 4.1 更新 `ROADMAP` §4.2 状态表、`doc/23` §11.9、`README` 项目结构、`doc/07` schema 归属表；验证：文档中 workflow 状态为「已完成」，回滚方式写明 `git revert` + 数据源指回 `iboot_admin`
- [x] 4.2 回滚数据演练：临时把 workflow 数据源指向 `iboot_admin`，确认旧表数据可读，再切回 `iboot_workflow`；验证：两侧审批记录均可查询，切回后新数据不受影响
