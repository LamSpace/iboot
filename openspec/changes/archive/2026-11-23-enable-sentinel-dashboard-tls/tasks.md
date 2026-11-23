# 任务

## 1. 证书与编排

- [x] 1.1 `tls-init.sh` 的 `SAN_NAMES` 增加 `sentinel-dashboard`，生成 `sentinel-keystore.p12` 并复制到 runtime。
- [x] 1.2 `docker-compose.yml` 为 sentinel-dashboard 挂载 `/certs`，开启 HTTPS 与登录，健康检查改用 HTTPS + CA。
- [x] 1.3 `docker-compose.prod.yml` 覆盖时保留 TLS 属性，并继续要求非空账号口令。
- [x] 1.4 `dev-init.sh` 的 `SENTINEL_DASHBOARD` 与 `wait_http` 改为 HTTPS。

## 2. 门户与数据库

- [x] 2.1 新增 `V17__sentinel_dashboard_https.sql`：URL 改 HTTPS，关闭内嵌。

## 3. 回归与文档

- [x] 3.1 `observability-tls.sh` 增加 Sentinel HTTPS、明文拒绝、无 CA 失败与匿名管理接口检查。
- [x] 3.2 更新 `docs/27`、`docs/23`、`docker/README.md`、`ROADMAP.md`。

## 4. 验证

- [x] 4.1 `scripts/tls-init.sh --check` 通过。
- [x] 4.2 `scripts/regress/observability-tls.sh` 通过。
- [x] 4.3 完整本地回归通过；仅剩 Docker 备份关闭导致的已知冒烟用例失败。
- [x] 4.4 重建 sentinel-dashboard 后确认 HTTPS 登录页、明文失败与匿名管理接口拒绝。
