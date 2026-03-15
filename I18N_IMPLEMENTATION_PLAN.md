# iBoot 国际化 (i18n) 实施计划

## 1. 项目背景与目标

### 1.1 当前状态分析

**前端 (iboot-portal)**:
- 框架：Vue 3 + TypeScript + Element Plus
- 状态管理：Pinia
- **现状**：尚未集成 vue-i18n，所有界面文本均为硬编码中文
- Element Plus 已配置中文语言包 (`zh-cn`)

**后端 (iboot-admin)**:
- 框架：Spring Boot 3.5.9
- **现状**：尚未集成 Spring MessageSource 国际化机制
- 响应消息均为硬编码中文（如 `Result.java` 中的 "操作成功"、"操作失败"）

### 1.2 实施目标

为 iBoot 系统实现完整的国际化支持，包括：

1. **前端国际化**：基于 vue-i18n 实现多语言切换
2. **后端国际化**：基于 Spring MessageSource 实现多语言响应
3. **语言支持**：首期支持中文（zh-CN）和英文（en-US）
4. **用户体验**：提供语言切换功能，支持记住用户语言偏好

---

## 2. 技术方案设计

### 2.1 前端国际化方案

#### 2.1.1 技术选型

| 组件 | 技术方案 | 说明 |
|------|----------|------|
| i18n 框架 | vue-i18n v9+ | Vue 3 官方推荐，支持 Composition API |
| 语言包格式 | JSON | 模块化组织，按需加载 |
| 语言检测 | Cookie + localStorage | 优先使用用户选择，其次浏览器语言 |
| Element Plus | 内置 i18n | 使用 Element Plus 多语言包 |

#### 2.1.2 语言包结构设计

```
iboot-portal/src/locales/
├── zh-CN/
│   ├── common.json        # 通用词汇（确定、取消、保存等）
│   ├── login.json         # 登录页面
│   ├── register.json      # 注册页面
│   ├── dashboard.json     # 仪表板
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
│   └── (同 zh-CN 结构)
└── index.ts               # i18n 配置入口
```

#### 2.1.3 语言包示例

**zh-CN/common.json**:
```json
{
  "confirm": "确定",
  "cancel": "取消",
  "save": "保存",
  "delete": "删除",
  "edit": "编辑",
  "search": "搜索",
  "reset": "重置",
  "add": "新增",
  "view": "查看",
  "export": "导出",
  "import": "导入",
  "action": "操作",
  "status": "状态",
  "create_time": "创建时间",
  "update_time": "更新时间",
  "operation": {
    "success": "操作成功",
    "failed": "操作失败",
    "confirm_delete": "确认删除该记录吗？"
  }
}
```

**en-US/common.json**:
```json
{
  "confirm": "Confirm",
  "cancel": "Cancel",
  "save": "Save",
  "delete": "Delete",
  "edit": "Edit",
  "search": "Search",
  "reset": "Reset",
  "add": "Add",
  "view": "View",
  "export": "Export",
  "import": "Import",
  "action": "Action",
  "status": "Status",
  "create_time": "Created At",
  "update_time": "Updated At",
  "operation": {
    "success": "Operation successful",
    "failed": "Operation failed",
    "confirm_delete": "Are you sure you want to delete this record?"
  }
}
```

#### 2.1.4 核心实现文件

**需新建文件**:
```
iboot-portal/src/
├── locales/
│   ├── index.ts           # vue-i18n 配置
│   ├── zh-CN/             # 中文语言包
│   └── en-US/             # 英文语言包
├── composables/
│   └── useLocale.ts       # 语言切换 Hook
└── components/
    └── LocalePicker.vue   # 语言切换组件
```

**需修改文件**:
```
iboot-portal/src/
├── main.ts                # 引入 i18n 插件
├── App.vue                # 设置 i18n 上下文
├── stores/
│   └── app.ts             # 增加语言设置状态
├── views/**/*.vue         # 所有视图组件（替换硬编码文本）
└── components/**/*.vue    # 所有组件（替换硬编码文本）
```

---

### 2.2 后端国际化方案

#### 2.2.1 技术选型

| 组件 | 技术方案 | 说明 |
|------|----------|------|
| 国际化框架 | Spring MessageSource | Spring 原生支持 |
| 语言解析器 | AcceptHeaderLocaleResolver | 基于 Accept-Language 请求头 |
| 语言包格式 | properties | Spring 标准格式 |
| 语言切换 | REST API | 提供用户设置语言接口 |

