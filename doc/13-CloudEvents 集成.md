# 13-CloudEvents 集成

## 目录

1. [CloudEvents 概述](#cloudevents 概述)
2. [集成架构设计](#集成架构设计)
3. [后端实现](#后端实现)
4. [前端适配](#前端适配)
5. [接口覆盖清单](#接口覆盖清单)
6. [响应格式示例](#响应格式示例)
7. [配置与使用](#配置与使用)
8. [最佳实践](#最佳实践)

## CloudEvents 概述

### 什么是 CloudEvents

[CloudEvents](https://cloudevents.io/) 是一个 CNCF（云原生计算基金会）孵化项目，旨在为事件数据定义一个通用的、标准化的格式。它提供了描述事件数据的统一方式，使得事件数据可以在不同系统、平台和服务之间轻松传递。

### 为什么选择 CloudEvents

iBoot 项目引入 CloudEvents 主要基于以下考虑：

1. **标准化**: CloudEvents 是业界公认的事件规范标准，被 Kubernetes、Knative、OpenShift 等主流平台广泛采用
2. **可观测性**: 统一的事件格式便于日志收集、追踪和监控
3. **解耦**: 事件生产者和消费者只需遵循统一格式，降低系统耦合度
4. **扩展性**: 支持元数据扩展，便于未来功能演进
5. **互操作性**: 便于与外部事件驱动架构（如 Knative Eventing、Apache EventMesh）集成

### CloudEvents 1.0 核心属性

| 属性 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `specversion` | String | 是 | CloudEvents 规范版本，固定为 "1.0" |
| `id` | String | 是 | 事件唯一标识符 |
| `type` | String | 是 | 事件类型，采用反向域名表示法 |
| `source` | URI | 是 | 事件源标识符 |
| `time` | Timestamp | 是 | 事件发生时间 |
| `datacontenttype` | String | 是 | 数据类型（MIME 类型） |
| `data` | Any | 否 | 事件负载数据 |

## 集成架构设计

### 整体架构

iBoot 采用 **结构化模式（Structured Mode）** 集成 CloudEvents，将原有的统一响应格式 `Result<T>` 作为 CloudEvents 的 `data` 内容进行包装。

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Request                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring MVC Controller                      │
│  @RestController                                             │
│  - UserController                                            │
│  - RoleController                                            │
│  - ... (20 Controllers)                                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              ResponseBodyAdvice (拦截器)                     │
│  CloudEventResponseBodyAdvice                                │
│  - 拦截 /api/** 路径                                         │
│  - 检查返回类型是否为 Result<T>                               │
│  - 自动包装为 CloudEventBody                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   CloudEventBody                             │
│  {                                                           │
│    "specversion": "1.0",                                     │
│    "id": "ce-uuid",                                          │
│    "type": "com.iboot.response.success",                     │
│    "source": "/api/user",                                    │
│    "time": "2026-03-11T10:30:00+08:00",                      │
│    "datacontenttype": "application/json",                    │
│    "data": Result<T>                                         │
│  }                                                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Jackson Serializer                         │
│  - application/cloudevents+json Content-Type                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Client Response                         │
│  Axios Response Interceptor 解包 CloudEvents                  │
└─────────────────────────────────────────────────────────────┘
```

### 技术选型对比

| 方案 | 模式 | 优点 | 缺点 |
|------|------|------|------|
| **结构化模式** | CloudEvents 作为完整 JSON 体 | 格式完整、易于调试、前端兼容性好 | 响应体积稍大 |
| 二进制模式 | CloudEvents 属性放在 HTTP Header | 响应体最小 | Header 有大小限制、不易调试 |

**决策**: 采用 **结构化模式**，将 CloudEvents 作为外层包装，原有的 `Result<T>` 作为 `data` 内容。

### 数据流设计

```
用户操作 → Controller → ApplicationService → DomainModel
                                                   │
                                                   ▼
                                              Repository
                                                   │
                                                   ▼
┌──────────────────────────────────────────────────┘
│
▼
Controller 返回 Result<T>
│
▼
CloudEventResponseBodyAdvice 拦截
│
▼
确定事件类型 (CloudEventTypes)
│
▼
确定事件源 (从请求 URI)
│
▼
生成事件 ID (UUID)
│
▼
包装为 CloudEventBody
│
▼
Jackson 序列化为 JSON
│
▼
返回客户端
```

## 后端实现

### 项目结构

```
iboot-admin/src/main/java/com/iboot/admin/
├── common/
│   ├── cloudevent/
│   │   ├── CloudEventTypes.java          # 事件类型常量定义
│   │   ├── CloudEventFactory.java        # 事件工厂工具类
│   │   └── CloudEventBody.java           # 响应体包装类
│   ├── annotation/
│   │   └── CloudEventEnabled.java        # (可选) 标记注解
│   └── result/
│       └── Result.java                   # 原有统一响应格式
└── interfaces/
    ├── advisor/
    │   └── CloudEventResponseBodyAdvice.java  # 响应拦截器
    └── config/
        └── CloudEventConfig.java         # 消息转换器配置
```

### 核心组件

#### 1. CloudEventTypes - 事件类型常量

定义所有业务事件类型，采用 `com.iboot.{domain}.{action}` 命名规范：

```java
public final class CloudEventTypes {

    private CloudEventTypes() {
        // 防止实例化
    }

    // ==================== 通用响应事件 ====================
    public static final String RESPONSE_SUCCESS = "com.iboot.response.success";
    public static final String RESPONSE_ERROR = "com.iboot.response.error";

    // ==================== 认证授权事件 ====================
    public static final String AUTH_LOGIN_SUCCESS = "com.iboot.auth.login.success";
    public static final String AUTH_LOGIN_FAILED = "com.iboot.auth.login.failed";
    public static final String AUTH_LOGOUT_SUCCESS = "com.iboot.auth.logout.success";
    public static final String AUTH_REGISTER_SUCCESS = "com.iboot.auth.register.success";

    // ==================== 用户管理事件 ====================
    public static final String USER_CREATED = "com.iboot.user.created";
    public static final String USER_UPDATED = "com.iboot.user.updated";
    public static final String USER_DELETED = "com.iboot.user.deleted";
    public static final String USER_PASSWORD_RESET = "com.iboot.user.password.reset";
    public static final String USER_STATUS_CHANGED = "com.iboot.user.status.changed";

    // ==================== 角色管理事件 ====================
    public static final String ROLE_CREATED = "com.iboot.role.created";
    public static final String ROLE_UPDATED = "com.iboot.role.updated";
    public static final String ROLE_DELETED = "com.iboot.role.deleted";
    public static final String ROLE_PERMISSION_ASSIGNED = "com.iboot.role.permission.assigned";

    // ==================== 部门管理事件 ====================
    public static final String DEPT_CREATED = "com.iboot.dept.created";
    public static final String DEPT_UPDATED = "com.iboot.dept.updated";
    public static final String DEPT_DELETED = "com.iboot.dept.deleted";

    // ==================== 岗位管理事件 ====================
    public static final String POST_CREATED = "com.iboot.post.created";
    public static final String POST_UPDATED = "com.iboot.post.updated";
    public static final String POST_DELETED = "com.iboot.post.deleted";

    // ==================== 菜单管理事件 ====================
    public static final String MENU_CREATED = "com.iboot.menu.created";
    public static final String MENU_UPDATED = "com.iboot.menu.updated";
    public static final String MENU_DELETED = "com.iboot.menu.deleted";

    // ==================== 字典管理事件 ====================
    public static final String DICT_TYPE_CREATED = "com.iboot.dict.type.created";
    public static final String DICT_TYPE_UPDATED = "com.iboot.dict.type.updated";
    public static final String DICT_TYPE_DELETED = "com.iboot.dict.type.deleted";
    public static final String DICT_DATA_CREATED = "com.iboot.dict.data.created";
    public static final String DICT_DATA_UPDATED = "com.iboot.dict.data.updated";
    public static final String DICT_DATA_DELETED = "com.iboot.dict.data.deleted";

    // ==================== 参数配置事件 ====================
    public static final String CONFIG_CREATED = "com.iboot.config.created";
    public static final String CONFIG_UPDATED = "com.iboot.config.updated";
    public static final String CONFIG_DELETED = "com.iboot.config.deleted";

    // ==================== 系统公告事件 ====================
    public static final String NOTICE_CREATED = "com.iboot.notice.created";
    public static final String NOTICE_UPDATED = "com.iboot.notice.updated";
    public static final String NOTICE_DELETED = "com.iboot.notice.deleted";

    // ==================== 消息中心事件 ====================
    public static final String MESSAGE_SENT = "com.iboot.message.sent";
    public static final String MESSAGE_MARKED_READ = "com.iboot.message.marked.read";
    public static final String MESSAGE_DELETED = "com.iboot.message.deleted";
    public static final String MESSAGE_TEMPLATE_CREATED = "com.iboot.message.template.created";
    public static final String MESSAGE_TEMPLATE_UPDATED = "com.iboot.message.template.updated";
    public static final String MESSAGE_TEMPLATE_DELETED = "com.iboot.message.template.deleted";

    // ==================== 定时任务事件 ====================
    public static final String JOB_CREATED = "com.iboot.job.created";
    public static final String JOB_UPDATED = "com.iboot.job.updated";
    public static final String JOB_DELETED = "com.iboot.job.deleted";
    public static final String JOB_STARTED = "com.iboot.job.started";
    public static final String JOB_STOPPED = "com.iboot.job.stopped";
    public static final String JOB_PAUSED = "com.iboot.job.paused";
    public static final String JOB_RESUMED = "com.iboot.job.resumed";

    // ==================== 文件管理事件 ====================
    public static final String FILE_UPLOADED = "com.iboot.file.uploaded";
    public static final String FILE_DELETED = "com.iboot.file.deleted";

    // ==================== 日志管理事件 ====================
    public static final String LOG_OPERATION_CREATED = "com.iboot.log.operation.created";
    public static final String LOG_LOGIN_CREATED = "com.iboot.log.login.created";

    // ==================== 系统监控事件 ====================
    public static final String MONITOR_SERVER_INFO = "com.iboot.monitor.server.info";
    public static final String MONITOR_REDIS_INFO = "com.iboot.monitor.redis.info";
    public static final String MONITOR_MINIO_INFO = "com.iboot.monitor.minio.info";
    public static final String STATISTICS_INFO = "com.iboot.statistics.info";
    public static final String DASHBOARD_INFO = "com.iboot.dashboard.info";
}
```

**事件分类**：
- **通用响应事件** (2 个)：RESPONSE_SUCCESS, RESPONSE_ERROR
- **认证授权事件** (4 个)：登录、登出、注册
- **用户管理事件** (5 个)：用户 CRUD、密码重置、状态变更
- **角色/部门/岗位/菜单事件** (各 3-4 个)：各自 CRUD 操作
- **字典/配置/公告/消息事件** (各 3-6 个)：各自业务操作
- **任务/文件/日志事件** (各 2-8 个)：定时任务、文件管理、日志记录
- **监控事件** (5 个)：服务器、Redis、MinIO、统计、仪表板

**完整事件类型列表** 共 **50+ 个事件**，详见 [事件类型详细清单](#事件类型详细清单)。

#### 2. CloudEventBody - 响应体包装类

```java
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudEventBody<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * CloudEvents 规范版本
     */
    @JsonProperty("specversion")
    private String specversion = "1.0";

    /**
     * 事件 ID，确保唯一性
     */
    @JsonProperty("id")
    private String id;

    /**
     * 事件类型
     */
    @JsonProperty("type")
    private String type;

    /**
     * 事件源
     */
    @JsonProperty("source")
    private String source;

    /**
     * 事件发生时间
     */
    @JsonProperty("time")
    private String time;

    /**
     * 数据内容类型
     */
    @JsonProperty("datacontenttype")
    private String datacontenttype = "application/json";

    /**
     * 实际的业务数据（原有的 Result 响应）
     */
    @JsonProperty("data")
    private Result<T> data;

    /**
     * 创建成功响应的 CloudEventBody
     */
    public static <T> CloudEventBody<T> success(String source, T data) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_SUCCESS);
        body.setSource(source);
        body.setTime(OffsetDateTime.now(ZoneId.systemDefault()).toString());
        body.setData(Result.success(data));
        return body;
    }

    /**
     * 创建错误响应的 CloudEventBody
     */
    public static <T> CloudEventBody<T> error(String source, String message) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_ERROR);
        body.setSource(source);
        body.setTime(OffsetDateTime.now(ZoneId.systemDefault()).toString());
        body.setData(Result.error(message));
        return body;
    }

    /**
     * 从 Result 构建 CloudEventBody
     */
    public static <T> CloudEventBody<T> fromResult(Result<T> result, String source) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(determineEventType(result));
        body.setSource(source);
        body.setTime(OffsetDateTime.now(ZoneId.systemDefault()).toString());
        body.setData(result);
        return body;
    }

    private static String determineEventType(Result<?> result) {
        if (result != null && result.getCode() != null && result.getCode() == 200) {
            return CloudEventTypes.RESPONSE_SUCCESS;
        }
        return CloudEventTypes.RESPONSE_ERROR;
    }

    private static String generateId() {
        return "ce-" + UUID.randomUUID();
    }
}
```

**关键特性**：
- 使用 `@JsonProperty` 确保字段名称符合 CloudEvents 规范（小写连字符）
- 使用 `@JsonInclude(JsonInclude.Include.NON_NULL)` 忽略 null 值
- 实现 `Serializable` 接口支持序列化
- 提供静态工厂方法简化使用

#### 3. CloudEventResponseBodyAdvice - 响应拦截器

自动拦截并包装响应：

```java
@Slf4j
@RestControllerAdvice(basePackages = "com.iboot.admin.interfaces")
public class CloudEventResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                           Class<? extends HttpMessageConverter<?>> converterType) {
        // 拦截返回类型为 Result<T> 的方法（CloudEventBody 除外）
        return Result.class.isAssignableFrom(returnType.getParameterType())
                && !CloudEventBody.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // 如果 body 为 null，直接返回
        if (body == null) {
            return null;
        }

        // 如果已经是 CloudEventBody，不再包装
        if (body instanceof CloudEventBody) {
            return body;
        }

        // 获取请求 URI 作为事件源
        HttpServletRequest servletRequest =
            ((ServletServerHttpRequest) request).getServletRequest();
        String requestURI = servletRequest.getRequestURI();

        // 只拦截 /api/** 路径下的请求
        if (!requestURI.startsWith("/api/")) {
            return body;
        }

        // 如果 body 是 Result 类型，包装为 CloudEventBody
        if (body instanceof Result) {
            Result<?> result = (Result<?>) body;

            // 确定事件类型
            String eventType = determineEventType(result);

            // 使用请求 URI 作为事件源
            return CloudEventBody.fromResult(result, eventType, requestURI);
        }

        // 其他类型直接返回（不包装）
        return body;
    }

    /**
     * 根据响应结果确定事件类型
     */
    private String determineEventType(Result<?> result) {
        if (result != null && Objects.equals(result.getCode(), 200)) {
            return CloudEventTypes.RESPONSE_SUCCESS;
        }
        return CloudEventTypes.RESPONSE_ERROR;
    }
}
```

**拦截规则**：
- 包路径：`com.iboot.admin.interfaces`（所有 Controller）
- 返回类型：`Result<T>`（排除 `CloudEventBody`）
- 请求路径：`/api/**`
- 事件源：请求 URI（如 `/api/user/1`）
- 事件类型：根据响应状态码自动判断（200 为成功，其他为错误）

#### 4. CloudEventConfig - 消息转换器配置

配置 Jackson 序列化器和 Content-Type：

```java
@Configuration
public class CloudEventConfig implements WebMvcConfigurer {

    @Bean
    public MappingJackson2HttpMessageConverter cloudEventHttpMessageConverter(
            ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter =
            new MappingJackson2HttpMessageConverter(objectMapper);
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        // 设置 CloudEvents 特定的 Content-Type
        supportedMediaTypes.add(MediaType.valueOf("application/cloudevents+json"));
        // 同时也支持普通 JSON
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        converter.setSupportedMediaTypes(supportedMediaTypes);
        return converter;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 将 CloudEvents 转换器添加到最前面，确保优先匹配
        ObjectMapper objectMapper = new ObjectMapper();
        MappingJackson2HttpMessageConverter converter =
            cloudEventHttpMessageConverter(objectMapper);
        converters.add(0, converter);
    }
}
```

### Maven 依赖

```xml
<!-- CloudEvents 核心库 -->
<dependency>
    <groupId>io.cloudevents</groupId>
    <artifactId>cloudevents-core</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- CloudEvents JSON 序列化 (Jackson) -->
<dependency>
    <groupId>io.cloudevents</groupId>
    <artifactId>cloudevents-json-jackson</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- CloudEvents HTTP 绑定 -->
<dependency>
    <groupId>io.cloudevents</groupId>
    <artifactId>cloudevents-http-basic</artifactId>
    <version>3.0.0</version>
</dependency>
```

## 前端适配

### TypeScript 类型定义

在 `iboot-portal/src/api/auth.ts` 中添加 CloudEvents 类型：

```typescript
/**
 * CloudEvents 1.0 响应格式
 * https://github.com/cloudevents/spec
 */
export interface CloudEvent<T> {
  /**
   * CloudEvents 规范版本
   */
  specversion: string
  /**
   * 事件 ID，确保唯一性
   */
  id: string
  /**
   * 事件类型，格式如 com.iboot.user.created
   */
  type: string
  /**
   * 事件源，通常是请求路径
   */
  source: string
  /**
   * 事件发生时间，ISO 8601 格式
   */
  time: string
  /**
   * 数据内容类型
   */
  datacontenttype: string
  /**
   * 实际的业务数据（原有的 ApiResponse 响应）
   */
  data: ApiResponse<T>
}

/**
 * 原有 API 响应格式（保持向后兼容）
 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}
```

### Axios 响应拦截器

在 `iboot-portal/src/utils/request.ts` 中添加 CloudEvents 解包逻辑：

```typescript
/**
 * 判断是否为 CloudEvent 响应
 */
function isCloudEvent(data: any): boolean {
  return (
    data &&
    typeof data === 'object' &&
    data.specversion === '1.0' &&
    data.id &&
    data.type &&
    data.source &&
    data.datacontenttype === 'application/json' &&
    data.data !== undefined
  )
}

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // 如果是 Blob 数据（二进制文件下载等），直接返回
    if (res instanceof Blob) {
      return res
    }

    // 检测是否为 CloudEvents 格式响应
    if (isCloudEvent(res)) {
      // CloudEvents 格式：解包获取内部的 data（ApiResponse）
      const cloudEvent = res as any
      const innerData = cloudEvent.data

      // 如果内部数据是 ApiResponse 格式
      if (innerData && innerData.code !== undefined) {
        // 如果是错误响应，显示错误消息
        if (innerData.code !== 200) {
          ElMessage({
            message: innerData.message || 'Error',
            type: 'error',
            duration: 5 * 1000
          })
          return Promise.reject(new Error(innerData.message || 'Error'))
        }
        // 成功响应，返回内部数据
        return innerData
      }
      // 如果内部数据格式不对，返回原始 CloudEvent 数据
      return cloudEvent
    }

    // 非 CloudEvents 格式：按原有逻辑处理
    if (res.code && res.code !== 200) {
      ElMessage({
        message: res.message || 'Error',
        type: 'error',
        duration: 5 * 1000
      })
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res
    }
  },
  // ... 错误处理逻辑
)
```

### 类型兼容说明

前端业务代码 **无需修改**，因为 Axios 拦截器会自动解包 CloudEvents 格式，返回原有的 `ApiResponse<T>` 类型。所有现有的组件、composables、stores 都可以正常工作。

### 前端调试

在浏览器控制台查看 CloudEvents 事件信息：

```typescript
// 在 Network 面板查看响应，或添加日志
console.log('[CloudEvent]', {
  id: res.id,
  type: res.type,
  source: res.source,
  time: res.time
})
```

## 接口覆盖清单

### 覆盖统计

| 模块 | Controller | 路径 | 接口数 | 事件类型 |
|------|-----------|------|--------|----------|
| 认证模块 | AuthController | /api/auth | 3 | `com.iboot.auth.*` |
| 用户管理 | UserController | /api/user | 13 | `com.iboot.user.*` |
| 角色管理 | RoleController | /api/role | 9 | `com.iboot.role.*` |
| 部门管理 | DeptController | /api/dept | 8 | `com.iboot.dept.*` |
| 岗位管理 | PostController | /api/post | 7 | `com.iboot.post.*` |
| 菜单管理 | MenuController | /api/menu | 9 | `com.iboot.menu.*` |
| 数据字典 | DictController | /api/dict | 14 | `com.iboot.dict.*` |
| 参数配置 | ConfigController | /api/config | 9 | `com.iboot.config.*` |
| 系统公告 | NoticeController | /api/notice | 10 | `com.iboot.notice.*` |
| 消息中心 | MessageController | /api/message | 13 | `com.iboot.message.*` |
| 消息模板 | MessageTemplateController | /api/message/template | 6 | `com.iboot.message.template.*` |
| 定时任务 | JobController | /api/job | 11 | `com.iboot.job.*` |
| 文件管理 | FileController | /api/file | 9 | `com.iboot.file.*` |
| 日志管理 | LogController | /api/log | 7 | `com.iboot.log.*` |
| 在线用户 | OnlineUserController | /api/online | 4 | `com.iboot.online.user.*` |
| 服务监控 | ServerMonitorController | /api/monitor/server | 1 | `com.iboot.monitor.server.*` |
| Redis 监控 | RedisMonitorController | /api/monitor/redis | 5 | `com.iboot.monitor.redis.*` |
| 统计分析 | StatisticsController | /api/statistics | 2 | `com.iboot.statistics.*` |
| MinIO 监控 | MinioMonitorController | /api/monitor/minio | 2 | `com.iboot.monitor.minio.*` |
| 仪表板 | DashboardController | /api/dashboard | 1 | `com.iboot.dashboard.*` |
| **总计** | **20 Controllers** | - | **143** | **50+ 事件类型** |

**排除接口**: 14 个导出/下载接口（返回 Excel 文件，不适合 CloudEvents 格式）

### 事件类型详细清单

#### 通用响应事件
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.response.success` | 通用成功响应 |
| `com.iboot.response.error` | 通用错误响应 |

