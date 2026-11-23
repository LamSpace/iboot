# Sentinel Dashboard TLS 与登录 — 设计

## 1. 证书与 keystore

复用 `docker/certs/portal.pem`（SAN 已覆盖 `localhost`、`127.0.0.1`、`host.docker.internal`），
在 `SAN_NAMES` 中补充 `sentinel-dashboard`。`tls-init.sh` 在 `ensure_runtime_copies` 中生成：

```bash
openssl pkcs12 -export -in portal.pem -inkey portal-key.pem -certfile rootCA.pem \
  -name sentinel -out sentinel-keystore.p12 -passout pass:changeit
```

Dashboard 以 Spring Boot 2.5 运行，使用标准 `server.ssl.*` 属性：

```text
-Dserver.ssl.enabled=true
-Dserver.ssl.key-store=file:/certs/sentinel-keystore.p12
-Dserver.ssl.key-store-password=changeit
-Dserver.ssl.key-store-type=PKCS12
-Dserver.ssl.key-alias=sentinel
```

## 2. 登录

基础 compose 默认开启登录并提供本地开发口令：

```text
-Dauth.enabled=true
-Dauth.username=${SENTINEL_DASHBOARD_USER:-sentinel}
-Dauth.password=${SENTINEL_DASHBOARD_PASSWORD:-sentinel123}
```

生产覆盖文件重复 TLS 属性，并把账号口令改为 `${SENTINEL_DASHBOARD_USER:?...}` 必填；
避免覆盖 `JAVA_TOOL_OPTIONS` 时丢失 TLS 配置。

## 3. 客户端

- Docker 服务：`SENTINEL_DASHBOARD=https://sentinel-dashboard:8858`。
- 宿主机服务：`SENTINEL_DASHBOARD=https://localhost:8858`。
- Sentinel 1.8.9 的 `TransportConfig` 已支持 `https://` 前缀，`SocketFactory` 使用默认 SSL Context；
  宿主机与容器的 JVM 已通过 `javax.net.ssl.trustStore` 信任本地 CA。
- Dashboard 反向探测服务命令端口 8719 仍是 HTTP，属于 Sentinel 内部协议，不在本变更范围。

## 4. 门户配置

新增迁移 `V17__sentinel_dashboard_https.sql`：

```sql
UPDATE sys_config SET config_value='https://localhost:8858'
WHERE config_key='sys.sentinel.dashboard.url' AND config_value='http://localhost:8858';

UPDATE sys_config SET config_value='false'
WHERE config_key='sys.sentinel.dashboard.embed' AND config_value='true';
```

关闭内嵌是因为登录会话 Cookie 不能可靠地跨源 iframe 复用；门户改为说明卡 + 新窗口打开。
Docker 全栈已有 `SYS_NACOS_CONSOLE_EMBED=false` 的先例。

## 5. 健康检查与回归

- compose 健康检查：`curl -sf --cacert /certs/ca/rootCA.pem https://localhost:8858/`。
- `dev-init.sh` 的 `wait_http` 使用 HTTPS 与本地 CA。
- `observability-tls.sh` 把 Sentinel 加入控制台矩阵：
  - HTTPS 健康端点返回 200/302；
  - 明文返回非 200；
  - 不带 CA 校验失败；
  - 不带会话访问 `/app/machine/list` 不是 200。
- 生产覆盖文件用 `docker compose config` 静态检查缺失变量时失败。

## 6. 边界

- 命令端口 8719 仍按 Sentinel 默认绑定策略监听；它不承载业务请求，继续登记为残余边界。
- Dashboard 的反向探测是 HTTP，仅在 Docker 网络内；不发布到宿主机。
- 规则仍从 Nacos 下发；Dashboard 重启不影响业务限流规则。