#### 2.2.2 消息文件结构设计

```
iboot-admin/src/main/resources/
├── i18n/
│   ├── messages.properties         # 默认消息（中文）
│   ├── messages_zh_CN.properties   # 中文消息
│   ├── messages_en_US.properties   # 英文消息
│   ├── exception/
│   │   ├── exceptions.properties
│   │   ├── exceptions_zh_CN.properties
│   │   └── exceptions_en_US.properties
│   ├── validation/
│   │   ├── validation.properties
│   │   ├── validation_zh_CN.properties
│   │   └── validation_en_US.properties
│   └── business/
│       ├── business.properties
│       ├── business_zh_CN.properties
│       └── business_en_US.properties
└── ...
```

#### 2.2.3 消息文件示例

**messages.properties**:
```properties
# 通用响应
result.success=操作成功
result.error=操作失败
result.not_found=资源不存在
result.unauthorized=未授权访问
result.forbidden=禁止访问

# 异常消息
exception.business=业务异常
exception.system=系统异常
exception.argument=参数异常

# 用户相关
user.login.success=登录成功
user.login.failed=用户名或密码错误
user.logout.success=退出成功
user.not_found=用户不存在
user.password.error=密码错误
```

**messages_en_US.properties**:
```properties
# Common Responses
result.success=Operation successful
result.error=Operation failed
result.not_found=Resource not found
result.unauthorized=Unauthorized
result.forbidden=Forbidden

# Exception Messages
exception.business=Business exception
exception.system=System exception
exception.argument=Argument exception

# User Related
user.login.success=Login successful
user.login.failed=Invalid username or password
user.logout.success=Logout successful
user.not_found=User not found
user.password.error=Invalid password
```

#### 2.2.4 核心实现文件

**需新建文件**:
```
iboot-admin/src/main/java/com/iboot/admin/
├── common/i18n/
│   ├── I18nMessage.java         # 国际化消息封装
│   └── I18nUtil.java            # 国际化工具类
└── infrastructure/config/
    └── I18nConfig.java          # i18n 配置类

iboot-admin/src/main/resources/
├── i18n/
│   ├── messages.properties
│   ├── messages_zh_CN.properties
│   └── messages_en_US.properties
└── ...
```

**需修改文件**:
```
iboot-admin/src/main/java/com/iboot/admin/
├── common/result/Result.java    # 增加国际化消息支持
├── common/exception/            # 异常消息国际化
├── infrastructure/config/
│   ├── WebConfig.java           # 增加 LocaleResolver 配置
│   └── SecurityConfig.java      # 语言切换接口白名单
└── interfaces/controller/
    └── I18nController.java      # 语言切换 API
```

---

## 3. 详细实施步骤

### 阶段一：后端国际化基础（2-3 天）

#### 3.1.1 创建国际化配置类

**文件**: `infrastructure/config/I18nConfig.java`

```java
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages", "i18n/exception/exceptions", "i18n/business/business");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return resolver;
    }
}
```

#### 3.1.2 创建国际化工具类

**文件**: `common/i18n/I18nUtil.java`

```java
@Component
public class I18nUtil {

    @Autowired
    private MessageSource messageSource;

    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    public String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }
}
```

#### 3.1.3 修改 Result 类支持国际化

**文件**: `common/result/Result.java`

增加支持消息码的构造方法：
```java
public static <T> Result<T> success(String code, T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setData(data);
    result.setMessage(I18nUtil.getMessage(code)); // 通过消息码获取国际化消息
    return result;
}
```

#### 3.1.4 创建语言切换 API

**文件**: `interfaces/controller/I18nController.java`

```java
@RestController
@RequestMapping("/api/i18n")
public class I18nController {

    @PostMapping("/locale")
    public Result<Map<String, String>> changeLocale(
            @RequestParam String locale,
            HttpServletResponse response) {
        // 设置 Cookie
        Cookie cookie = new Cookie("org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver.locale", locale);
        cookie.setPath("/");
        cookie.setMaxAge(31536000); // 1 年
        response.addCookie(cookie);

        Map<String, String> data = Map.of(
            "locale", locale,
            "message", "Language changed successfully"
        );
        return Result.success(data);
    }

    @GetMapping("/locale")
    public Result<String> getCurrentLocale(@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String locale = acceptLanguage != null ? acceptLanguage.split(",")[0] : "zh-CN";
        return Result.success(locale);
    }
}
```

