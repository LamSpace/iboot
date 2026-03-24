# Specification Deltas

## ADDED Requirements

### Requirement: 国际化设计文档

WHEN 开发人员查阅项目文档时，
系统 SHALL 提供完整的国际化设计文档。

#### Scenario: 查阅前端 i18n 架构
GIVEN 开发人员需要理解前端国际化实现
WHEN 打开 `doc/17-国际化设计.md`
THEN 文档 SHALL 包含 vue-i18n v9+ 配置说明
AND SHALL 包含语言包目录结构说明
AND SHALL 包含语言加载机制说明

#### Scenario: 查阅后端 i18n 架构
GIVEN 开发人员需要理解后端国际化实现
WHEN 打开 `doc/17-国际化设计.md`
THEN 文档 SHALL 包含 MessageSource 配置说明
AND SHALL 包含资源束（Resource Bundle）结构说明
AND SHALL 包含 LocaleResolver 工作原理说明

#### Scenario: 查阅语言切换流程
GIVEN 开发人员需要理解语言切换机制
WHEN 阅读 `doc/17-国际化设计.md`
THEN 文档 SHALL 包含前后端交互流程图或说明
AND SHALL 包含 Cookie/LocalStorage 持久化机制说明
AND SHALL 包含 Element Plus 集成说明

---

### Requirement: 国际化开发指南文档

WHEN 开发人员需要添加翻译时，
系统 SHALL 提供标准化的开发指南文档。

#### Scenario: 添加新翻译键
GIVEN 开发人员需要在语言包中添加新翻译
WHEN 查阅 `doc/18-国际化开发指南.md`
THEN 文档 SHALL 提供翻译文件命名规范
AND SHALL 提供键名命名约定（如：module.feature.key）
AND SHALL 提供中英文对照示例

#### Scenario: 前端组件使用翻译
GIVEN 开发人员需要在 Vue 组件中使用翻译
WHEN 查阅 `doc/18-国际化开发指南.md`
THEN 文档 SHALL 提供 useI18n() 组合式 API 示例
AND SHALL 提供 t() 函数使用示例
AND SHALL 提供参数化翻译示例

#### Scenario: 后端使用翻译
GIVEN 开发人员需要在后端代码中获取多语言消息
WHEN 查阅 `doc/18-国际化开发指南.md`
THEN 文档 SHALL 提供 I18nUtil 使用示例
AND SHALL 提供 Result 类消息码响应示例
AND SHALL 提供异常消息国际化示例

---

### Requirement: 文档导航更新

WHEN 用户访问 doc/README.md 时，
系统 SHALL 提供国际化文档的导航链接。

#### Scenario: 后端开发人员导航
GIVEN 后端开发人员查看文档导航
WHEN 阅读 `doc/README.md` 的"后端开发人员"列表
THEN SHALL 包含 `doc/17-国际化设计.md` 链接
AND SHALL 包含 `doc/18-国际化开发指南.md` 链接

#### Scenario: 技术栈概览更新
GIVEN 用户查看技术栈说明
WHEN 阅读 `doc/README.md` 的"技术栈概览"
THEN 前端技术栈 SHALL 列出 vue-i18n v9+
AND 后端技术栈 SHALL 列出 Spring MessageSource

---
