# Spec Delta

## Purpose

提供门户内的微服务治理控制台入口，使运维人员可以从统一导航访问 Nacos 注册/配置中心与 Sentinel 治理控制台，并在开发与生产环境采用与鉴权状态匹配的访问方式。

## ADDED Requirements

### Requirement: 微服务治理菜单入口

门户 SHALL 提供顶级“微服务治理”导航，包含“Nacos 控制台”和“Sentinel 控制台”两个入口；入口可见性与访问 MUST 受权限控制。

#### Scenario: 有权限用户进入控制台
- **WHEN** 用户拥有 `governance:nacos` 或 `governance:sentinel` 权限并打开门户
- **THEN** 导航中显示“微服务治理”及其下有权限的控制台入口
- **AND** 点击入口可直接打开对应页面

#### Scenario: 无权限用户不可见也不可访问
- **WHEN** 用户没有对应控制台权限
- **THEN** 菜单中不显示该入口
- **AND** 直接访问其路由不会渲染控制台页面

### Requirement: 开发环境内嵌模式

当控制台未启用登录且页面配置为内嵌时，门户 SHALL 使用 iframe 直接渲染控制台，并 MUST 复用统一嵌入面板的加载、失败和重试状态。

- 内嵌 MUST 通过门户同源入口访问；该入口 MUST NOT 返回禁止嵌入的响应头，且 MUST NOT 在生产环境提供。
- iframe 加载失败或超时 MUST 显示明确错误与重试按钮，MUST NOT 留下空白区域。

#### Scenario: Nacos 开发环境内嵌
- **WHEN** 本地开发环境关闭 Nacos 控制台登录，且 `sys.nacos.console.embed` 为 `true`
- **THEN** 门户通过同源入口加载 Nacos 控制台，不出现 `X-Frame-Options` 拒绝
- **AND** 用户不需要在 iframe 内再次登录

#### Scenario: Sentinel 开发环境内嵌
- **WHEN** 本地开发环境关闭 Sentinel 控制台登录，且 `sys.sentinel.dashboard.embed` 为 `true`
- **THEN** 门户在“Sentinel 控制台”页面直接渲染 Sentinel Dashboard
- **AND** 工具栏的新窗口打开与刷新按钮可用

#### Scenario: 内嵌加载失败
- **WHEN** iframe 在超时时间内未完成加载
- **THEN** 页面显示加载失败提示和重试按钮
- **AND** 重试会重新加载 iframe

### Requirement: 生产环境跳转模式

当控制台启用登录或页面配置为不内嵌时，门户 MUST 只显示说明性提示和“在新窗口打开”入口，MUST NOT 内嵌、代理或代填控制台凭据。

#### Scenario: 生产环境 Nacos 只跳转
- **WHEN** 非本地环境将 `sys.nacos.console.embed` 设为 `false`
- **THEN** “Nacos 控制台”页面显示说明性提示和“在新窗口打开”按钮
- **AND** 门户不渲染 Nacos iframe

#### Scenario: 生产环境 Sentinel 只跳转
- **WHEN** 非本地环境将 `sys.sentinel.dashboard.embed` 设为 `false`
- **THEN** “Sentinel 控制台”页面显示说明性提示和“在新窗口打开”按钮
- **AND** 门户不渲染 Sentinel iframe

#### Scenario: 门户不保存控制台凭据
- **WHEN** 用户通过门户打开控制台
- **THEN** 门户不读取、不保存、不代填任何控制台账号或密码
- **AND** 控制台登录只能由用户在控制台自己的页面完成

### Requirement: 控制台地址可配置

门户 SHALL 从系统配置读取两个控制台的访问地址与内嵌开关；配置缺失或读取失败时 MUST 使用开发默认地址并保持页面可用。

#### Scenario: 配置覆盖默认地址
- **WHEN** 管理员修改 `sys.nacos.console.url` 或 `sys.sentinel.dashboard.url`
- **THEN** 对应页面使用新的地址打开控制台
- **AND** 不需要重新构建前端

#### Scenario: 配置缺失回退默认值
- **WHEN** 地址配置不存在或读取失败
- **THEN** 页面使用开发默认地址
- **AND** 页面不因配置读取失败而白屏

### Requirement: 控制台鉴权边界

本地开发环境 MAY 关闭控制台登录以支持内嵌；非本地环境 MUST 保持控制台鉴权开启，门户 MUST NOT 通过关闭鉴权来换取内嵌能力。

#### Scenario: 开发环境关闭 Nacos 控制台鉴权
- **WHEN** 本地开发编排启动 Nacos
- **THEN** Nacos 控制台鉴权关闭，控制台不需要登录即可访问
- **AND** Nacos 客户端与管理端鉴权仍按既有配置运行

#### Scenario: 生产环境保持鉴权
- **WHEN** 以非本地环境部署 Nacos 或 Sentinel
- **THEN** 控制台登录保持开启，账号密码由环境变量或密钥管理注入
- **AND** 门户只提供跳转入口，不内嵌控制台

### Requirement: 控制台不可用不影响门户

控制台停机或不可达时，门户 MUST 保持可用，并在对应页面给出明确的不可用状态；控制台恢复后 MUST 可通过刷新重新加载。

#### Scenario: 控制台停机
- **WHEN** Nacos 或 Sentinel 控制台停止运行
- **THEN** 门户其他页面正常可用
- **AND** 对应控制台页面显示不可用提示而不是持续空白

#### Scenario: 控制台恢复
- **WHEN** 控制台恢复运行后用户点击刷新
- **THEN** 页面重新加载控制台内容
- **AND** 不需要重启门户
