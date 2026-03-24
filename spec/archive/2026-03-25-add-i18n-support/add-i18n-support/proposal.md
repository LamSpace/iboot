# 提案：添加国际化 (i18n) 支持

## 背景

当前 iBoot 系统的所有文字都硬编码为中文：
- **前端 (iboot-portal)**: 所有 UI 文本、按钮、标签和消息都是硬编码的中文
- **后端 (iboot-admin)**: 所有 API 响应消息、错误消息和验证消息都是硬编码的中文

这造成了以下障碍：
- 非中文用户无法使用
- 无法部署到国际环境
- 跨国企业使用受限
- 未来拓展非中文市场受限

实施国际化将使系统支持多种语言（首期支持中文和英文），允许用户根据偏好切换语言。

## 变更内容

### 前端变更
- 集成 `vue-i18n` v9+ 实现 Vue 3 国际化
- 创建模块化语言包结构（JSON 格式）
- 实现语言切换组件（`LocalePicker.vue`）
- 创建 `useLocale` composable 管理语言设置
- 重构所有视图组件使用 `$t()` 翻译函数
- 支持语言偏好持久化（localStorage + 后端同步）
- 集成 Element Plus 内置 i18n 实现组件翻译

### 后端变更
- 配置 Spring `MessageSource` bean 解析消息
- 实现 `AcceptHeaderLocaleResolver` 检测语言
- 创建 `I18nUtil` 工具类获取消息
- 重构 `Result` 类支持消息码
- 创建语言切换 REST API（`/api/i18n/locale`）
- 异常和验证消息国际化
- 创建中文（zh-CN）和英文（en-US）的 properties 文件

### 语言支持
- **第一期**: 中文（zh-CN）为默认语言，英文（en-US）为备选
- **未来**: 可扩展至繁体中文、日语、韩语、法语、西班牙语

## 影响

### 受影响的规范
- 无现有规范修改（这是新增能力）

### 代码影响
| 范围 | 文件数量 | 类型 |
|------|----------|------|
| 前端语言包 | ~20 个新 JSON 文件 | 新增 |
| 后端 i18n | ~9 个新 properties 文件 | 新增 |
| 前端配置 | `locales/index.ts`, `useLocale.ts`, `LocalePicker.vue` | 新增 |
| 后端配置 | `I18nConfig.java`, `I18nUtil.java`, `I18nController.java` | 新增 |
| 前端视图 | 30+ 个 `src/views/` 下的 `.vue` 文件 | 修改 |
| 前端核心 | `main.ts`, `App.vue`, `stores/app.ts` | 修改 |
| 后端核心 | `Result.java`, `WebConfig.java`, `BusinessException.java` | 修改 |

### API 影响
- **新增接口**:
  - `POST /api/i18n/locale` - 修改用户语言偏好
  - `GET /api/i18n/locale` - 获取当前语言
- **响应格式**: 现有 `Result<T>` 结构不变，但 message 字段支持多语言

### 用户影响
- **正面**: 用户可在中文和英文之间切换
- **正面**: 语言偏好在会话间持久化
- **中性**: 对现有功能无破坏性变更
- **迁移**: 用户默认为中文（现有行为），无需迁移

### 性能考虑
- 语言包在启动时加载（两种语言共约 50KB）
- 可按需实现模块级懒加载
- Spring 缓存消息解析（默认 TTL: 1 小时）

---

**变更 ID**: `add-i18n-support`
**预估工时**: 72-104 小时（9-13 工作日）
**优先级**: P1 (核心功能增强)