#### 3.1.5 异常消息国际化

**文件**: `common/exception/BusinessException.java`

修改为支持消息码：
```java
public BusinessException(String messageCode, Object... args) {
    super(I18nUtil.getMessage(messageCode, args));
    this.code = 500;
    this.message = I18nUtil.getMessage(messageCode, args);
}
```

---

### 阶段二：前端国际化基础（2-3 天）

#### 3.2.1 安装 vue-i18n

```bash
cd iboot-portal
npm install vue-i18n@9
```

#### 3.2.2 创建 i18n 配置

**文件**: `src/locales/index.ts`

```typescript
import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN/index'
import enUS from './en-US/index'

// 默认语言
const defaultLocale = localStorage.getItem('locale') || 'zh-CN'

const i18n = createI18n({
  legacy: false, // Composition API 模式
  locale: defaultLocale,
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  },
  globalInjection: true
})

export default i18n
```

#### 3.2.3 创建语言包索引

**文件**: `src/locales/zh-CN/index.ts`

```typescript
import common from './common.json'
import login from './login.json'
// ... 其他模块

export default {
  common,
  login,
  // ...
}
```

#### 3.2.4 创建语言切换 Composable

**文件**: `src/composables/useLocale.ts`

```typescript
import { useI18n } from 'vue-i18n'
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import elementPlusLocales from 'element-plus/es/locale'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'

const elementLocales = {
  'zh-CN': zhCn,
  'en-US': en
}

export function useLocale() {
  const { locale, t } = useI18n()
  const appStore = useAppStore()

  const currentLocale = computed(() => locale.value)

  const elementLocale = computed(() =>
    elementLocales[locale.value as keyof typeof elementLocales] || zhCn
  )

  const setLocale = (newLocale: string) => {
    locale.value = newLocale
    localStorage.setItem('locale', newLocale)
    appStore.setLocale(newLocale)
  }

  const changeLocale = async (newLocale: string) => {
    setLocale(newLocale)
    // 调用后端 API 保存用户语言偏好
    try {
      await api.changeLocale(newLocale)
    } catch (e) {
      console.error('Failed to save locale preference:', e)
    }
  }

  return {
    currentLocale,
    elementLocale,
    setLocale,
    changeLocale,
    t
  }
}
```

#### 3.2.5 创建语言切换组件

**文件**: `src/components/LocalePicker.vue`

```vue
<template>
  <el-dropdown @command="handleLocaleChange">
    <span class="locale-trigger">
      <el-icon><Globe /></el-icon>
      {{ currentLabel }}
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in locales"
          :key="item.value"
          :command="item.value"
        >
          {{ item.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useLocale } from '@/composables/useLocale'

const { currentLocale, changeLocale } = useLocale()

const locales = [
  { label: '简体中文', value: 'zh-CN' },
  { label: 'English', value: 'en-US' }
]

const currentLabel = computed(() => {
  return locales.find(l => l.value === currentLocale.value)?.label || '简体中文'
})

const handleLocaleChange = (locale: string) => {
  changeLocale(locale)
}
</script>
```

#### 3.2.6 修改 main.ts

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import i18n from './locales'  // 引入 i18n
import './styles/global.css'
import { permission } from './directives/permission'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)  // 使用 i18n 插件
app.use(ElementPlus, {
  locale: i18n.global.messages['zh-CN'], // 初始语言
})
app.directive('permission', permission)

app.mount('#app')
```

#### 3.2.7 修改 App.vue 实现 Element Plus 语言动态切换

```vue
<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useLocale } from '@/composables/useLocale'

const { elementLocale } = useLocale()
</script>
```

---

### 阶段三：视图组件国际化改造（3-5 天）

#### 3.3.1 登录页面改造示例

**修改前** (`LoginView.vue`):
```vue
<template>
  <div class="login-container">
    <div class="login-form">
      <h2>{{ systemName }}</h2>
      <el-form>
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" />
        </el-form-item>
        <el-button type="primary" @click="handleLogin">登录</el-button>
      </el-form>
    </div>
  </div>
</template>
```

**修改后**:
```vue
<template>
  <div class="login-container">
    <div class="login-form">
      <h2>{{ systemName }}</h2>
      <el-form>
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" :placeholder="t('login.username_placeholder')" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" :placeholder="t('login.password_placeholder')" />
        </el-form-item>
        <el-button type="primary" @click="handleLogin">{{ t('login.submit') }}</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
