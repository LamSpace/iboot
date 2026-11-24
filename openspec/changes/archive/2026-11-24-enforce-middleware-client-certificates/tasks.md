# Tasks

## 1. 证书与账号

- [x] 1.1 `tls-init.sh` 为八个数据服务生成 `services/<service>.p12`，并生成 `clients/redis-exporter.pem`（clientAuth）；verification: 文件存在，`openssl pkcs12 -info` 可读。
- [x] 1.2 `mysql-users-init.sh` 对八个服务账号执行 `REQUIRE SUBJECT '/CN=iboot-<service>'`；verification: `SHOW CREATE USER` 含 REQUIRE 子句，migrator/exporter 不受影响。

## 2. 客户端与编排

- [x] 2.1 compose 为八个数据服务挂载 `client.p12`，`x-app-env` 注入 MySQL 客户端 keystore 参数；verification: `docker compose config` 渲染正确。
- [x] 2.2 Redis 开启 `--tls-auth-clients yes`；`redis-exporter` 注入客户端证书；verification: exporter 与数据服务可连接。
- [x] 2.3 `iboot-tls.yml`（三份）增加 `redis-client` PEM bundle；`dev-init.sh` 注入宿主机 MySQL/Redis 客户端证书路径；verification: 服务启动并连接成功。

## 3. 回归与文档

- [x] 3.1 更新 `internal-tls.sh`、`middleware-auth.sh`、`middleware-least-privilege.sh`、`exporter-mtls.sh`：MySQL/Redis 无证书拒绝、正确证书成功、错误服务证书不能冒充；verification: 脚本逐项 PASS。
- [x] 3.2 更新 `docs/27` §13/§14/§29、`docs/08`、`docker/README.md` 与 `ROADMAP.md`：登记 MySQL/Redis mTLS 与 Silo/Nacos HTTP 显式例外；verification: 文档与配置一致。
- [x] 3.3 运行全量回归；verification: `scripts/regress/regress.sh` 通过。

## 4. Workflow follow-up

- [x] 归档 OpenSpec 变更并同步 `transport-security` 主规格。
- [x] 新建 `feat/enforce-middleware-client-certificates` 分支，提交、推送、创建 PR；PR CI 通过后合并到 `master`。
- [x] 在 GitHub 触发 `full-regress` 并确认通过（或记录不可抗因素）。
