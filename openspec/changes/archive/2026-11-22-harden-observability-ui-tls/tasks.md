# Tasks

## 1. 控制台服务端 TLS

- [x] 1.1 新增 Prometheus/Alertmanager Web TLS 配置并挂载证书，启用 `--web.config.file`；验证：两者 HTTPS 可用、明文失败
- [x] 1.2 Thanos Query 启用 `--http.config` 并挂载证书；验证：19192 HTTPS 可用、明文失败
- [x] 1.3 Grafana 启用原生 HTTPS，Kibana 启用服务端 SSL；验证：3000/5601 HTTPS 可用、明文失败

## 2. 上游链路与数据源

- [x] 2.1 Grafana 的 Prometheus/Thanos 数据源改为 HTTPS 并配置 CA；验证：两个数据源健康检查通过
- [x] 2.2 Thanos sidecar 查询 Prometheus 改为 HTTPS + CA，Prometheus 抓取自身与通知 Alertmanager 改为 HTTPS；验证：sidecar 无证书错误、告警可投递
- [x] 2.3 新增 `V16__observability_https_urls.sql` 更新五个控制台 URL；验证：`sys_config` 查询全部为 `https://`

## 3. 回归与明文清单

- [x] 3.1 新增 `scripts/regress/observability-tls.sh`，检查五个控制台 HTTPS、明文拒绝与无 CA 失败，并接入 `regress.sh`；验证：新脚本全部通过
- [x] 3.2 `tls-cert-monitor.sh` 改为 HTTPS 查询 Prometheus；验证：证书监控脚本通过
- [x] 3.3 从 `plaintext-allowlist.txt` 移除 Grafana→Prometheus/Thanos 两条；验证：明文审计仅剩 CLI 回环回调

## 4. 文档

- [x] 4.1 更新 `docs/08`、`docs/11`、`docs/27`、`docker/README.md` 的控制台地址、边界与验证命令；登记 Sentinel 回环 HTTP 例外
- [x] 4.2 更新回滚与排障说明；验证：文档命令可执行，且无明文控制台地址

## 5. 集成验证

- [x] 5.1 重建可观测组件与门户，确认门户内嵌 Grafana/Kibana/Thanos 无混合内容；验证：五个控制台 HTTPS 页面可加载，`sys_config` 与门户默认地址均为 `https://`
- [x] 5.2 执行全栈回归；验证：TLS 11/11、内部 TLS 53/53、可观测 TLS 23/23、证书监控 4/4、中间件认证 33/33、日志检索 12/12、暴露面 46/46、明文审计通过、权限矩阵 34/34；冒烟 43/44，唯一失败为既有备份用例
