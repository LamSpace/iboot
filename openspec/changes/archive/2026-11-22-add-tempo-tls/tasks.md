# Tasks

## 1. Tempo 服务端 TLS

- [x] 1.1 在 `docker/tempo/tempo.yaml` 为 3200 配置 `http_tls_config`，为 OTLP 4317/4318 接收器配置 TLS，复用 `/certs/portal.pem`；验证：临时容器启动无配置解析错误，3200 与 4318 的 `openssl s_client` 握手成功
- [x] 1.2 在 `docker/docker-compose.yml` 为 Tempo 挂载 `./certs/runtime:/certs:ro`，并把 `TEMPO_OTLP` 改为 `https://tempo:4318/v1/traces`；验证：`docker compose config` 中 Tempo 含证书挂载且环境变量为 https

## 2. 应用与 Grafana 接入

- [x] 2.1 将九个服务 `application.yml` 的 `TEMPO_OTLP` 默认值改为 `https://localhost:4318/v1/traces`；验证：`grep -rn 'TEMPO_OTLP' iboot-*/src/main/resources/application.yml` 全部为 https
- [x] 2.2 将 Grafana Tempo 数据源改为 `https://tempo:3200` 并配置 CA 校验；验证：Grafana 重建后数据源健康检查通过，Tempo 查询可用

## 3. 脚本与回归

- [x] 3.1 `scripts/regress/trace-chain.sh` 默认 HTTPS 并支持 `--cacert`；验证：对运行中的 Tempo 执行一次 traceId 查询成功
- [x] 3.2 `scripts/apm/verify.sh` 的 Tempo 默认地址改为 HTTPS；验证：脚本帮助与调用参数均为 https
- [x] 3.3 在 `scripts/regress/internal-tls.sh` 增加 Tempo 3200/4317/4318 的 TLS 正向、明文与无 CA 负向检查；验证：该脚本 Tempo 段全部通过
- [x] 3.4 从 `scripts/regress/plaintext-allowlist.txt` 移除 Tempo 两条；验证：`plaintext-audit.sh` 通过且清单只剩 CLI 回环回调

## 4. 文档

- [x] 4.1 更新 `docs/11-可观测性设计.md`、`docs/23-微服务底座操作指南.md`、`docs/27-TLS 与零信任操作指南.md`、`docs/02-技术栈.md` 与 `docker/README.md` 中的 Tempo 地址、边界与验证命令
- [x] 4.2 验证：`grep -rn 'http://.*:\(3200\|4318\)' docs docker/README.md` 不再命中现行文档（归档与明文边界说明除外）

## 5. 集成验证

- [x] 5.1 在容器全栈重启后，确认服务经 HTTPS 上报 span，Grafana 可查询到该链路；验证：`scripts/regress/trace-chain.sh` 查询到 gateway + system 的完整链路，Grafana 数据源健康检查为 OK
- [x] 5.2 执行全栈回归；验证：TLS、内部 TLS、明文审计与权限矩阵全部通过，冒烟除既有 `IBOOT_BACKUP_ENABLED=false` 备份用例外通过（该失败与本次变更无关）