#### 认证事件 (Auth)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.auth.login.success` | 登录成功 |
| `com.iboot.auth.login.failed` | 登录失败 |
| `com.iboot.auth.logout.success` | 登出成功 |
| `com.iboot.auth.register.success` | 用户注册成功 |

#### 用户事件 (User)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.user.created` | 用户创建 |
| `com.iboot.user.updated` | 用户更新 |
| `com.iboot.user.deleted` | 用户删除 |
| `com.iboot.user.password.reset` | 密码重置 |
| `com.iboot.user.status.changed` | 状态变更 |

#### 角色事件 (Role)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.role.created` | 角色创建 |
| `com.iboot.role.updated` | 角色更新 |
| `com.iboot.role.deleted` | 角色删除 |
| `com.iboot.role.permission.assigned` | 权限分配 |

#### 部门事件 (Dept)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.dept.created` | 部门创建 |
| `com.iboot.dept.updated` | 部门更新 |
| `com.iboot.dept.deleted` | 部门删除 |

#### 岗位事件 (Post)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.post.created` | 岗位创建 |
| `com.iboot.post.updated` | 岗位更新 |
| `com.iboot.post.deleted` | 岗位删除 |

#### 菜单事件 (Menu)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.menu.created` | 菜单创建 |
| `com.iboot.menu.updated` | 菜单更新 |
| `com.iboot.menu.deleted` | 菜单删除 |