// ...
</script>
```

#### 3.3.2 需要改造的视图文件清单

| 模块 | 文件 | 优先级 | 预估工时 |
|------|------|--------|----------|
| 认证 | LoginView.vue, RegisterView.vue | P0 | 2 小时 |
| 仪表板 | DashboardView.vue, DashboardHomeView.vue | P0 | 2 小时 |
| 系统管理 | UserView.vue, RoleView.vue, MenuView.vue, DictView.vue, ConfigView.vue | P0 | 4 小时 |
| 组织管理 | DeptView.vue, PostView.vue, OrgChartView.vue | P1 | 3 小时 |
| 安全管理 | LoginLogView.vue, OperateLogView.vue, AuditLogView.vue | P1 | 3 小时 |
| 消息管理 | NoticeView.vue, MessageCenterView.vue | P2 | 2 小时 |
| 文件管理 | FileManageView.vue | P2 | 2 小时 |
| 监控管理 | OnlineUserView.vue, ServerMonitorView.vue, RedisMonitorView.vue, SqlMonitorView.vue, JobView.vue | P2 | 4 小时 |
| 分析报表 | ReportView.vue, UsageView.vue | P3 | 2 小时 |

---

### 阶段四：测试与优化（2-3 天）

#### 3.4.1 功能测试清单

- [ ] 语言切换功能正常
- [ ] 语言偏好持久化（localStorage/Cookie）
- [ ] Element Plus 组件语言正确显示
- [ ] 日期、数字格式化正确
- [ ] 后端 API 返回消息语言正确
- [ ] 异常消息语言正确
- [ ] 表单验证消息语言正确

#### 3.4.2 测试场景

1. **切换语言后页面刷新**：语言设置应保持
2. **切换语言后重新登录**：语言设置应从后端获取
3. **浏览器语言为英文**：首次访问应自动使用英文
4. **混合语言场景**：某些翻译缺失时使用 fallbackLocale

#### 3.4.3 性能优化

- 懒加载语言包（按需加载模块翻译）
- 压缩语言包文件
- 缓存语言包到 localStorage

---

## 4. 工作量评估

### 4.1 总体工作量

| 阶段 | 任务 | 预估工时 | 说明 |
|------|------|----------|------|
| **阶段一** | 后端国际化基础 | 16-24 小时 | 2-3 工作日 |
| | - I18nConfig 配置类 | 2 小时 | |
| | - I18nUtil 工具类 | 2 小时 | |
| | - Result 类改造 | 2 小时 | |
| | - I18nController API | 2 小时 | |
| | - 异常国际化 | 2 小时 | |
| | - 消息文件编写（中/英） | 6-12 小时 | 主要工作量 |
| **阶段二** | 前端国际化基础 | 16-24 小时 | 2-3 工作日 |
| | - 安装 vue-i18n | 0.5 小时 | |
| | - i18n 配置 | 2 小时 | |
| | - 语言包结构设计 | 2 小时 | |
| | - useLocale Hook | 2 小时 | |
| | - LocalePicker 组件 | 2 小时 | |
| | - main.ts/App.vue 改造 | 2 小时 | |
| | - 语言包翻译（中/英） | 6-12 小时 | 主要工作量 |
| **阶段三** | 视图组件改造 | 24-32 小时 | 3-4 工作日 |
| | - 认证模块 | 2 小时 | |
| | - 仪表板 | 2 小时 | |
| | - 系统管理 | 4 小时 | |
| | - 组织管理 | 3 小时 | |
| | - 安全管理 | 3 小时 | |
| | - 消息管理 | 2 小时 | |
| | - 文件管理 | 2 小时 | |
| | - 监控管理 | 4 小时 | |
| | - 分析报表 | 2 小时 | |
| **阶段四** | 测试与优化 | 16-24 小时 | 2-3 工作日 |
| | - 功能测试 | 8 小时 | |
| | - 兼容性测试 | 4 小时 | |
| | - 性能优化 | 4 小时 | |
| | - Bug 修复 | 4-8 小时 | |
| **总计** | | **72-104 小时** | **约 9-13 工作日** |

### 4.2 人力资源建议

- **前端开发**: 1 人（负责阶段二、三）
- **后端开发**: 1 人（负责阶段一）
- **测试人员**: 0.5 人（负责阶段四）
- **翻译人员**: 可考虑外包英文翻译

---

## 5. 风险与应对措施

### 5.1 技术风险

| 风险项 | 可能性 | 影响 | 应对措施 |
|--------|--------|------|----------|
| Element Plus 某些组件翻译缺失 | 低 | 低 | 使用 fallbackLocale，提交 issue |
| 日期/数字格式化问题 | 中 | 中 | 使用 dayjs 和 @intlify/core |
| 后端消息码管理混乱 | 中 | 中 | 建立消息码命名规范 |
| 语言包体积过大 | 低 | 低 | 按需加载，压缩优化 |

### 5.2 项目风险

| 风险项 | 可能性 | 影响 | 应对措施 |
|--------|--------|------|----------|
| 翻译质量不佳 | 中 | 中 | 使用专业翻译服务校对 |
| 视图改造遗漏 | 中 | 低 | 建立检查清单，逐一核对 |
| 时间超出预期 | 中 | 中 | 分阶段实施，优先核心页面 |
| 语言切换后部分文本未更新 | 中 | 低 | 加强测试，建立反馈机制 |

---

## 6. 扩展规划

### 6.1 未来可支持的语言

| 语言 | 代码 | 优先级 | 使用场景 |
|------|------|--------|----------|
| 繁体中文 | zh-TW | P1 | 港澳台用户 |
| 日语 | ja-JP | P2 | 日本用户 |
| 韩语 | ko-KR | P3 | 韩国用户 |
| 法语 | fr-FR | P3 | 法语区用户 |
| 西班牙语 | es-ES | P3 | 西班牙语区用户 |

### 6.2 扩展功能

1. **用户语言偏好同步**：登录时将本地语言偏好同步到后端
2. **按模块懒加载**：大型语言包按模块拆分，按需加载
3. **管理后台翻译管理**：提供翻译管理界面，支持在线编辑
4. **机器翻译辅助**：集成翻译 API 自动翻译新增文本

---

## 7. 决策建议

### 7.1 建议实施的情况

- 系统有海外用户或计划拓展海外市场
- 企业有国际化形象需求
- 团队有足够的时间和人力资源
- 项目处于成长期，有持续迭代计划

### 7.2 建议暂缓实施的情况

- 系统仅面向国内用户
- 团队资源紧张，有紧急业务需求
- 项目即将进入维护期

### 7.3 推荐实施方案

**建议采用分阶段渐进式实施**：

1. **第一期**（1-2 周）：后端国际化基础 + 前端核心页面（登录、仪表板、系统管理）
2. **第二期**（1-2 周）：前端其他页面改造
3. **第三期**（1 周）：测试优化 + 其他语言支持

---

## 8. 附录

### 8.1 消息码命名规范

```
模块名。功能名。具体项
例：
user.login.success
user.login.failed
user.logout.success
system.user.add.success
system.user.delete.confirm
```

### 8.2 参考资源

- [vue-i18n 官方文档](https://vue-i18n.intlify.dev/)
- [Spring 国际化文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#context-functionality-messagesource)
- [Element Plus 国际化](https://element-plus.org/zh-CN/guide/i18n.html)
- [INTL NumberFormat](https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat)

### 8.3 项目文件清单总览

**新建文件**（约 40+ 文件）:
```
iboot-portal/src/locales/        # 2 个语言 x 8 模块 = 16+ 文件
iboot-admin/src/main/resources/i18n/  # 3 个语言 x 3 类型 = 9+ 文件
iboot-portal/src/composables/useLocale.ts
iboot-portal/src/components/LocalePicker.vue
iboot-admin/src/main/java/.../I18nConfig.java
iboot-admin/src/main/java/.../I18nUtil.java
iboot-admin/src/main/java/.../I18nController.java
```

**修改文件**（约 50+ 文件）:
```
iboot-portal/src/main.ts
iboot-portal/src/App.vue
iboot-portal/src/views/**/*.vue    # 30+ 视图组件
iboot-portal/src/stores/app.ts
iboot-admin/src/main/java/.../Result.java
iboot-admin/src/main/java/.../WebConfig.java
iboot-admin/src/main/java/.../BusinessException.java
```

---

**文档版本**: v1.0
**创建日期**: 2026-03-13
**最后更新**: 2026-03-13
**负责人**: [待指定]
