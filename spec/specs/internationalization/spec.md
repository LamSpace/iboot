# Internationalization Specification

## Overview

iBoot Admin 支持多语言国际化（i18n），包括简体中文和英文。系统使用 vue-i18n v9+ 进行前端国际化，使用 Spring MessageSource 进行后端国际化。

---

## Requirements

### Requirement: Frontend Language Pack Structure

WHEN the application loads,
the system SHALL provide modular language pack structure for Vue 3.

#### Structure:
```
iboot-portal/src/locales/
├── zh-CN/
│   ├── common.json        # Common vocabulary (confirm, cancel, save, etc.)
│   ├── login.json         # Login page translations
│   ├── register.json      # Register page translations
│   ├── dashboard.json     # Dashboard translations
│   ├── system/            # System management module
│   │   ├── user.json
│   │   ├── role.json
│   │   ├── menu.json
│   │   ├── dict.json
│   │   └── config.json
│   ├── organization/      # Organization management module
│   │   ├── dept.json
│   │   ├── post.json
│   │   └── org-chart.json
│   ├── security/          # Security management module
│   │   ├── login-log.json
│   │   ├── operate-log.json
│   │   └── audit-log.json
│   ├── monitor/           # Monitoring management module
│   │   ├── online-user.json
│   │   ├── server.json
│   │   ├── redis.json
│   │   ├── sql.json
│   │   └── job.json
│   ├── message/           # Message management module
│   │   ├── notice.json
│   │   └── message-center.json
│   ├── file/              # File management module
│   │   └── file-manage.json
│   └── analysis/          # Analysis report module
│       ├── report.json
│       └── usage.json
├── en-US/
│   └──（same structure as zh-CN）
└── index.ts               # i18n configuration entry

#### Scenario: Language Pack Module Loading
GIVEN user loads the dashboard page
WHEN the dashboard module is activated
THEN the system SHALL load the current language's `dashboard.json`
AND cache it for subsequent requests

---

### Requirement: Vue I18n Configuration

WHEN the application initializes,
the system SHALL configure vue-i18n v9+ supporting Composition API.

#### Scenario: Initialize i18n Settings
GIVEN Vue application is starting
WHEN `main.ts` initializes the Vue app
THEN the system SHALL install vue-i18n plugin
AND set default locale from localStorage or browser preference
AND configure fallbackLocale to zh-CN

#### Scenario: Composition API Usage
GIVEN a Vue component needs translations
WHEN the component uses `useI18n()` composable
THEN the system SHALL provide `t()` function for translations
AND provide `locale` ref to get current language

---

### Requirement: Language Switcher Component

WHEN user views any page with the language switcher,
the system SHALL provide UI component for language switching.

#### Scenario: User Switches Language
GIVEN user is viewing any page with language switcher
WHEN user clicks language dropdown
AND selects "English" (en-US)
THEN the system SHALL:
  - Update vue-i18n locale to en-US
  - Update Element Plus locale to English
  - Save preference to localStorage
  - Call backend API to persist preference
  - Re-render all translated text on the page

#### Scenario: Language Switcher Display
GIVEN current language is zh-CN
WHEN language switcher renders
THEN it SHALL display "简体中文" as current selection
AND display "English" as available option

GIVEN current language is en-US
WHEN language switcher renders
THEN it SHALL display "English" as current selection
AND display "简体中文" as available option

---

### Requirement: Backend MessageSource Configuration

WHEN Spring application context starts,
the system SHALL configure Spring MessageSource for backend i18n.

#### Scenario: MessageSource Bean Initialization
GIVEN Spring application context is starting
WHEN `I18nConfig` is processed
THEN the system SHALL create `ResourceBundleMessageSource` bean
AND configure basenames: `i18n/messages`, `i18n/exception/exceptions`, `i18n/business/business`
AND set default encoding to UTF-8
AND set cache timeout to 3600 seconds

#### Scenario: LocaleResolver Configuration
GIVEN HTTP request arrives
WHEN `LocaleResolver` is invoked
THEN the system SHALL parse language from `Accept-Language` header
AND default to Simplified Chinese (zh-CN) if not specified

---

### Requirement: Backend Language Switch API

WHEN authenticated user requests language change,
the system SHALL provide REST API for managing language preferences.

#### Scenario: Change User Language
GIVEN authenticated user
WHEN user sends POST to `/api/i18n/locale?locale=en-US`
THEN the system SHALL:
  - Validate locale parameter
  - Set Cookie with name `org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver.locale`
  - Set cookie path to "/"
  - Set cookie max age to 31536000 (1 year)
  - Return success response with current locale

**Request:**
```http
POST /api/i18n/locale?locale=en-US
Cookie: JSESSIONID=xxx
```

**Response:**
```json
{
  "code": 200,
  "message": "Language changed successfully",
  "data": {
    "locale": "en-US"
  }
}
```

#### Scenario: Get Current Locale
GIVEN any client request
WHEN client sends GET to `/api/i18n/locale`
THEN the system SHALL return current locale from `Accept-Language` header

**Response:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "zh-CN"
}
```

