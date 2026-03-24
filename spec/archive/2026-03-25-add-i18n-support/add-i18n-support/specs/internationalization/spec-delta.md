# 规范差异：国际化支持

## ADDED Requirements

### Requirement: 前端语言包结构
系统 SHALL 为 Vue 3 前端提供模块化的语言包结构。

#### 结构：
```
iboot-portal/src/locales/
├── zh-CN/
│   ├── common.json        # 通用词汇（确定、取消、保存等）
│   ├── login.json         # 登录页面翻译
│   ├── register.json      # 注册页面翻译
│   ├── dashboard.json     # 仪表板翻译
│   ├── system/            # 系统管理模块
│   │   ├── user.json
│   │   ├── role.json
│   │   ├── menu.json
│   │   ├── dict.json
│   │   └── config.json
│   ├── organization/      # 组织管理模块
│   │   ├── dept.json
│   │   ├── post.json
│   │   └── org-chart.json
│   ├── security/          # 安全管理模块
│   │   ├── login-log.json
│   │   ├── operate-log.json
│   │   └── audit-log.json
│   ├── monitor/           # 监控管理模块
│   │   ├── online-user.json
│   │   ├── server.json
│   │   ├── redis.json
│   │   ├── sql.json
│   │   └── job.json
│   ├── message/           # 消息管理模块
│   │   ├── notice.json
│   │   └── message-center.json
│   ├── file/              # 文件管理模块
│   │   └── file-manage.json
│   └── analysis/          # 分析报表模块
│       ├── report.json
│       └── usage.json
├── en-US/
│   └──（同 zh-CN 结构）
└── index.ts               # i18n 配置入口
```

#### Scenario: 语言包模块加载
GIVEN 用户加载仪表板页面
WHEN 仪表板模块被激活
THEN 系统 SHALL 加载当前语言的 `dashboard.json`
AND 缓存它以供后续请求使用

---

### Requirement: Vue I18n 配置
系统 SHALL 配置 vue-i18n v9+ 支持 Composition API。

#### Scenario: 初始化 i18n 设置
GIVEN 应用启动
WHEN `main.ts` 初始化 Vue 应用
THEN 系统 SHALL 安装 vue-i18n 插件
AND 从 localStorage 或浏览器偏好设置默认语言
AND 配置 fallbackLocale 为 zh-CN

#### Scenario: Composition API 使用
GIVEN Vue 组件需要翻译
WHEN 组件使用 `useI18n()` composable
THEN 系统 SHALL 提供 `t()` 函数用于翻译
AND 提供 `locale` ref 获取当前语言

---

### Requirement: 语言切换组件
系统 SHALL 提供语言切换 UI 组件。

#### Scenario: 用户切换语言
GIVEN 用户查看带有语言切换组件的任意页面
WHEN 用户点击语言下拉框
AND 选择 "English" (en-US)
THEN 系统 SHALL:
  - 更新 vue-i18n locale 为 en-US
  - 更新 Element Plus locale 为英文
  - 保存偏好设置到 localStorage
  - 调用后端 API 持久化偏好设置
  - 重新渲染页面上所有翻译文本

#### Scenario: 语言切换器显示
GIVEN 当前语言是 zh-CN
WHEN 语言切换器渲染
THEN 它 SHALL 显示 "简体中文" 为当前选择
AND 显示 "English" 为可选项

GIVEN 当前语言是 en-US
WHEN 语言切换器渲染
THEN 它 SHALL 显示 "English" 为当前选择
AND 显示 "简体中文" 为可选项

---

### Requirement: 后端 MessageSource 配置
系统 SHALL 配置 Spring MessageSource 实现后端国际化。

#### Scenario: MessageSource bean 初始化
GIVEN Spring 应用上下文启动
WHEN `I18nConfig` 被处理
THEN 系统 SHALL 创建 `ResourceBundleMessageSource` bean
AND 配置 basenames: `i18n/messages`, `i18n/exception/exceptions`, `i18n/business/business`
AND 设置默认编码为 UTF-8
AND 设置缓存时间为 3600 秒

