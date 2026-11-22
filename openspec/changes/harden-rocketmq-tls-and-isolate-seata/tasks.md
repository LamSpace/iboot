# Tasks

## 1. RocketMQ TLS 入口与 `mq` 网络

- [x] 1.1 新增 `docker/rocketmq/tls-proxy.conf`：nginx stream `ssl_preread` 只转发 TLS ClientHello，非 TLS 路由到关闭端口；验证明文 `curl` 失败、`openssl s_client` 握手成功
- [x] 1.2 在 `docker/docker-compose.yml` 新增 `rocketmq-tls` 服务与 `mq` 网络，RocketMQ NameServer/Broker/Proxy/初始化任务只加入 `mq`；验证容器之间 DNS 与健康检查正常
- [x] 1.3 移除 Proxy 的 `18080/18081` 宿主机映射，只发布 `127.0.0.1:18081` 到 `rocketmq-tls`；验证宿主机 TLS 可用、明文不可用
- [x] 1.4 把 compose 中 `IBOOT_ROCKETMQ_ENDPOINTS` 改为 `rocketmq-tls:8081`，保持 `IBOOT_ROCKETMQ_SSL_ENABLED=true`；验证 workflow 投递与 notify 消费成功

## 2. Seata `tx` 网络隔离

- [x] 2.1 新增 `tx` 网络，`seata-server` 只加入 `tx`，`iboot-workflow` 与 `iboot-system` 加入 `tx`；验证三者可解析并连接 8091
- [x] 2.2 验证非 `tx` 服务无法解析或连接 `seata-server:8091`，8091 仍只绑定 `127.0.0.1`
- [x] 2.3 运行 `scripts/regress/seata.sh` 与 `seata-tcc.sh`，确认 AT/TCC 用例通过

## 3. 回归与脚本

- [x] 3.1 更新 `scripts/regress/log-search-tls.sh`：RocketMQ 改为 TLS-only 检查，新增明文拒绝用例，移除“同一端口仍接受明文”的旧输出
- [x] 3.2 更新 `scripts/regress/exposure.sh`：新增非 `tx` 服务访问 Seata 失败、非 `mq` 服务直连 RocketMQ Proxy 失败、TLS 入口正向可达
- [x] 3.3 更新 `scripts/regress/regress.sh`/README 中涉及的端点说明；验证端口与容器矩阵在 Docker 模式下全部通过

## 4. 文档

- [x] 4.1 更新 `docs/27` §9.4、§11、§12.2/12.4、§13.5，并新增 B5.3 小节：RocketMQ TLS-only 入口、Seata 显式例外与残余风险
- [x] 4.2 更新 `docs/08` 端口表、`docs/23` RocketMQ/Seata 表、`docker/README.md` 端口与网络说明
- [x] 4.3 更新 `ROADMAP.md` B5 状态，写明 RocketMQ TLS-only 与 Seata 例外已完成、剩余 ACL/服务端校验

## 5. 集成验证

- [x] 5.1 重建 RocketMQ、Seata、system、workflow、notify，执行全量 `scripts/regress/regress.sh`；验证公开入口、内部 TLS、日志、暴露面、冒烟全部通过
- [ ] 5.2 在 CI 分支运行全栈回归，确认 host 模式与 Docker 模式均通过；记录已知备份冒烟例外

## Workflow follow-up

- 同步 delta specs 到主 specs 并归档变更。
- 新建功能分支、提交、创建 PR、合并到 master。
