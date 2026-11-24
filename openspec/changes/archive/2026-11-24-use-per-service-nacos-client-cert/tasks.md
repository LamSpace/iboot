# Tasks

## 1. 配置与启动路径

- [x] 1.1 更新 `docker/docker-compose.yml`：Nacos 客户端属性指向 `/certs/nacos-client.pem` 与 `-key.pem`，十服务各挂载自己的证书；verification: `docker compose config` 渲染后每个服务只挂载自己的证书。
- [x] 1.2 更新 `scripts/dev-init.sh`：Nacos `certChainFile` / `certPrivateKey` 使用 `services/<service>.pem`；verification: `/proc/<pid>/cmdline` 中证书路径包含服务名。

## 2. 回归

- [x] 2.1 新增 `scripts/regress/nacos-client-cert.sh`：Docker 模式比较容器内证书指纹，宿主机模式检查进程参数，并断言不引用 `portal.pem`；verification: 脚本逐项 PASS。
- [x] 2.2 把脚本并入 `scripts/regress/regress.sh`；verification: 全量回归包含该节。
- [x] 2.3 运行全量回归；verification: `scripts/regress/regress.sh` 通过。

## 3. 文档

- [x] 3.1 更新 `docs/27` §18.5：移除“Nacos 注册客户端仍使用共享 portal.pem”，登记每服务证书与归因边界；verification: 文档与 compose/脚本一致。
- [x] 3.2 更新 `docs/23`、`docs/08`、`docker/README.md` 的 Nacos 客户端证书说明；verification: 无残留共享 `portal.pem` 描述。

## 4. Workflow follow-up

- [x] 归档 OpenSpec 变更并同步 `transport-security` 主规格。
- [x] 新建 `feat/use-per-service-nacos-client-cert` 分支，提交、推送、创建 PR；PR CI 通过后合并到 `master`。
- [x] 在 GitHub 触发 `full-regress` 并确认通过（或记录不可抗因素）。