#### Scenario: LocaleResolver 配置
GIVEN HTTP 请求到达
WHEN `LocaleResolver` 被调用
THEN 系统 SHALL 从 `Accept-Language` 头解析语言
AND 如果未指定则默认为简体中文 (zh-CN)

---

### Requirement: 后端语言切换 API
系统 SHALL 提供 REST API 管理语言偏好。

#### Scenario: 修改用户语言
GIVEN 已认证用户
WHEN 用户发送 POST 到 `/api/i18n/locale?locale=en-US`
THEN 系统 SHALL:
  - 验证 locale 参数
  - 设置 Cookie，名称为 `org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver.locale`
  - 设置 cookie path 为 "/"
  - 设置 cookie max age 为 31536000 (1 年)
  - 返回成功响应包含当前语言

**请求：**
```http
POST /api/i18n/locale?locale=en-US
Cookie: JSESSIONID=xxx
```

**响应：**
```json
{
  "code": 200,
  "message": "Language changed successfully",
  "data": {
    "locale": "en-US"
  }
}
```

#### Scenario: 获取当前语言
GIVEN 任意客户端请求
WHEN 客户端发送 GET 到 `/api/i18n/locale`
THEN 系统 SHALL 从 `Accept-Language` 头返回当前语言

**响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "zh-CN"
}
```

---

### Requirement: I18nUtil 工具类
系统 SHALL 提供工具类获取国际化消息。

#### Scenario: 获取当前语言的消息
GIVEN Spring 管理的组件
WHEN 组件调用 `i18nUtil.getMessage("user.login.success")`
THEN 系统 SHALL:
  - 从 `LocaleContextHolder` 获取当前语言
  - 在 MessageSource 中查找消息码
  - 返回本地化的消息字符串
  - 如果翻译未找到则返回消息码本身（fallback）

#### Scenario: 获取带参数的消息
GIVEN 消息码包含占位符
WHEN 组件调用 `i18nUtil.getMessage("user.welcome", username)`
THEN 系统 SHALL 将 `{0}` 替换为用户名
AND 返回格式化后的消息

---

### Requirement: Result 类国际化
系统 SHALL 在 `Result` 类中支持基于消息码的响应。

#### Scenario: 带消息码的成功响应
GIVEN 控制器方法返回成功
WHEN 方法调用 `Result.success("user.login.success", data)`
THEN 系统 SHALL:
  - 在当前语言中查找消息码 "user.login.success"
  - 设置响应消息为本地化文本（如 "登录成功" 或 "Login successful"）
  - 返回标准 Result 结构包含 code、message 和 data

#### Scenario: 带消息码的错误响应
GIVEN 控制器方法返回错误
WHEN 方法调用 `Result.error("user.login.failed")`
THEN 系统 SHALL 查找消息码
AND 返回本地化的错误消息

---

### Requirement: 异常消息国际化
系统 SHALL 支持国际化的异常消息。

#### Scenario: 带消息码的 BusinessException
GIVEN 服务抛出 `new BusinessException("user.not_found")`
WHEN 异常被全局异常处理器捕获
THEN 系统 SHALL:
  - 在当前语言中查找消息码
  - 返回本地化的错误响应给客户端

**响应 (zh-CN):**
```json
{
  "code": 500,
  "message": "用户不存在"
}
```

**响应 (en-US):**
```json
{
  "code": 500,
  "message": "User not found"
}
```

---

### Requirement: Element Plus 语言集成
系统 SHALL 集成 Element Plus 内置 i18n 实现组件翻译。

#### Scenario: Element Plus 日期选择器翻译
GIVEN 用户查看带有 Element Plus DatePicker 的页面
WHEN 语言设置为 en-US
THEN DatePicker SHALL 显示英文标签 (Sun, Mon, Tue 等)
AND 英文月份名称 (January, February 等)

#### Scenario: Element Plus 表格翻译
GIVEN 用户查看带有 Element Plus Table 的页面
WHEN 语言设置为 en-US
THEN 表格为空时 SHALL 显示 "No Data"
AND 分页文本 SHALL 显示 "Total {total} items"

---

## MODIFIED Requirements

### Requirement: 用户认证流程
系统 SHALL 在认证过程中返回国际化消息。

**之前的行为：**
- 登录成功总是返回 "登录成功"
- 登录失败总是返回 "用户名或密码错误"

**更新后的行为：**
- 登录成功根据语言返回消息：
  - zh-CN: "登录成功"
  - en-US: "Login successful"
- 登录失败根据语言返回消息：
  - zh-CN: "用户名或密码错误"
  - en-US: "Invalid username or password"

#### Scenario: 英文环境登录
GIVEN 用户已选择英文 (en-US) 作为语言
WHEN 用户提交有效凭据
THEN 系统 SHALL 返回英文成功消息
AND 消息 SHALL 为 "Login successful"

---

### Requirement: 全局异常处理器
系统 SHALL 以用户偏好的语言返回异常消息。

**之前的行为：**
- 所有异常消息都是硬编码的中文

**更新后的行为：**
- 异常消息从 MessageSource 根据请求语言查找

#### Scenario: 英文环境业务异常
GIVEN 发生业务规则违反
WHEN 系统抛出 BusinessException 带消息码 "order.insufficient_stock"
AND 用户语言为 en-US
THEN 响应消息 SHALL 为 "Insufficient stock available"

---

## REMOVED Requirements

### Requirement: Result 类中的硬编码中文消息
系统 SHALL NOT 在 `Result` 类中使用硬编码的中文消息。

**移除的行为：**
```java
// 旧：硬编码中文
public static <T> Result<T> success(T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setData(data);
    result.setMessage("操作成功"); // 已移除：硬编码中文
    return result;
}
```

**替换为：**
```java
// 新：基于消息码的国际化
public static <T> Result<T> success(String code, T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setData(data);
    result.setMessage(I18nUtil.getMessage(code)); // 使用语言感知查找
    return result;
}
```

---

### Requirement: 视图组件中的硬编码中文占位符
系统 SHALL NOT 在 Vue 组件中使用硬编码的中文占位符。

**移除的行为：**
```vue
<!-- 旧：硬编码中文 -->
<el-input v-model="form.username" placeholder="用户名" />
<el-button type="primary">登录</el-button>
```

**替换为：**
```vue
<!-- 新：翻译函数 -->
<el-input v-model="form.username" :placeholder="t('login.username_placeholder')" />
<el-button type="primary">{{ t('login.submit') }}</el-button>
```

---

## 验证模式

### 验证文件创建
```bash
# 前端语言包
find iboot-portal/src/locales -name "*.json" | wc -l  # 应为 16+

