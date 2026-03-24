# 06-API 集成

## 目录

1. [API 设计规范](#api 设计规范)
2. [认证与授权](#认证与授权)
3. [请求封装](#请求封装)
4. [接口列表](#接口列表)
5. [数据格式](#数据格式)
6. [错误处理](#错误处理)
7. [最佳实践](#最佳实践)
8. [后端接口实现](#后端接口实现)

## API 设计规范

### 6.1 RESTful 风格

iBoot 后端 API 遵循 RESTful 设计风格，使用 HTTP 方法表达对资源的操作：

| HTTP 方法 | 操作 | 示例 |
|-----------|------|------|
| GET | 查询资源 | GET /api/users |
| POST | 创建资源 | POST /api/users |
| PUT | 更新资源（全量） | PUT /api/users/1 |
| PATCH | 更新资源（部分） | PATCH /api/users/1 |
| DELETE | 删除资源 | DELETE /api/users/1 |

### 6.2 URL 命名规范

- 使用小写字母
- 单词间使用连字符（-）分隔
- 使用名词复数形式
- 避免在 URL 中出现动词

**示例：**
```
/api/users          # 用户列表
/api/users/1        # ID 为 1 的用户
/api/user-roles     # 用户角色（而非 /api/getUserRoles）
```

### 6.3 细粒度 API 版本控制

#### 版本控制方式

iBoot 项目采用 Spring Framework 7.0+ 的细粒度 API 版本控制，通过请求头指定 API 版本号：

| 特性 | 说明 |
|------|------|
| 版本请求头 | `Accept-Version: 1` |
| 注解方式 | `@GetMapping(version = "1", ...)` |
| 版本解析器 | `HeaderApiVersionResolver` |
| 版本可选 | 支持无版本号请求（如 SSE 端点） |

**请求示例：**
```bash
# 使用 Accept-Version 请求头指定版本号
GET /api/user/list
Accept-Version: 1
Authorization: Bearer <token>

# 无版本号请求（兼容旧版本）
GET /api/user/list
Authorization: Bearer <token>
```

#### 与传统 URL 路径版本控制的对比

| 方式 | 示例 | 优点 | 缺点 |
|------|------|------|------|
| 请求头版本控制 | `Accept-Version: 1` + `/api/users` | URL 简洁，便于维护 | 调试不够直观 |
| URL 路径版本控制 | `/api/v1/users` | 直观可见 | URL 冗余，版本升级需改路由 |

iBoot 采用请求头版本控制方式，保持 URL 的稳定性。

#### 版本控制配置

```java
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useVersionResolver(new HeaderApiVersionResolver("Accept-Version"))
            .setVersionRequired(false);  // 不强制要求版本号，允许没有版本号的请求（如 SSE 连接）
    }
}
```

配置说明：
- `HeaderApiVersionResolver("Accept-Version")`：通过 `Accept-Version` 请求头解析版本号
- `setVersionRequired(false)`：允许无版本号的请求，用于兼容旧版本或特殊端点（如 SSE）

#### Controller 实现示例

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    // API v1 版本 - 用户列表查询
    @GetMapping(version = "1", value = "/list")
    public Result<PageResult<UserResponse>> list(...) {
        // API v1 版本实现
    }

    // API v2 版本 - 用户列表查询（扩展的数据结构）
    @GetMapping(version = "2", value = "/list")
    public Result<PageResult<UserResponseV2>> listV2(...) {
        // API v2 版本实现（返回扩展的数据结构）
    }

    // 无版本号接口 - 适用于所有版本
    @PostMapping
    public Result<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        // 不指定版本号，适用于所有版本
    }
}
```

#### 多版本共存策略

当需要升级 API 时，可以保留旧版本接口并同时提供新版本接口：

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    // v1 版本：返回基础用户信息
    @GetMapping(version = "1", value = "/{id}")
    public Result<UserResponseV1> getByIdV1(@PathVariable Long id) {
        User user = userApplicationService.getUserById(id);
        return Result.success(userMapper.toResponseV1(user));
    }

    // v2 版本：返回扩展用户信息（包含部门、岗位详情）
    @GetMapping(version = "2", value = "/{id}")
    public Result<UserResponseV2> getByIdV2(@PathVariable Long id) {
        User user = userApplicationService.getUserById(id);
        return Result.success(userMapper.toResponseV2(user));
    }
}
```

**请求示例：**
```bash
# 请求 v1 版本
curl -H "Accept-Version: 1" http://localhost:8080/api/user/1

# 请求 v2 版本
curl -H "Accept-Version: 2" http://localhost:8080/api/user/1
```

#### 版本兼容性说明

- **向后兼容**：新版本的接口应尽量保持向后兼容
- **默认版本**：未指定版本号时，使用最新版本接口
- **废弃策略**：旧版本接口应提前公告废弃时间，建议至少保留 3 个月

### 6.4 查询参数

对于列表查询，支持以下通用参数：

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| page | integer | 1 | 页码 |
| size | integer | 10 | 每页数量 |
| sort | string | - | 排序字段 |
| order | string | desc | 排序方向（asc/desc） |

**示例：**
```
GET /api/users?page=1&size=20&sort=createTime&order=desc
```

## 认证与授权

### 6.5 JWT 认证流程

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  客户端  │     │  认证层  │     │  业务层  │     │  数据层  │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ 1.POST /login │               │               │
     │──────────────►│               │               │
     │               │               │               │
     │               │2.验证用户信息 │               │
     │               │──────────────►│               │
     │               │               │               │
     │               │3.生成 JWT     │               │
     │               │◄──────────────│               │
     │               │               │               │
     │ 4.返回 Token  │               │               │
     │◄──────────────│               │               │
     │               │               │               │
     │5.后续请求携带 Token │               │               │
     │──────────────►│               │               │
     │               │               │               │
     │               │6.验证 Token   │               │
     │               │──────────────►│               │
     │               │               │               │
     │               │               │7.业务处理     │
     │               │               │──────────────►│
     │               │               │               │
     │ 8.返回结果    │               │               │
     │◄──────────────│               │               │
```

### 6.6 Token 格式

**请求头格式：**
```
Authorization: Bearer <token>
```

**Token 结构：**
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user_id",
    "username": "admin",
    "roles": ["admin"],
    "iat": 1699999999,
    "exp": 1700003599
  },
  "signature": "xxx"
}
```

### 6.7 Token 配置

| 配置项 | 默认值 | 描述 |
|--------|--------|------|
| token 过期时间 | 1 小时 | access_token 有效期 |
| refresh_token 过期时间 | 7 天 | 刷新令牌有效期 |
| 自动续期 | 支持 | 请求活跃时自动续期 |

### 6.8 权限注解

后端使用 Spring Security 的权限注解：

| 注解 | 描述 | 示例 |
|------|------|------|
| @PreAuthorize | 方法级权限控制 | @PreAuthorize("hasRole('ADMIN')") |
| @Secured | 角色控制 | @Secured("ROLE_ADMIN") |
| @PermitAll | 允许所有用户访问 | @PermitAll |
| @AuthenticationPrincipal | 获取当前用户 | @AuthenticationPrincipal UserDetails user |

## 请求封装

### 6.9 前端 Axios 封装

**基础配置：**
```typescript
// utils/request.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 50000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      // TODO: 处理 token 过期等特殊情况
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    console.error('Response error:', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

### 6.10 API 模块封装

**示例 - 用户管理 API：**
```typescript
// api/user.ts
import request from '@/utils/request'

export function getUserList(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

export function getUserById(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}
```

## 接口列表

### 6.11 认证接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /auth/login | POST | 用户登录 |
| /auth/logout | POST | 用户登出 |
| /auth/refresh | POST | 刷新 Token |
| /auth/captcha | GET | 获取验证码 |

### 6.11.1 国际化接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /i18n/locale | GET | 获取当前语言 |
| /i18n/locale | POST | 切换语言 |

**切换语言请求示例：**
```bash
POST /api/i18n/locale?locale=en-US
```

**切换语言响应示例：**
```json
{
  "code": 200,
  "message": "Language changed successfully",
  "data": {
    "locale": "en-US"
  }
}
```

**获取当前语言响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "zh-CN"
}
```

**说明：**
- 该接口无需认证，可在未登录状态下切换语言
- 语言设置会通过 Cookie 持久化（有效期 1 年）
- 前端同时会将语言偏好保存到 localStorage
- 支持的語言：`zh-CN`（简体中文）、`en-US`（英文）

**登录请求示例：**
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123",
  "captcha": "1234",
  "rememberMe": true
}
```

**登录响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "xxx",
    "expiresIn": 3600,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员"
    }
  }
}
```

### 6.12 用户接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /users | GET | 用户列表（分页） |
| /users/{id} | GET | 用户详情 |
| /users | POST | 创建用户 |
| /users/{id} | PUT | 更新用户 |
| /users/{id} | DELETE | 删除用户 |
| /users/{id}/password | PATCH | 修改密码 |
| /users/{id}/status | PATCH | 修改状态 |
| /users/me | GET | 当前用户信息 |

### 6.13 角色接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /roles | GET | 角色列表 |
| /roles/{id} | GET | 角色详情 |
| /roles | POST | 创建角色 |
| /roles/{id} | PUT | 更新角色 |
| /roles/{id} | DELETE | 删除角色 |
| /roles/{id}/menus | PUT | 分配菜单权限 |
| /roles/{id}/depts | PUT | 分配数据权限 |

### 6.14 菜单接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /menus | GET | 菜单列表 |
| /menus/tree | GET | 菜单树 |
| /menus/{id} | GET | 菜单详情 |
| /menus | POST | 创建菜单 |
| /menus/{id} | PUT | 更新菜单 |
| /menus/{id} | DELETE | 删除菜单 |

### 6.15 部门接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /depts | GET | 部门列表 |
| /depts/tree | GET | 部门树 |
| /depts/{id} | GET | 部门详情 |
| /depts | POST | 创建部门 |
| /depts/{id} | PUT | 更新部门 |
| /depts/{id} | DELETE | 删除部门 |

### 6.16 字典接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /dict/types | GET | 字典类型列表 |
| /dict/types/{id} | GET | 字典类型详情 |
| /dict/types | POST | 创建字典类型 |
| /dict/types/{id} | PUT | 更新字典类型 |
| /dict/types/{id} | DELETE | 删除字典类型 |
| /dict/data | GET | 字典数据列表 |
| /dict/data/type/{type} | GET | 根据类型查询字典数据 |

### 6.17 文件接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /files | POST | 文件上传 |
| /files | GET | 文件列表 |
| /files/{id} | GET | 文件详情 |
| /files/{id} | DELETE | 删除文件 |
| /files/{id}/download | GET | 文件下载 |
| /files/{id}/preview | GET | 文件预览 |

**文件上传示例：**

### 6.18 SSE 推送接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /push/connect | GET | 建立 SSE 连接（需要 Token 参数） |
| /push/status | GET | 获取在线状态 |

**SSE 连接示例：**

```bash
# 使用 curl 连接 SSE（t 参数传递 Token）
curl http://localhost:8080/api/push/connect?t=<YOUR_JWT_TOKEN>
```

**SSE 连接响应格式（text/event-stream）：**
```
id: conn-xxx
event: com.iboot.push.connection.established
data: {"userId":1,"connectedAt":"2026-03-12T10:00:00","onlineCount":5}

id: push-msg-xxx
event: com.iboot.push.new.message
data: {"messageId":1,"title":"新消息","content":"Hello","messageType":"notice","priority":"0","senderId":2,"sentAt":"2026-03-12T10:00:00"}

id: push-read-xxx
event: com.iboot.push.message.read
data: {"messageId":1,"userId":1,"readAt":"2026-03-12T10:00:00"}

id: heartbeat
event: heartbeat
data: ping
```

**推送事件类型：**

| 事件类型 | 事件来源 | 数据格式 | 说明 |
|---------|---------|---------|------|
| `com.iboot.push.connection.established` | `/api/push/connect` | `{userId, connectedAt, onlineCount}` | 连接建立成功 |
| `com.iboot.push.new.message` | `/api/messages` | `{messageId, title, content, messageType, priority, senderId, sentAt}` | 新消息推送 |
| `com.iboot.push.message.read` | `/api/messages/read` | `{messageId, userId, readAt}` | 消息已读通知 |
| `com.iboot.push.system.notification` | `/api/system` | `{title, content, type}` | 系统通知 |
| `heartbeat` | `/api/push/connect` | `ping` | 心跳包（30 秒间隔） |

**前端使用示例：**

```typescript
// 建立 SSE 连接（Token 通过 URL 参数传递）
const token = useUserStore().token
const url = `${BASE_URL}/api/push/connect?t=${token}`
const eventSource = new EventSource(url)

// 监听连接建立
eventSource.addEventListener('com.iboot.push.connection.established', (event) => {
  const data = JSON.parse(event.data)
  console.log('SSE 连接已建立，在线人数:', data.onlineCount)
})

// 监听新消息推送
eventSource.addEventListener('com.iboot.push.new.message', (event) => {
  const data = JSON.parse(event.data)
  console.log('收到新消息:', data.title)
  // 更新通知铃铛 badge
  // 显示消息提示
})

// 监听消息已读通知
eventSource.addEventListener('com.iboot.push.message.read', (event) => {
  const data = JSON.parse(event.data)
  console.log('消息已读:', data.messageId)
})

// 监听心跳
eventSource.addEventListener('heartbeat', (event) => {
  console.log('收到心跳:', event.data)
})

// 错误处理（自动重连）
eventSource.onerror = (error) => {
  console.error('SSE 连接错误，准备重连...', error)
  eventSource.close()
  // 3 秒后重连
  setTimeout(() => {
    window.location.reload() // 或者重新创建 EventSource
  }, 3000)
}
```

**在线状态接口示例：**

```bash
GET /api/push/status
Authorization: Bearer <JWT_TOKEN>
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "online": true,
    "totalOnline": 5
  }
}
```

**注意事项：**

1. **认证方式**：由于浏览器原生 `EventSource` 不支持设置自定义请求头，Token 必须通过 URL 参数传递（`?t=xxx` 或 `?token=xxx`）
2. **HTTPS 要求**：使用 URL 参数传递 Token 时，生产环境必须使用 HTTPS，防止 Token 泄露
3. **重连机制**：SSE 内置自动重连，默认间隔 3 秒，可通过 `retry` 字段自定义
4. **超时设置**：默认超时时间 5 分钟，超时后自动断开，客户端需重新连接
5. **心跳保活**：服务端每 30 秒发送一次心跳，保持连接活跃

详细实现请参考 [15-SSE 推送集成](./15-SSE 推送集成.md)。

```typescript
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/files',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
```

## 数据格式

### 6.19 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1699999999999
}
```

| 字段 | 类型 | 描述 |
|------|------|------|
| code | integer | 业务状态码，200 表示成功 |
| message | string | 响应消息（支持国际化，根据 `Accept-Language` 头返回对应语言） |
| data | any | 响应数据 |
| timestamp | long | 时间戳 |

### 6.19.1 消息码响应

后端支持使用消息码返回响应，消息内容会根据用户语言自动国际化：

**请求示例：**
```java
// 后端代码示例
return Result.success("user.login.success", data);
```

**响应示例（中文环境）：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {...},
  "timestamp": 1699999999999
}
```

**响应示例（英文环境）：**
```json
{
  "code": 200,
  "message": "Login successful",
  "data": {...},
  "timestamp": 1699999999999
}
```

**错误响应示例（中文环境）：**
```json
{
  "code": 500,
  "message": "用户不存在",
  "data": null,
  "timestamp": 1699999999999
}
```

**错误响应示例（英文环境）：**
```json
{
  "code": 500,
  "message": "User not found",
  "data": null,
  "timestamp": 1699999999999
}
```

### 6.20 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "size": 10,
    "pages": 10
  },
  "timestamp": 1699999999999
}
```

| 字段 | 类型 | 描述 |
|------|------|------|
| list | array | 数据列表 |
| total | long | 总记录数 |
| page | integer | 当前页码 |
| size | integer | 每页数量 |
| pages | integer | 总页数 |

### 6.21 请求参数格式

**创建/更新请求体：**
```json
{
  "username": "newuser",
  "password": "password123",
  "nickname": "新用户",
  "email": "user@example.com",
  "deptId": 1,
  "roleIds": [1, 2]
}
```

**查询参数：**
```json
{
  "page": 1,
  "size": 10,
  "username": "admin",
  "status": 1,
  "deptId": 1
}
```

## 错误处理

### 6.22 业务状态码

| 状态码 | 描述 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或 Token 过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 6.23 业务错误码

| 错误码 | 描述 | 处理建议 |
|--------|------|----------|
| 1001 | 用户名或密码错误 | 提示用户检查 credentials |
| 1002 | 用户已被禁用 | 联系管理员 |
| 1003 | Token 无效 | 重新登录 |
| 1004 | Token 已过期 | 刷新 Token 或重新登录 |
| 1005 | 账号已登录 | 提示用户或强制下线 |
| 2001 | 参数校验失败 | 检查请求参数 |
| 2002 | 数据不存在 | 检查资源 ID |
| 2003 | 数据已存在 | 修改输入 |
| 3001 | 权限不足 | 联系管理员授权 |

### 6.24 前端错误处理

```typescript
// 全局错误处理
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      handleBusinessError(res.code, res.message)
      return Promise.reject(new Error(res.message))
    }
    return res.data
  },
  error => {
    handleHttpError(error)
    return Promise.reject(error)
  }
)