#### 字典事件 (Dict)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.dict.type.created` | 字典类型创建 |
| `com.iboot.dict.type.updated` | 字典类型更新 |
| `com.iboot.dict.type.deleted` | 字典类型删除 |
| `com.iboot.dict.data.created` | 字典数据创建 |
| `com.iboot.dict.data.updated` | 字典数据更新 |
| `com.iboot.dict.data.deleted` | 字典数据删除 |

#### 配置事件 (Config)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.config.created` | 配置创建 |
| `com.iboot.config.updated` | 配置更新 |
| `com.iboot.config.deleted` | 配置删除 |

#### 公告事件 (Notice)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.notice.created` | 公告创建 |
| `com.iboot.notice.updated` | 公告更新 |
| `com.iboot.notice.deleted` | 公告删除 |

#### 消息事件 (Message)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.message.sent` | 消息发送 |
| `com.iboot.message.marked.read` | 消息已读 |
| `com.iboot.message.deleted` | 消息删除 |
| `com.iboot.message.template.created` | 模板创建 |
| `com.iboot.message.template.updated` | 模板更新 |
| `com.iboot.message.template.deleted` | 模板删除 |

#### 任务事件 (Job)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.job.created` | 任务创建 |
| `com.iboot.job.updated` | 任务更新 |
| `com.iboot.job.deleted` | 任务删除 |
| `com.iboot.job.started` | 任务启动 |
| `com.iboot.job.stopped` | 任务停止 |
| `com.iboot.job.paused` | 任务暂停 |
| `com.iboot.job.resumed` | 任务恢复 |