---

### Requirement: I18nUtil Utility Class

WHEN Spring-managed component needs localized message,
the system SHALL provide utility class for getting i18n messages.

#### Scenario: Get Message in Current Locale
GIVEN Spring-managed component
WHEN component calls `i18nUtil.getMessage("user.login.success")`
THEN the system SHALL:
  - Get current locale from `LocaleContextHolder`
  - Look up message code in MessageSource
  - Return localized message string
  - Return message code itself as fallback if translation not found

#### Scenario: Get Parameterized Message
GIVEN message code contains placeholders
WHEN component calls `i18nUtil.getMessage("user.welcome", username)`
THEN the system SHALL replace `{0}` with username
AND return formatted message

---

### Requirement: Result Class Internationalization

WHEN controller returns response,
the system SHALL support message-code-based responses in `Result` class.

#### Scenario: Success Response with Message Code
GIVEN controller method returns success
WHEN method calls `Result.success("user.login.success", data)`
THEN the system SHALL:
  - Look up message code "user.login.success" in current locale
  - Set response message to localized text (e.g., "登录成功" or "Login successful")
  - Return standard Result structure with code, message and data

#### Scenario: Error Response with Message Code
GIVEN controller method returns error
WHEN method calls `Result.error("user.login.failed")`
THEN the system SHALL look up message code
AND return localized error message

---

### Requirement: Exception Message Internationalization

WHEN exception is thrown,
the system SHALL support internationalized exception messages.

#### Scenario: BusinessException with Message Code
GIVEN service throws `new BusinessException("user.not_found")`
WHEN exception is caught by global exception handler
THEN the system SHALL:
  - Look up message code in current locale
  - Return localized error response to client

**Response (zh-CN):**
```json
{
  "code": 500,
  "message": "用户不存在"
}
```

**Response (en-US):**
```json
{
  "code": 500,
  "message": "User not found"
}
```

---

### Requirement: Element Plus Locale Integration

WHEN user views Element Plus components,
the system SHALL integrate Element Plus built-in i18n for component translations.

#### Scenario: Element Plus DatePicker Translation
GIVEN user views page with Element Plus DatePicker
WHEN language is set to en-US
THEN DatePicker SHALL display English labels (Sun, Mon, Tue, etc.)
AND English month names (January, February, etc.)

#### Scenario: Element Plus Table Translation
GIVEN user views page with Element Plus Table
WHEN language is set to en-US
THEN table SHALL display "No Data" when empty
AND pagination text SHALL display "Total {total} items"

---

## Implementation Details

### Frontend Language Files

**Location:** `iboot-portal/src/locales/`

**Supported Languages:**
- zh-CN (Simplified Chinese) - Default and fallback
- en-US (English)

