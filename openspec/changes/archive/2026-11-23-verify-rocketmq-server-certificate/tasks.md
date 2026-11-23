# 任务

## 1. 兼容层

- [x] 1.1 在 workflow/notify 增加 `InsecureTrustManagerFactory` 兼容层，使用本地信任库并失败关闭。
- [x] 1.2 `tls-init.sh` 的 `SAN_NAMES` 增加 `rocketmq-tls`，更新 `--check` 说明。

## 2. 测试

- [x] 2.1 两个模块增加 `RocketMqServerTrustTest`：正确 CA 通过、自签名拒绝、可选集成投递。
- [x] 2.2 新增 `scripts/regress/rocketmq-server-auth.sh`：SAN、fat jar 打包、正确/错误信任库与投递。
- [x] 2.3 `regress.sh` 接入新的 RocketMQ 服务端证书检查。

## 3. 文档

- [x] 3.1 更新 `docs/27` §1/§19.1/§22.3，删除“客户端不校验服务端证书”的边界表述。
- [x] 3.2 更新 `docker/README.md` 与 `ROADMAP.md`。

## 4. 验证

- [x] 4.1 `scripts/tls-init.sh --check` 通过（新增 SAN 后按需 `--force`）。
- [x] 4.2 `bash scripts/regress/rocketmq-server-auth.sh` 通过。
- [x] 4.3 完整本地回归通过（8c 6/6）；仅剩 Docker 备份关闭导致的已知冒烟用例失败。
- [x] 4.4 workflow 发布与 notify 消费仍成功，错误 CA 场景失败。