#### 文件事件 (File)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.file.uploaded` | 文件上传 |
| `com.iboot.file.deleted` | 文件删除 |

#### 日志事件 (Log)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.log.operation.created` | 操作日志创建 |
| `com.iboot.log.login.created` | 登录日志创建 |

#### 监控事件 (Monitor)
| 事件类型 | 描述 |
|----------|------|
| `com.iboot.monitor.server.info` | 服务器信息 |
| `com.iboot.monitor.redis.info` | Redis 信息 |
| `com.iboot.monitor.minio.info` | MinIO 信息 |
| `com.iboot.statistics.info` | 统计信息 |
| `com.iboot.dashboard.info` | 仪表板信息 |

## 响应格式示例

### 成功响应示例

**请求**: `GET /api/user/1`

**响应**:
```json
{
  "specversion": "1.0",
  "id": "ce-550e8400-e29b-41d4-a716-446655440000",
  "type": "com.iboot.user.updated",
  "source": "/api/user/1",
  "time": "2026-03-11T10:30:00+08:00",
  "datacontenttype": "application/json",
  "data": {
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "email": "admin@iboot.com",
      "phone": "13800138000",
      "gender": 1,
      "avatar": "/avatar/default.png",
      "status": 1,
      "userType": 1,
      "deptId": 1,
      "createTime": "2026-01-01T00:00:00"
    },
    "timestamp": 1710123000000
  }
}
```