# 后端消息文件
find iboot-admin/src/main/resources/i18n -name "*.properties" | wc -l  # 应为 9+

# 新配置文件
ls iboot-admin/src/main/java/com/iboot/admin/common/i18n/  # I18nUtil.java, I18nMessage.java
ls iboot-admin/src/main/java/com/iboot/admin/infrastructure/config/ | grep I18n  # I18nConfig.java

# 前端新组件
ls iboot-portal/src/components/LocalePicker.vue
ls iboot-portal/src/composables/useLocale.ts
```

### 验证文件修改
```bash
# 后端修改文件
grep -l "I18nUtil.getMessage" iboot-admin/src/main/java/com/iboot/admin/common/result/Result.java
grep -l "LocaleResolver" iboot-admin/src/main/java/com/iboot/admin/infrastructure/config/WebConfig.java

# 前端修改文件
grep -l "useI18n" iboot-portal/src/views/**/*.vue | wc -l  # 应为 30+
grep -l "import i18n" iboot-portal/src/main.ts
grep -l "ElConfigProvider" iboot-portal/src/App.vue
```

### 格式验证
```bash
# 验证 spec-delta 结构
grep -c "## ADDED Requirements\|MODIFIED Requirements\|REMOVED Requirements" spec/changes/add-i18n-support/specs/internationalization/spec-delta.md

# 验证需求格式
grep -c "### Requirement:" spec/changes/add-i18n-support/specs/internationalization/spec-delta.md

# 验证场景格式（应使用 4 个 # 符号）
grep -c "#### Scenario:" spec/changes/add-i18n-support/specs/internationalization/spec-delta.md
```