function handleBusinessError(code: number, message: string) {
  switch (code) {
    case 1003:
    case 1004:
      // Token 无效或过期，重新登录
      redirectToLogin()
      break
    case 1005:
      // 账号被顶替
      ElMessageBox.confirm('您的账号在其他地方登录，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        type: 'warning'
      }).then(() => {
        redirectToLogin()
      })
      break
    default:
      ElMessage.error(message)
  }
}

function handleHttpError(error: any) {
  switch (error.response?.status) {
    case 401:
      ElMessage.error('未授权，请重新登录')
      redirectToLogin()
      break
    case 403:
      ElMessage.error('无权限访问')
      break
    case 404:
      ElMessage.error('资源不存在')
      break
    case 500:
      ElMessage.error('服务器错误')
      break
    default:
      ElMessage.error(error.message || '网络错误')
  }
}
```

## 最佳实践

### 6.24 请求优化

**防重复提交：**
```typescript
// 使用请求队列防止重复提交
const pendingRequests = new Map()

function addPendingRequest(key: string, cancel: () => void) {
  if (pendingRequests.has(key)) {
    pendingRequests.get(key)() // 取消之前的请求
  }
  pendingRequests.set(key, cancel)
}

function removePendingRequest(key: string) {
  pendingRequests.delete(key)
}

