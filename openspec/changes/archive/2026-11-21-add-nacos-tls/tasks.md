# Tasks

## 1. Nacos 服务端 TLS

- [x] 1.1 在 `scripts/tls-init.sh` 生成 `runtime/nacos-keystore.p12` 并设置容器可读权限；执行后 `keytool -list` 能读出私钥条目
- [x] 1.2 修改 compose 的 Nacos 环境变量与证书挂载，启用 HTTP/控制台 HTTPS 与 gRPC mTLS；验证 `https://localhost:8848`、`https://localhost:8849` 返回 200，HTTP 失败，9848 带客户端证书握手成功、无证书失败

## 2. 客户端 TLS

- [x] 2.1 在 `dev-init.sh` 与 compose 的 `JAVA_TOOL_OPTIONS` 注入 Nacos client TLS 系统属性与客户端证书；启动全部服务，验证每个实例在 Nacos 中可见
- [x] 2.2 验证宿主机的 gRPC mTLS 与配置拉取正常；去掉 `trustCollectionChainPath`、客户端证书或改为 `trustAll=true` 时注册失败

## 3. 控制台、脚本与健康检查

- [x] 3.1 将 Vite Nacos 代理、`dev-init.sh` 健康检查、`sentinel-init.sh`、`docker-deploy.sh` 与 Docker 控制台 URL 改为 HTTPS + CA；验证门户内嵌控制台可加载、Sentinel 基线写入成功
- [x] 3.2 更新 `.env.example` 与 Nacos 控制台相关配置说明；确认明文 HTTP 不再被脚本使用

## 4. 回归与文档

- [x] 4.1 在 `internal-tls.sh` 或新增检查中覆盖 Nacos HTTPS/gRPC TLS、明文拒绝与无 CA 失败，并接入 `regress.sh`
- [x] 4.2 更新 `docs/27`、`docs/23`、`docs/10`、`docs/05`、`docs/03`、`docker/README.md`、`ROADMAP.md` 的 Nacos 地址、命令与排障
- [x] 4.3 宿主机与 Docker 全量栈各跑一次回归；记录 Nacos 相关检查、冒烟、权限矩阵与 CLI 结果
  - 宿主机：内部 TLS 47/47、冒烟 44/44、权限矩阵 34/34。
  - Docker：内部 TLS 47/47、冒烟 43/44（唯一失败为既有 `IBOOT_BACKUP_ENABLED=false`）、权限矩阵 34/34。

## Workflow follow-up

- 代码评审与回归通过后提交功能分支、创建 PR、合并到 `master`。
- 合并后同步 delta spec 并归档本变更。
