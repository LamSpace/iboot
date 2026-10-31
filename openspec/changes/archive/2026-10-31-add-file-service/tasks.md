# Tasks

> 前置：`add-shared-kernel` 已完成（`iboot-kernel` 可被依赖）。
> 构建命令统一使用 `mvn -s /home/lam/repo/settings.xml`。

## 1. 模块与代码迁移

- [x] 1.1 根 `pom.xml` 增加 `iboot-file` 模块；模块 pom 依赖 `iboot-kernel` 与文件域所需依赖（Web、MyBatis、MySQL、Redis、Springdoc、存储 SDK），**不引入 Flyway**；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-file -am package` 成功
- [x] 1.2 新建 `com.iboot.file.FileApplication` 启动类与 `application.yml`（端口 `8082`、`spring.application.name=iboot-file`、数据源指向现有库、Nacos 用 `optional:` 导入、追踪配置与 admin 一致）；验证：本地启动后 `curl http://localhost:8082/actuator/health` 返回 `UP`
- [x] 1.3 迁入文件域七件套：`FileController`、`FileApplicationService`、`FileInfo` / `StorageObject` / `FileRepository` / `FileStorageStrategy`、`FileRepositoryImpl` / `FileMapper`(+xml)、三个存储实现与 `FileStorageConfig`；包名改为 `com.iboot.file.*`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-file test` 通过，且 `grep -rn "com.iboot.admin" iboot-file/src` 无结果
- [x] 1.4 新增只读 `sys_config` 查询，读取 `sys.file.maxSize` 与 `sys.file.allowedExtensions`，未配置时回退到原默认值；验证：单测覆盖「有配置 / 无配置」两条分支，取值与迁移前一致
- [x] 1.5 安全与日志复用内核：声明 `iboot.security.whitelist`（Swagger、`/actuator/**`、`/error`），引入内核自动配置；验证：不带 token 请求 `/api/file/list` 返回 401，带 token 返回 200
- [x] 1.6 存储交叉验证：用管理端旧实现上传一个文件，从 `iboot-file` 下载；再用新服务上传，从管理端下载；验证：两次都能取回且内容一致（对象路径规则一致）

## 2. 服务接入

- [x] 2.1 接入 Nacos 注册与 OpenTelemetry 追踪，配置与既有服务同构；验证：`http://localhost:8849` 可见 `iboot-file` 实例，一次文件请求在 Tempo 中能查到该服务的 span
- [x] 2.2 网关 `application-local.yml` 与 `application-nacos.yml` 增加 `/api/file/**` 路由（order 20，位于 ai 与 admin 兜底之间）；验证：经 `8088` 访问文件接口与直连 `8082` 响应一致
- [x] 2.3 `scripts/regress/routes.txt` 增加 `/api/file iboot-file`；验证：`bash scripts/regress/route-coverage.sh` 退出码 0
- [x] 2.4 在 `doc/23-微服务底座操作指南.md` 增加「新增服务接入清单」章节，按本变更的真实步骤写（模块、端口、服务名、路由、白名单、追踪、回归登记），并登记「过渡期共用 `sys_file` 与 `sys_config`」这一临时状态及退出条件；验证：照文档从零接入可复现，章节中的端口与配置与代码一致

## 3. 回归工具扩展

- [x] 3.1 `api-snapshot.sh` 与 `regress.sh` 的服务清单从写死改为读取配置，新增 `golden/file.json`；验证：对三个服务连续导出两次，两次结果 diff 为空
- [x] 3.2 新增 `path-conservation.sh` 与 `migrations.txt`：比较本次导出并集与基线并集，差异必须与登记的迁移逐条对应；验证：人为从 golden 删除一条路径（不登记）时退出码非 0 并打印该路径；登记后恢复通过
- [x] 3.3 新增 `equivalence.sh`（`--record` / `--verify`），对 `list`、`{id}`、`download-url/{id}`、`export` 四个只读请求记录状态码与归一化响应体；验证：同一状态下两次 diff 为空；人为改动响应中一个字段后检查失败并指出字段
- [x] 3.4 `smoke.sh` 增加经网关的文件链路：上传 → 查询 → 下载 → 删除，断言状态码与关键字段；验证：`bash scripts/regress/smoke.sh` 退出码 0，停掉 `iboot-file` 后该链路失败
- [x] 3.5 `permission-matrix.sh` 增加 file 用例：具备 `file:list` 与不具备该权限的角色各一条；验证：矩阵全部符合预期，把期望临时改为「放行」后脚本失败
- [x] 3.6 在 `doc/23` 补充三个新脚本的用法、`migrations.txt` 的登记规则与等价性回放的适用范围（只读请求，写操作由冒烟覆盖）；验证：按文档执行三条命令均得到文档描述的结果

## 4. 验证与演练

- [x] 4.1 全量构建与测试：`TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml verify`；验证：退出码 0
- [x] 4.2 全量回归：`bash scripts/regress/regress.sh`；验证：退出码 0，契约零漂移、路径守恒通过、等价性通过、冒烟与权限矩阵通过
- [x] 4.3 故障演练：停掉 `iboot-file`；验证：`/api/file/**` 经网关返回 503，`/api/user/**` 与 `/api/ai/**` 正常；重启后恢复且网关未重启
- [x] 4.4 回滚演练：把网关 `/api/file` 路由改回管理端 → 上传一个文件 → 改回 `iboot-file` → 列表可见；验证：三步均成功，数据未丢失
- [x] 4.5 追踪贯穿：一次经网关的文件下载得到 TraceID，链路含网关与 file 两跳（若触发回源则含 admin）；验证：`bash scripts/regress/trace-chain.sh <traceId> --expect iboot-gateway,iboot-file` 退出码 0

## 5. 文档与状态

- [x] 5.1 `ROADMAP.md` §4.2 增加按服务的拆分状态表（gateway 完成、kernel 完成、file 进行中、auth 待办），并把 auth 的排序依据改为「资源服务器契约先行、服务可后置」；验证：状态与变更目录一致，链接可达
- [x] 5.2 `doc/03-架构设计.md` 更新服务拓扑与端口表（新增 `iboot-file:8082`、`iboot-kernel` 模块）；验证：拓扑图与 `pom.xml`、`routes.txt` 逐项一致