// 在请求拦截器中使用
request.interceptors.request.use(config => {
  const key = `${config.method}:${config.url}`
  config.cancelToken = new axios.CancelToken(cancel => {
    addPendingRequest(key, cancel)
  })
  return config
})

// 在响应拦截器中移除
request.interceptors.response.use(response => {
  const key = `${response.config.method}:${response.config.url}`
  removePendingRequest(key)
  return response
})
```

### 6.25 缓存策略

**接口数据缓存：**
```typescript
const cache = new Map<string, { data: any; timestamp: number }>()
const CACHE_TTL = 5 * 60 * 1000 // 5 分钟

function getCache(key: string) {
  const item = cache.get(key)
  if (item && Date.now() - item.timestamp < CACHE_TTL) {
    return item.data
  }
  return null
}

function setCache(key: string, data: any) {
  cache.set(key, { data, timestamp: Date.now() })
}

// 使用示例
export function getDictData(type: string) {
  const cacheKey = `dict:${type}`
  const cached = getCache(cacheKey)
  if (cached) {
    return Promise.resolve(cached)
  }

  return request({
    url: `/dict/data/type/${type}`,
    method: 'get'
  }).then(data => {
    setCache(cacheKey, data)
    return data
  })
}
```

### 6.26 请求重试

```typescript
import axiosRetry from 'axios-retry'