### 错误响应示例

**请求**: `POST /api/user` (用户名重复)

**响应**:
```json
{
  "specversion": "1.0",
  "id": "ce-error-uuid",
  "type": "com.iboot.response.error",
  "source": "/api/user",
  "time": "2026-03-11T10:30:00+08:00",
  "datacontenttype": "application/json",
  "data": {
    "code": 400,
    "message": "用户名已存在",
    "data": null,
    "timestamp": 1710123000000
  }
}
```

### 登录成功响应示例

**请求**: `POST /api/auth/login`

**响应**:
```json
{
  "specversion": "1.0",
  "id": "ce-auth-success-uuid",
  "type": "com.iboot.auth.login.success",
  "source": "/api/auth/login",
  "time": "2026-03-11T10:30:00+08:00",
  "datacontenttype": "application/json",
  "data": {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expiresIn": 7200,
      "user": {
        "id": 1,
        "username": "admin",
        "nickname": "管理员"
      }
    },
    "timestamp": 1710123000000
  }
}
```

### 列表查询响应示例

**请求**: `GET /api/user/list?page=1&size=10`

**响应**:
```json
{
  "specversion": "1.0",
  "id": "ce-list-uuid",
  "type": "com.iboot.response.success",
  "source": "/api/user/list",
  "time": "2026-03-11T10:30:00+08:00",
  "datacontenttype": "application/json",
  "data": {
    "code": 200,
    "message": "操作成功",
    "data": {
      "list": [
        {
          "id": 1,
          "username": "admin",
          "nickname": "管理员"
        }
      ],
      "total": 100,
      "pageNum": 1,
      "pageSize": 10,
      "pages": 10
    },
    "timestamp": 1710123000000
  }
}
```

