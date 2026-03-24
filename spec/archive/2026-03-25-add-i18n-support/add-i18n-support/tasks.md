# 实施任务清单

## 第一阶段：后端 i18n 基础（任务 1-8）

1. 创建 `I18nConfig.java`，配置 `MessageSource` 和 `LocaleResolver` beans
2. 创建 `I18nUtil.java` 工具类用于获取消息
3. 创建 `I18nMessage.java` 封装国际化响应
4. 修改 `Result.java` 支持基于消息码的国际化
5. 创建 `I18nController.java` 提供语言切换 API 接口
6. 创建 `i18n/messages.properties`（默认中文消息）
7. 创建 `i18n/messages_zh_CN.properties`（中文消息）
8. 创建 `i18n/messages_en_US.properties`（英文消息）

## 第二阶段：前端 i18n 基础（任务 9-16）

9. 通过 npm 安装 `vue-i18n@9` 包
10. 创建 `src/locales/index.ts` 配置 vue-i18n
11. 创建 `src/locales/zh-CN/` 目录及初始 JSON 语言包
12. 创建 `src/locales/en-US/` 目录及英文翻译
13. 创建 `src/composables/useLocale.ts` Hook 管理语言切换
14. 创建 `src/components/LocalePicker.vue` 语言切换组件
15. 修改 `src/main.ts` 注册 i18n 插件
16. 修改 `src/App.vue` 使用 `ElConfigProvider` 设置 Element Plus 语言

## 第三阶段：视图组件重构（任务 17-26）

17. 重构 `LoginView.vue` 和 `RegisterView.vue`（认证模块）
18. 重构 `DashboardView.vue` 和 `DashboardHomeView.vue`（仪表板）
19. 重构 `UserView.vue`、`RoleView.vue`、`MenuView.vue`（系统管理）
20. 重构 `DictView.vue`、`ConfigView.vue`（系统管理续）
21. 重构 `DeptView.vue`、`PostView.vue`、`OrgChartView.vue`（组织管理）
22. 重构 `LoginLogView.vue`、`OperateLogView.vue`、`AuditLogView.vue`（安全管理）
23. 重构 `NoticeView.vue`、`MessageCenterView.vue`（消息管理）
24. 重构 `FileManageView.vue`（文件管理）
25. 重构 `OnlineUserView.vue`、`ServerMonitorView.vue`、`RedisMonitorView.vue`（监控管理）
26. 重构 `SqlMonitorView.vue`、`JobView.vue`（监控管理续）

## 第四阶段：测试与优化（任务 27-32）

27. 测试所有页面的语言切换功能
28. 测试语言偏好持久化（localStorage + Cookie）
29. 测试 Element Plus 组件翻译（日期选择器、表格等）
30. 测试后端 API 响应消息的两种语言
31. 测试异常和验证消息的国际化
32. 修复缺失的翻译或格式化问题