axiosRetry(request, {
  retries: 3,
  retryDelay: axiosRetry.exponentialDelay,
  retryCondition: error => {
    return axiosRetry.isNetworkOrIdempotentRequestError(error) ||
           error.response?.status === 500
  }
})
```

### 6.27 批量操作

```typescript
// 批量删除
export function batchDeleteUsers(ids: number[]) {
  return request({
    url: '/users/batch',
    method: 'delete',
    data: { ids }
  })
}

// 批量导出
export function exportUsers(params) {
  return request({
    url: '/users/export',
    method: 'get',
    params,
    responseType: 'blob' // 重要：设置为 blob 以下载文件
  })
}
```

## 后端接口实现

### 6.28 Controller 实现

**UserController 完整实现：**

```java
package com.iboot.admin.interfaces.controller;

import com.iboot.admin.common.result.Result;
import com.iboot.admin.interfaces.dto.user.CreateUserRequest;
import com.iboot.admin.interfaces.dto.user.UserResponse;
import com.iboot.admin.application.service.UserApplicationService;
import com.iboot.admin.application.dto.UserDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 *
 * 处理用户相关的 HTTP 请求，包括用户创建、查询、更新、删除等操作
 *
 * @author iBoot
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    @PostMapping
    @Operation(summary = "创建用户")
    public ResponseEntity<Result<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserDTO userDTO = userApplicationService.createUser(request.toCommand());
        return ResponseEntity.ok(Result.success(UserResponse.fromDTO(userDTO)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public ResponseEntity<Result<UserResponse>> getUser(@PathVariable Long id) {
        UserDTO userDTO = userApplicationService.getUserById(id);
        return ResponseEntity.ok(Result.success(UserResponse.fromDTO(userDTO)));
    }

    @GetMapping
    @Operation(summary = "分页查询用户")
    public ResponseEntity<Result<PageResult<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        PageResult<UserDTO> page = userApplicationService.getUsers(pageNum, pageSize, username, status);
        return ResponseEntity.ok(Result.success(page.map(UserResponse::fromDTO)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    public ResponseEntity<Result<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request) {
        UserDTO userDTO = userApplicationService.updateUser(id, request.toCommand());
        return ResponseEntity.ok(Result.success(UserResponse.fromDTO(userDTO)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public ResponseEntity<Result<Void>> deleteUser(@PathVariable Long id) {
        userApplicationService.deleteUser(id);
        return ResponseEntity.ok(Result.success());
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "修改密码")
    public ResponseEntity<Result<Void>> changePassword(
            @PathVariable Long id,
            @RequestBody PasswordChangeRequest request) {
        userApplicationService.changePassword(id, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(Result.success());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改用户状态")
    public ResponseEntity<Result<Void>> changeStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        userApplicationService.changeStatus(id, status);
        return ResponseEntity.ok(Result.success());
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ResponseEntity<Result<UserResponse>> getCurrentUser() {
        UserDTO userDTO = userApplicationService.getCurrentUser();
        return ResponseEntity.ok(Result.success(UserResponse.fromDTO(userDTO)));
    }
}
```

### 6.29 统一响应结果

**Result 工具类：**

```java
package com.iboot.admin.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

### 6.30 分页结果封装

**PageResult 实现：**

```java
package com.iboot.admin.common.result;

import lombok.Data;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结果封装
 *
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer pages;

    public PageResult() {}

    public PageResult(List<T> list, Long total, Integer page, Integer size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
    }

    public static <T> PageResult<T> of(List<T> list, Long total, Integer page, Integer size) {
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 转换数据类型
     */
    public <R> PageResult<R> map(Function<T, R> converter) {
        List<R> newList = list.stream().map(converter).collect(Collectors.toList());
        return new PageResult<>(newList, total, page, size);
    }
}
```

### 6.31 全局异常处理

**GlobalExceptionHandler 实现：**

```java
package com.iboot.admin.common.exception;

import com.iboot.admin.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author iBoot
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(Exception e) {
        String message = extractValidationError(e);
        log.warn("参数校验失败：{}", message);
        return Result.error(400, message);
    }

    /**
     * 认证失败处理
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败：{}", e.getMessage());
        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 权限不足处理
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return Result.error(403, "无权限访问");
    }

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error("系统繁忙，请稍后再试");
    }

    private String extractValidationError(Exception e) {
        if (e instanceof MethodArgumentNotValidException ex) {
            return ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        }
        if (e instanceof BindException ex) {
            return ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        }
        return "参数校验失败";
    }
}
```

### 6.32 DDD 层间交互流程

**完整的请求处理流程：**

```
┌─────────────────┐
│  HTTP 请求       │
│  POST /api/users │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ UserController  │  ← 接口层：接收请求、参数校验
│  - @Valid       │
│  - @RequestBody │
└────────┬────────┘
         │ CreateUserRequest.toCommand()
         ▼
┌─────────────────┐
│ UserApplication │  ← 应用层：编排业务流程
│ Service         │
│  - 校验用户名    │
│  - 加密密码     │
│  - 创建实体     │
│  - 保存用户     │
│  - 发布事件     │
└────────┬────────┘
         │
         ├─────────────────┐
         │                 │
         ▼                 ▼
┌─────────────────┐ ┌─────────────────┐
│ UserRepository  │ │ PasswordService │  ← 领域层：业务逻辑
│ (接口)          │ │ (领域服务)      │
└────────┬────────┘ └─────────────────┘
         │
         ▼
┌─────────────────┐
│ UserRepository  │  ← 基础设施层：持久化实现
│ Impl            │
│  - PO 转换       │
│  - MyBatis 调用  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ UserMapper      │
│ (MyBatis)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ MySQL           │
│ (数据库)        │
└─────────────────┘
```

### 6.33 接口文档（Swagger）

**Swagger 配置：**

```java
package com.iboot.admin.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.components.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 配置
 *
 * @author iBoot
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("iBoot Admin API")
                .version("1.0.0")
                .description("iBoot 通用管理系统 API 文档"))
            .addSecurityItem(new SecurityRequirement()
                .addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

## 相关文档

### 6.34 延伸阅读

- **[09-后端技术架构详解](./09-后端技术架构详解.md)** - 后端 API 设计规范、安全架构详解
- **[10-后端配置详解](./10-后端配置详解.md)** - JWT 认证配置、Swagger/OpenAPI 配置详解

**访问地址：**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/v3/api-docs`