## 配置与使用

### 自动开启

CloudEvents 默认为 **自动开启**，无需任何配置。

拦截器 `CloudEventResponseBodyAdvice` 会自动拦截 `/api/**` 路径下所有返回 `Result<T>` 类型的接口，并将其包装为 CloudEvents 格式。

### 拦截规则

| 规则项 | 配置 | 说明 |
|--------|------|------|
| 包路径 | `com.iboot.admin.interfaces` | 所有 Controller 所在包 |
| 返回类型 | `Result<T>` | 排除 `CloudEventBody` 类型 |
| 请求路径 | `/api/**` | 排除非 API 路径 |
| 事件源 | 请求 URI | 如 `/api/user/1` |
| 事件类型 | 根据响应码 | 200 为成功，其他为错误 |

### 全局异常处理集成

`GlobalExceptionHandler` 所有异常处理方法均返回 `Result<T>` 类型，因此异常响应也会被 `CloudEventResponseBodyAdvice` 自动拦截并包装为 CloudEvents 格式。

**异常处理流程**：

```
Controller 抛出异常
    │
    ▼
GlobalExceptionHandler 捕获
    │
    ▼
返回 Result.error(code, message)
    │
    ▼
CloudEventResponseBodyAdvice 拦截
    │
    ▼
包装为 CloudEventBody (type=RESPONSE_ERROR)
    │
    ▼
返回客户端
```

