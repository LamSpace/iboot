# Proposal

## Why

`iboot-portal` 目前没有统一的设计系统：44 个 Vue 文件全部携带页面级 scoped 样式，27 个文件直接硬编码 Element Plus 调色板色值（如 `#409eff`、`#303133`），8 个文件使用自定义渐变；应用外壳仍是深色侧栏 + 高亮黄色的旧式后台风格。视觉语言不一致，改一处颜色需要逐页搜索替换，新增页面也会继续复制这些写法。

MiniMax 的 `DESIGN.md` 提供了一套成熟的黑白主色、紧凑字阶、圆角层级与扁平边框语言，适合作为后台视觉基线。现在需要把它转化为可执行的 token 与组件约束，在不改变业务行为的前提下完成一次系统性视觉与布局优化。

## What Changes

- 引入单一 design token 来源（颜色、字体、间距、圆角、阴影、动效），并映射到 Element Plus CSS 变量；业务视图不再直接使用调色板色值。
- 字体策略：Latin/数字使用 DM Sans，中文回退到 CJK 字体（Noto Sans SC / PingFang SC 等），字体自托管，不依赖外部 CDN。
- 重做应用外壳：浅色侧栏、可折叠菜单、移动端抽屉、面包屑、统一页头与内容容器；**菜单树、路由、权限模型保持不变**。
- 建立共享页面原型与组件：页头、筛选栏、数据表格容器、空态/加载态/错误态、指标卡、确认弹窗等。
- 深度优化约 10 个核心页面（Home / Login / Register / Dashboard / User / Role / File / Usage / Report / MessageCenter）。
- 其余 CRUD、日志、组织、通知页面套用统一原型，仅做必要的样式清理与状态补齐。
- 8 个 iframe 监控页（Grafana / Kibana / Prometheus / Thanos / Swagger / Druid / Alertmanager / Server）只统一外层容器与状态，不重设计 iframe 内部。
- 清理业务视图中的硬编码调色板色值与装饰性渐变，补齐中英文与四档断点下的可用性。

**非目标（本变更明确不做）**

- 不修改业务逻辑、API 契约、路由、权限模型或 `sys_menu` 菜单树；不做信息架构重组或数据迁移。
- 不升级 Element Plus 大版本，不引入新的 UI 框架或图表库。
- 不做暗色模式（`DESIGN.md` 未提供暗色 token，如需另立变更）。
- 不做移动端优先重写；仅保证桌面优先下的必要响应式。
- 不重新设计 iframe 内部页面。

## Capabilities

### New Capabilities
- `portal-design-system`: `iboot-portal` 的设计 token、Element Plus 皮肤映射、应用外壳、页面原型与视觉一致性约束。

### Modified Capabilities
<!-- 无现有 spec；本次不修改既有能力要求。 -->

## Impact

- 前端代码：`iboot-portal/src/styles/`、`src/components/`、`src/views/`（34 个视图）、`src/main.ts`、`src/App.vue`、`package.json`（字体资源与可能的构建配置）。
- 新增资源：自托管字体文件（woff2）与 token/主题样式文件。
- 不改动：`iboot-admin` 后端、数据库、Flyway 迁移、API 路径、权限数据、`sys_menu` 菜单树。
- 依赖：保持 Element Plus 2.6.x 与现有构建工具链；不新增重型运行时依赖。
- 行为影响：仅视觉与布局；登录 → Dashboard → CRUD → 导入导出 → 文件上传 → 监控嵌入等核心流程行为不变。