### Backend Message Bundles

**Location:** `iboot-admin/src/main/resources/i18n/`

**Bundles:**
- `messages.properties` - Default (Chinese)
- `messages_zh_CN.properties` - Chinese
- `messages_en_US.properties` - English
- `exceptions.properties` - Exception messages (Chinese)
- `exceptions_en_US.properties` - Exception messages (English)
- `business.properties` - Business messages (Chinese)
- `business_en_US.properties` - Business messages (English)

### Key Classes

**Backend:**
- `I18nConfig.java` - MessageSource and LocaleResolver configuration
- `I18nUtil.java` - Utility class for getting messages
- `I18nController.java` - REST API for language switching
- `I18nMessage.java` - Wrapper for i18n responses

**Frontend:**
- `locales/index.ts` - Vue I18n configuration
- `composables/useLocale.ts` - Composable for locale management
- `components/LocalePicker.vue` - Language switcher UI component

---

## MODIFIED Requirements

### Requirement: User Authentication Flow

WHEN authentication completes,
the system SHALL return localized messages based on user's language preference.

**Previous Behavior:**
- Login success always returns "登录成功" (Chinese)
- Login failure always returns "用户名或密码错误" (Chinese)

**Updated Behavior:**
- Login success returns message based on language:
  - zh-CN: "登录成功"
  - en-US: "Login successful"
- Login failure returns message based on language:
  - zh-CN: "用户名或密码错误"
  - en-US: "Invalid username or password"

#### Scenario: English Environment Login
GIVEN user has selected English (en-US) as language
WHEN user submits valid credentials
THEN the system SHALL return English success message
AND the message SHALL be "Login successful"

---

### Requirement: Global Exception Handler

WHEN exception is handled,
the system SHALL return exception message in user's preferred language.

**Previous Behavior:**
- All exception messages are hardcoded in Chinese

**Updated Behavior:**
- Exception messages are looked up from MessageSource based on request locale

#### Scenario: Business Exception in English Environment
GIVEN business rule violation occurs
WHEN system throws BusinessException with message code "order.insufficient_stock"
AND user language is en-US
THEN response message SHALL be "Insufficient stock available"

---

## REMOVED Requirements

### Requirement: Hardcoded Chinese Messages in Result Class

The system SHALL NOT use hardcoded Chinese messages in `Result` class.

**Removed Behavior:**
```java
// Old: Hardcoded Chinese
public static <T> Result<T> success(T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setData(data);
    result.setMessage("操作成功"); // Removed: Hardcoded Chinese
    return result;
}
```

**Replaced With:**
```java
// New: Message-code-based internationalization
public static <T> Result<T> success(String code, T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setData(data);
    result.setMessage(I18nUtil.getMessage(code)); // Locale-aware lookup
    return result;
}
```

---

### Requirement: Hardcoded Chinese Placeholders in View Components

The system SHALL NOT use hardcoded Chinese placeholders in Vue components.

**Removed Behavior:**
```vue
<!-- Old: Hardcoded Chinese -->
<el-input v-model="form.username" placeholder="用户名" />
<el-button type="primary">登录</el-button>
```

**Replaced With:**
```vue
<!-- New: Translation function -->
<el-input v-model="form.username" :placeholder="t('login.username_placeholder')" />
<el-button type="primary">{{ t('login.submit') }}</el-button>
```

---

## Related Files

- `iboot-admin/src/main/java/com/iboot/admin/infrastructure/config/I18nConfig.java`
- `iboot-admin/src/main/java/com/iboot/admin/infrastructure/security/JwtAuthenticationFilter.java`
- `iboot-admin/src/main/resources/i18n/` - Message bundles
- `iboot-portal/src/locales/` - Frontend language packs
- `iboot-portal/src/components/LocalePicker.vue` - Language switcher
- `iboot-portal/src/composables/useLocale.ts` - Locale composable