**异常响应示例**：

```json
{
  "specversion": "1.0",
  "id": "ce-error-uuid",
  "type": "com.iboot.response.error",
  "source": "/api/user/1",
  "time": "2026-03-11T10:30:00+08:00",
  "datacontenttype": "application/json",
  "data": {
    "code": 403,
    "message": "无权限访问",
    "data": null,
    "timestamp": 1710123000000
  }
}
```

**支持的异常类型**：
- `BusinessException` - 业务异常
- `MethodArgumentNotValidException` - 参数校验异常
- `BindException` - 参数绑定异常
- `ConstraintViolationException` - 约束违反异常
- `IllegalArgumentException` - 非法参数异常
- `DuplicateKeyException` - 数据库唯一键冲突
- `AccessDeniedException` - 权限拒绝
- `AuthenticationException` - 认证异常
- `RuntimeException` - 运行时异常
- `Exception` - 通用异常

### 例外情况

以下情况 **不会** 被 CloudEvents 包装：

1. **导出接口**：返回 Excel 文件的接口（如 `/api/user/export`）
2. **下载接口**：返回二进制流的接口（如 `/api/file/download`）
3. **非 Result 返回**：返回类型不是 `Result<T>` 的接口
4. **非 /api 路径**：如 Swagger UI、Actuator 端点等

### 事件类型映射

当前实现中，事件类型根据响应状态码自动判断：

```java
private String determineEventType(Result<?> result) {
    if (result != null && Objects.equals(result.getCode(), 200)) {
        return CloudEventTypes.RESPONSE_SUCCESS;
    }
    return CloudEventTypes.RESPONSE_ERROR;
}
```

**注意**：当前实现使用通用事件类型（`RESPONSE_SUCCESS`/`RESPONSE_ERROR`），如需更细粒度的事件类型映射（如区分 `USER_CREATED`、`USER_UPDATED` 等），可在拦截器中扩展路径匹配逻辑。

### 日志记录

开启 CloudEvents 拦截器日志，便于调试和追踪：

```yaml
# application.yml
logging:
  level:
    com.iboot.admin.interfaces.advisor.CloudEventResponseBodyAdvice: DEBUG
```

