# Spec Delta

## ADDED Requirements

### Requirement: 回归在 HTTPS 公开入口下执行

登录、冒烟、权限矩阵与契约相关的端到端检查 SHALL 通过 HTTPS 公开入口执行，并使用本地 CA 校验服务端证书。

- `REG_BASE`、`REG_AUTH_BASE`、`REG_GATEWAY` 与 `REG_OIDC_REDIRECT` MUST 支持 HTTPS，且默认值 MUST 与当前运行模式的公开来源一致。
- 回归脚本中的全部 HTTPS 请求 MUST 使用 `REG_CACERT` 指向的本地 CA；MUST NOT 使用 `-k` 或关闭校验。
- 未配置 CA 或 CA 不匹配时，检查 MUST 失败并指出证书校验错误，不得回退到明文。
- 至少一组负向用例 MUST 证明明文入口不能完成登录或业务请求。

#### Scenario: HTTPS 登录与请求成功

- **WHEN** 在启用 TLS 的环境中执行回归
- **THEN** 登录、令牌交换与业务请求都通过 HTTPS 公开入口完成
- **AND** 所有请求使用本地 CA 校验通过

#### Scenario: 缺少 CA 时失败

- **WHEN** 回归请求未携带 `REG_CACERT` 指向的 CA
- **THEN** 请求因证书校验失败而终止
- **AND** 检查结果明确指出证书问题

#### Scenario: 明文入口不能完成登录

- **WHEN** 回归尝试通过明文入口完成授权码登录
- **THEN** 请求被重定向或拒绝
- **AND** 不取得授权码或令牌

#### Scenario: 公开来源不一致时失败

- **WHEN** 回归的认证地址与认证服务 issuer 不一致
- **THEN** 登录或令牌交换失败
- **AND** 检查输出指出期望的公开来源
