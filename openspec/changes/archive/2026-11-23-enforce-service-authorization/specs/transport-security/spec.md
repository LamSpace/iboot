# Spec Delta

## MODIFIED Requirements

### Requirement: 每服务工作负载身份由客户端证书证明

每个调用方 SHALL 在访问业务服务时出示与自身身份对应的证书；服务端 SHALL 只接受由本地 CA 签发的客户端证书。

- 十个业务服务 MUST 使用自己的服务证书作为出站客户端证书。
- 门户 nginx 与宿主机 Vite 代理 MUST 使用门户证书作为客户端身份。
- Prometheus 与 blackbox MUST 使用各自的探针客户端证书抓取服务端口与执行证书探针。
- 服务端 MUST 通过本地 CA 校验证书链；调用方授权 MUST 由 `service-authorization` 执行，MUST NOT 仅凭证书链放过内部路径。

#### Scenario: 服务间调用携带各自身份

- **WHEN** 任一服务调用另一个服务
- **THEN** 调用方出示自己的服务证书
- **AND** 服务端信任链校验通过

#### Scenario: 门户链路携带门户身份

- **WHEN** 浏览器经门户访问网关、系统服务或运维控制台
- **THEN** 门户向上游出示门户证书
- **AND** 上游接受该证书并返回业务响应

#### Scenario: 探针携带探针身份

- **WHEN** Prometheus 抓取服务指标或 blackbox 执行证书探针
- **THEN** 请求携带对应的探针客户端证书
- **AND** 抓取与探针成功

#### Scenario: 未做按调用方授权

- **WHEN** 使用任一由本地 CA 签发的服务证书访问未被允许的路径
- **THEN** TLS 层接受该证书
- **AND** 业务层返回 403，该历史边界已由 `service-authorization` 关闭