### 前端调试

在浏览器控制台查看 CloudEvents 事件信息：

```typescript
// src/utils/request.ts
service.interceptors.response.use(
  response => {
    const res = response.data

    // 检测并输出 CloudEvents 信息
    if (isCloudEvent(res)) {
      console.log('[CloudEvent]', {
        id: res.id,
        type: res.type,
        source: res.source,
        time: res.time
      })
      return res.data
    }

    return res
  }
)
```

## 最佳实践

### 1. 事件命名规范

- 使用 **反向域名表示法** (Reverse Domain Name Notation)
- 格式：`com.iboot.{domain}.{action}`
- 动词使用过去式：`created`, `updated`, `deleted`, `changed`

### 2. 事件源设计

- 事件源应能唯一标识事件产生位置
- 使用 API 路径作为事件源，便于追踪
- 避免在事件源中包含动态参数（如用户 ID）

**推荐**:
```
source: "/api/user/1"
```

**不推荐**:
```
source: "/api/user/{id}"  // 模板形式
```

### 3. 事件数据设计

- CloudEvents 的 `data` 应保持简洁，避免嵌套过深
- 敏感数据（密码、token）应脱敏或加密
- 大数据量考虑分页或引用方式

### 4. 性能考虑

- CloudEvents 包装会增加响应体积（约增加 200-300 字节）
- 对于高频接口，考虑异步事件发布
- 大文件下载、导出接口不使用 CloudEvents

### 5. 事件消费

```typescript
// 监听特定类型事件
function subscribeToEventType(eventType: string, callback: (event: any) => void) {
  // 在 Axios 拦截器中分发事件
  eventBus.on(eventType, callback)
}

// 使用示例
subscribeToEventType('com.iboot.user.created', (event) => {
  console.log('用户创建事件:', event)
  // 刷新用户列表
  refreshUserList()
})
```

### 6. 跨服务事件传递

CloudEvents 格式便于跨服务传递，可集成消息队列：

```yaml
# 未来扩展：将事件发布到消息队列
iboot:
  cloudevents:
    publish-to-kafka: true
    kafka-topic: iboot-events
```

### 7. 事件溯源

利用 CloudEvents 标准化格式，可实现事件溯源（Event Sourcing）：

```
Command → Aggregate → Event → EventStore → Projection → ReadModel
```

### 8. 监控告警

基于 CloudEvents 类型实现业务告警：

```yaml
# Prometheus 告警规则
groups:
  - name: cloudevents
    rules:
      - alert: HighErrorRate
        expr: rate(cloudevents_error_total[5m]) > 0.1
        annotations:
          summary: "高错误率告警"
```

### 9. 调试技巧

**后端调试**：
```bash
# 使用 curl 测试接口
curl -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/user/1 | jq

# 验证 CloudEvents 字段
curl -s http://localhost:8080/api/user/1 | \
  jq 'has("specversion") and has("id") and has("type") and has("source")'
```

**前端调试**：
```typescript
// 在浏览器 Developer Tools 的 Network 面板查看响应
// 或在 Axios 拦截器中添加日志
console.log('[CloudEvent]', {
  id: res.id,
  type: res.type,
  source: res.source,
  time: res.time
})
```

## 总结

### 核心价值

1. **标准化**: 统一响应格式，符合 CloudEvents 1.0 规范
2. **可观测**: 事件 ID、类型、源、时间等元数据便于追踪
3. **解耦**: 事件生产者和消费者遵循统一规范
4. **扩展**: 便于未来集成事件驱动架构

### 技术亮点

- 自动拦截包装，业务代码零侵入
- 前端透明解包，保持向后兼容
- 全量接口覆盖（143 个 API）
- 50+ 业务事件类型定义
- 全局异常处理集成

### 未来规划

1. **事件总线**：前端事件订阅与分发
2. **事件持久化**：操作审计日志
3. **消息队列集成**：异步事件发布
4. **事件溯源**：支持 CQRS 架构

## 相关文档

| 文档 | 描述 |
|------|------|
| [06-API 集成](./06-API 集成.md) | API 设计规范、统一响应格式、错误处理 |
| [09-后端技术架构详解](./09-后端技术架构详解.md) | DDD 分层架构、全局异常处理 |
| [11-可观测性设计](./11-可观测性设计.md) | 监控指标、日志系统、告警规则 |

## 外部参考

- [CloudEvents 官方规范](https://github.com/cloudevents/spec)
- [CloudEvents 官方网站](https://cloudevents.io/)
- [CloudEvents Java SDK](https://github.com/cloudevents/sdk-java)

---

*文档最后更新：2026-03-11*
