# SSE 推送集成技术文档

## 1. SSE 推送概述

### 1.1 什么是 SSE

SSE（Server-Sent Events）是一种基于 HTTP 的服务器推送技术，允许服务器向客户端发送实时更新。它使用 `text/event-stream` 格式，通过长连接实现服务器到客户端的单向数据传输。

### 1.2 为什么选择 SSE

在 iBoot 项目中，我们选择 SSE 而非 WebSocket 作为消息推送方案，主要基于以下考虑：

| 对比项 | SSE | WebSocket |
|--------|-----|-----------|
| 协议 | HTTP 协议 | 独立协议 |
| 通信方向 | 单向（服务器→客户端） | 双向 |
| 数据格式 | 文本（支持 JSON） | 二进制/文本 |
| 连接建立 | 简单 HTTP 请求 | 需要握手 |
| 防火墙穿透 | 容易（标准 HTTP 端口） | 可能需要特殊配置 |
| 重连机制 | 原生支持 | 需手动实现 |
| 适用场景 | 消息推送、实时通知 | 实时聊天、在线游戏 |

### 1.3 功能特性

- **实时消息推送**：新消息到达时实时推送到客户端
- **消息已读通知**：多端同步消息已读状态
- **在线状态感知**：实时统计在线用户数量
- **心跳保活**：自动心跳机制保持连接活跃
- **断线重连**：客户端自动重连机制
- **集群支持**：基于 Redis Pub/Sub 实现多实例消息广播

---

## 2. 架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端 (Browser)                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              SSE Client (eventsource.ts)                │    │
│  │  - 自动重连 (3s)                                         │    │
│  │  - 事件监听 (heartbeat/message/read)                     │    │
│  │  - Token 认证 (?t=xxx)                                   │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP + SSE (?t=JWT_TOKEN)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway / Filter                        │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │           JwtAuthenticationFilter                       │    │
│  │  - 白名单检查                                            │    │
│  │  - Token 解析 (Header/URL 参数)                          │    │
│  │  - Redis 会话验证                                        │    │
│  │  - 设置 Authentication(loginUser as Principal)           │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Spring Boot App                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │  PushEndpoint    │  │ PushEventService │  │SseEmitterMgr │  │
│  │  - /connect      │  │ - sendToUser()   │  │ - connect()  │  │
│  │  - /status       │  │ - sendToUsers()  │  │ - send()     │  │
│  │                  │  │ - broadcast()    │  │ - disconnect()│ │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
│           │                      │                      │       │
│           ▼                      ▼                      ▼       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              CloudEvents / Transaction Events           │    │
│  │  - MessageSentEvent  → MessagePushListener             │    │
│  │  - MessageReadEvent  → MessagePushListener             │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│    Redis Pub/Sub │ │   MySQL/ES       │ │  BusinessMetrics │
│  - Channel:      │ │  - 消息持久化     │ │  - 连接数统计     │
│    push:event    │ │  - 历史记录查询   │ │  - 推送成功率     │
│  - 集群消息广播   │ │                  │ │                  │
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

### 2.2 核心组件

| 组件 | 职责 |
|------|------|
| `PushEndpoint` | SSE 连接入口，处理 `/api/push/connect` 和 `/api/push/status` |
| `SseEmitterManager` | 管理所有 SSE 连接（连接池、发送、断开） |
| `PushEventService` | 推送事件服务，支持单播、组播、广播 |
| `RedisPushEventBroadcaster` | Redis 集群事件广播器 |
| `MessagePushListener` | 监听业务事件并触发推送 |
| `PushProperties` | 推送配置属性 |
| `PushEvent` | 推送事件数据模型（CloudEvents 兼容） |

### 2.3 数据流设计

```
用户 A 发送消息给用户 B
        │
        ▼
┌───────────────────┐
│ MessageController │
└───────────────────┘
        │
        ▼
┌───────────────────────────┐
│ MessageApplicationService │
│  - 保存消息到数据库         │
└───────────────────────────┘
        │
        ▼
┌───────────────────────────┐
│ ApplicationEventPublisher │
│  发布 MessageSentEvent    │
└───────────────────────────┘
        │
        ▼
┌───────────────────────────┐
│  MessagePushListener      │
│  @TransactionalEventListener│
│  - 监听到消息发送事件       │
└───────────────────────────┘
        │
        ▼
┌───────────────────────────┐
│   PushEventService        │
│  - 构建 PushEvent         │
│  - 调用 sendToUser()      │
└───────────────────────────┘
        │
        ▼
┌───────────────────────────┐
│   SseEmitterManager       │
│  - 查找用户 B 的连接        │
│  - 发送 SSE 消息           │
└───────────────────────────┘
        │
        ▼
┌───────────────────────────┐
│   用户 B 的浏览器            │
│  - 接收到新消息推送         │
│  - 更新通知铃铛badge        │
└───────────────────────────┘
```

### 2.4 认证机制

由于浏览器原生 `EventSource` 不支持设置自定义请求头，我们采用 **URL 参数传递 Token** 的方式进行认证：

```typescript
// 前端 SSE 客户端连接
const token = useUserStore().token;
const url = `${BASE_URL}/api/push/connect?t=${token}`;
const eventSource = new EventSource(url);
```

后端 `JwtAuthenticationFilter` 支持从 URL 参数中提取 Token：

```java
// 优先从 Authorization 请求头获取
String bearerToken = request.getHeader("Authorization");
// 其次从 URL 参数获取（支持 "token" 和 "t" 两种参数名）
String tokenParam = request.getParameter("token");
String shortTokenParam = request.getParameter("t");
```

---

## 3. 后端实现

### 3.1 核心类说明

#### 3.1.1 PushEvent - 推送事件模型

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PushEvent {
    /**
     * 事件 ID
     */
    private String id;

    /**
     * 事件类型（如：message.new, message.read, system.notify）
     */
    private String type;

    /**
     * 事件来源（如：/message, /system）
     */
    private String source;

    /**
     * 事件时间（ISO-8601 格式）
     */
    private String time;

    /**
     * 数据内容类型
     */
    private String dataContentType = "application/json";

    /**
     * 数据负载（可以是任意对象）
     */
    private Object data;

    /**
     * 目标用户 ID（单播时必填，广播时可为空）
     */
    private Long targetUserId;
}
```

#### 3.1.2 SseEmitterManager - SSE 连接管理器

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class SseEmitterManager {

    // 存储每个用户的 SSE 连接
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final PushProperties pushProperties;
    private final BusinessMetricsService businessMetricsService;

    /**
     * 建立连接
     */
    public SseEmitter connect(Long userId) {
        // 如果已有连接，先关闭
        disconnect(userId);

        // 创建新的 SseEmitter
        SseEmitter emitter = new SseEmitter(pushProperties.getSse().getTimeout());
        emitters.put(userId, emitter);

        // 更新连接数指标
        businessMetricsService.updateGauge("sse_connection_count", emitters.size());

        // 设置回调
        emitter.onCompletion(() -> disconnect(userId));
        emitter.onTimeout(() -> disconnect(userId));
        emitter.onError(ex -> {
            log.error("SSE 连接异常：userId={}", userId, ex);
            disconnect(userId);
        });

        // 发送初始连接成功消息
        try {
            emitter.send(SseEmitter.event()
                .id("connected")
                .name("connected")
                .data("connected"));
        } catch (IOException e) {
            log.error("发送连接成功消息失败", e);
        }

        return emitter;
    }

    /**
     * 发送消息给指定用户（单播）
     */
    public boolean sendToUser(Long userId, PushEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("用户不在线：userId={}", userId);
            return false;
        }

        try {
            emitter.send(SseEmitter.event()
                .id(event.getId())
                .name(event.getType())
                .data(event));
            return true;
        } catch (IOException e) {
            log.error("发送消息失败：userId={}, eventId={}", userId, event.getId(), e);
            disconnect(userId);
            return false;
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public int broadcast(PushEvent event) {
        List<Long> disconnectedUsers = new ArrayList<>();
        int successCount = 0;

        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(SseEmitter.event()
                    .id(event.getId())
                    .name(event.getType())
                    .data(event));
                successCount++;
            } catch (IOException e) {
                log.error("广播消息失败：userId={}", userId, e);
                disconnectedUsers.add(userId);
            }
        }

        // 清理断开的连接
        disconnectedUsers.forEach(this::disconnect);
        return successCount;
    }

    /**
     * 断开连接
     */
    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE 连接断开：userId={}", userId);
        }
    }

    /**
     * 获取在线用户列表
     */
    public Set<Long> getOnlineUsers() {
        return emitters.keySet();
    }
}
```

#### 3.1.3 PushEndpoint - SSE 接入端点

```java
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Slf4j
public class PushEndpoint {

    private final SseEmitterManager emitterManager;
    private final BusinessMetricsService businessMetricsService;

    /**
     * 建立 SSE 连接
     */
    @GetMapping("/connect")
    public SseEmitter connect() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户请求建立 SSE 连接：userId={}", userId);

        SseEmitter emitter = emitterManager.connect(userId);
        businessMetricsService.increment("sse_connection_count");

        return emitter;
    }

    /**
     * 获取在线状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getOnlineStatus() {
        Set<Long> onlineUsers = emitterManager.getOnlineUsers();
        Map<String, Object> status = new HashMap<>();
        status.put("onlineUsers", onlineUsers.size());
        status.put("isOnline", onlineUsers.contains(SecurityUtils.getCurrentUserId()));
        return Result.success(status);
    }
}
```

#### 3.1.4 PushEventService - 推送事件服务

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class PushEventService {

    private final SseEmitterManager emitterManager;
    private final RedisPushEventBroadcaster redisBroadcaster;
    private final PushProperties pushProperties;
    private final BusinessMetricsService businessMetricsService;

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Long userId, PushEvent event) {
        boolean success = emitterManager.sendToUser(userId, event);

        // 如果是集群模式且用户不在线，通过 Redis 广播
        if (!success && pushProperties.getBroadcaster() == PushProperties.BroadcasterType.REDIS) {
            redisBroadcaster.broadcast(event);
        }

        // 更新指标
        if (success) {
            businessMetricsService.increment("sse_push_success_total");
        } else {
            businessMetricsService.increment("sse_push_failed_total");
        }
    }

    /**
     * 发送消息给多个用户
     */
    public void sendToUsers(List<Long> userIds, PushEvent event) {
        for (Long userId : userIds) {
            sendToUser(userId, event);
        }
    }

    /**
     * 广播消息给所有用户
     */
    public void broadcast(PushEvent event) {
        int successCount = emitterManager.broadcast(event);
        log.info("广播消息：type={}, 成功数={}", event.getType(), successCount);
    }
}
```

#### 3.1.5 RedisPushEventBroadcaster - Redis 集群广播

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisPushEventBroadcaster implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PushProperties pushProperties;
    private final SseEmitterManager emitterManager;

    // 当前实例 ID（防止回环）
    private final String instanceId = UUID.randomUUID().toString();

    /**
     * 广播事件到 Redis
     */
    public void broadcast(PushEvent event) {
        String channel = pushProperties.getRedis().getChannel();
        Map<String, Object> message = new HashMap<>();
        message.put("instanceId", instanceId);
        message.put("event", event);

        redisTemplate.convertAndSend(channel, message);
        log.debug("广播事件到 Redis: channel={}, type={}", channel, event.getType());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 反序列化消息
            Map<String, Object> msg = objectMapper.readValue(
                message.getBody(),
                new TypeReference<Map<String, Object>>() {}
            );

            String sourceInstanceId = (String) msg.get("instanceId");

            // 忽略来自当前实例的消息（防止回环）
            if (instanceId.equals(sourceInstanceId)) {
                return;
            }

            PushEvent event = objectMapper.convertValue(
                msg.get("event"),
                PushEvent.class
            );

            // 发送给目标用户
            if (event.getTargetUserId() != null) {
                emitterManager.sendToUser(event.getTargetUserId(), event);
            }

        } catch (Exception e) {
            log.error("处理 Redis 推送消息失败", e);
        }
    }
}
```

#### 3.1.6 MessagePushListener - 消息推送监听器

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePushListener {

    private final PushEventService pushEventService;

    /**
     * 监听消息发送事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageSentEvent(MessageSentEvent event) {
        Long senderId = event.getSenderId();
        Long recipientId = event.getRecipientId();

        // 构建推送事件
        PushEvent pushEvent = PushEvent.builder()
            .id(UUID.randomUUID().toString())
            .type("message.new")
            .source("/message")
            .time(Instant.now().toString())
            .dataContentType("application/json")
            .data(event.getMessageData())
            .targetUserId(recipientId)
            .build();

        // 只推送给接收方（不推送给发送方自己）
        pushEventService.sendToUser(recipientId, pushEvent);
        log.info("推送新消息通知：sender={}, recipient={}", senderId, recipientId);
    }

    /**
     * 监听消息已读事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageReadEvent(MessageReadEvent event) {
        Long readerId = event.getReaderId();
        List<Long> messageIds = event.getMessageIds();

        // 构建已读事件数据
        Map<String, Object> readData = new HashMap<>();
        readData.put("messageIds", messageIds);
        readData.put("readerId", readerId);
        readData.put("readTime", event.getReadTime());

        // 查询消息的发送者
        List<Long> senderIds = event.getSenderIds();

        // 构建推送事件
        PushEvent pushEvent = PushEvent.builder()
            .id(UUID.randomUUID().toString())
            .type("message.read")
            .source("/message")
            .time(Instant.now().toString())
            .dataContentType("application/json")
            .data(readData)
            .build();

        // 推送给所有发送者
        pushEventService.sendToUsers(senderIds, pushEvent);
        log.info("推送消息已读通知：reader={}, senderCount={}", readerId, senderIds.size());
    }
}
```

#### 3.1.7 PushProperties - 推送配置属性

```java
@Data
@ConfigurationProperties(prefix = "iboot.push")
@Component
public class PushProperties {

    /**
     * 广播器类型（LOCAL 或 REDIS）
     */
    private BroadcasterType broadcaster = BroadcasterType.LOCAL;

    /**
     * Redis 配置
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * SSE 配置
     */
    private SseConfig sse = new SseConfig();

    /**
     * 通知配置
     */
    private NotificationConfig notification = new NotificationConfig();

    public enum BroadcasterType {
        LOCAL,      // 单机模式
        REDIS       // 集群模式
    }

    @Data
    public static class RedisConfig {
        /**
         * Redis 频道名称
         */
        private String channel = "push:event";
    }

    @Data
    public static class SseConfig {
        /**
         * SSE 连接超时时间（毫秒）
         */
        private long timeout = 60 * 60 * 1000L; // 1 小时

        /**
         * 心跳间隔（秒）
         */
        private int heartbeatInterval = 30;

        /**
         * 重连间隔（秒）
         */
        private int reconnectInterval = 3;
    }

    @Data
    public static class NotificationConfig {
        /**
         * 是否启用通知
         */
        private boolean enabled = true;

        /**
         * 通知最大重试次数
         */
        private int maxRetries = 3;
    }
}
```

### 3.2 推送事件定义

| 事件类型 | 事件来源 | 数据格式 | 说明 |
|---------|---------|---------|------|
| `message.new` | `/message` | `MessagePushVO` | 新消息推送 |
| `message.read` | `/message` | `{messageIds, readerId, readTime}` | 消息已读通知 |
| `system.notify` | `/system` | `SystemNotifyVO` | 系统通知 |
| `heartbeat` | `/system` | `{timestamp}` | 心跳包 |

### 3.3 推送模式

#### 3.3.1 单播（Unicast）

发送给单个用户：

```java
PushEvent event = PushEvent.builder()
    .id(UUID.randomUUID().toString())
    .type("message.new")
    .source("/message")
    .time(Instant.now().toString())
    .data(messageData)
    .targetUserId(recipientId)
    .build();

pushEventService.sendToUser(recipientId, event);
```

#### 3.3.2 组播（Multicast）

发送给多个用户：

```java
List<Long> userIds = Arrays.asList(1L, 2L, 3L);
PushEvent event = PushEvent.builder()
    .id(UUID.randomUUID().toString())
    .type("system.notify")
    .source("/system")
    .time(Instant.now().toString())
    .data(notifyData)
    .build();

pushEventService.sendToUsers(userIds, event);
```

#### 3.3.3 广播（Broadcast）

发送给所有在线用户：

```java
PushEvent event = PushEvent.builder()
    .id(UUID.randomUUID().toString())
    .type("system.broadcast")
    .source("/system")
    .time(Instant.now().toString())
    .data(broadcastData)
    .build();

pushEventService.broadcast(event);
```

### 3.4 Redis 集群支持

当应用部署多个实例时，需要配置 Redis 广播器确保消息能推送给连接在不同实例上的用户：

```yaml
iboot:
  push:
    broadcaster: redis  # 设置为 REDIS 模式
    redis:
      channel: push:event  # Redis Pub/Sub 频道
```

工作原理：
1. 当用户 A 连接在实例 1，用户 B 连接在实例 2
2. 用户 A 发送消息给用户 B
3. 实例 1 的 `PushEventService` 发现用户 B 不在线（不在本实例）
4. 通过 Redis Pub/Sub 广播事件
5. 实例 2 监听到事件，推送给用户 B

### 3.5 配置项详解

```yaml
iboot:
  push:
    # 广播器类型：LOCAL（单机）或 REDIS（集群）
    broadcaster: local

    # Redis 配置（broadcaster=redis 时使用）
    redis:
      channel: push:event

    # SSE 配置
    sse:
      # 连接超时时间（毫秒），默认 1 小时
      timeout: 3600000
      # 心跳间隔（秒），默认 30 秒
      heartbeat-interval: 30
      # 重连间隔建议（秒），前端据此设置重连时间
      reconnect-interval: 3

    # 通知配置
    notification:
      # 是否启用通知
      enabled: true
      # 最大重试次数
      max-retries: 3
```

---

## 4. 前端实现

### 4.1 SSE 客户端封装

#### 4.1.1 eventsource.ts - SSE 客户端

```typescript
import { EventSourcePolyfill } from 'event-source-polyfill'
import { useUserStore } from '@/stores/user'
import { BASE_URL } from '@/utils/request'

export interface PushMessage {
  id: string
  type: string
  source: string
  time: string
  data: any
}

export interface PushEventMap {
  heartbeat: { timestamp: number }
  'message.new': MessagePushVO
  'message.read': { messageIds: number[]; readerId: number; readTime: string }
}

class PushClientClass {
  private eventSource: EventSourcePolyfill | null = null
  private readonly reconnectTime = 3000 // 3 秒重连

  connect() {
    const userStore = useUserStore()
    const token = userStore.token

    // 关闭旧连接
    if (this.eventSource) {
      this.eventSource.close()
    }

    // 创建新连接（Token 通过 URL 参数传递）
    const url = `${BASE_URL}/api/push/connect?t=${token}`
    this.eventSource = new EventSourcePolyfill(url, {
      heartbeatTimeout: 60000, // 60 秒超时
    })

    // 监听连接打开
    this.eventSource.onopen = () => {
      console.log('[SSE] 连接已建立')
    }

    // 监听错误
    this.eventSource.onerror = (error) => {
      console.error('[SSE] 连接错误:', error)
      this.eventSource?.close()
      // 3 秒后尝试重连
      setTimeout(() => this.connect(), this.reconnectTime)
    }

    // 监听心跳
    this.addEventListener('heartbeat', (data) => {
      console.log('[SSE] 心跳:', data)
    })

    // 监听新消息
    this.addEventListener('message.new', (data) => {
      console.log('[SSE] 收到新消息:', data)
      // 触发消息通知
    })

    // 监听已读事件
    this.addEventListener('message.read', (data) => {
      console.log('[SSE] 消息已读:', data)
      // 更新消息状态
    })
  }

  addEventListener<T extends keyof PushEventMap>(
    type: string,
    handler: (data: PushEventMap[T]) => void
  ) {
    this.eventSource?.addEventListener(type, (event) => {
      try {
        const data = JSON.parse(event.data)
        handler(data)
      } catch (error) {
        console.error('[SSE] 解析消息失败:', error)
      }
    })
  }

  disconnect() {
    this.eventSource?.close()
    this.eventSource = null
  }
}

// 导出单例
export const PushClient = new PushClientClass()
```

### 4.2 Vue Composable

#### 4.2.1 usePush.ts - Push Composable

```typescript
import { onMounted, onUnmounted } from 'vue'
import { PushClient } from '@/utils/eventsource'
import { usePushStore } from '@/stores/push'

export function usePush() {
  const pushStore = usePushStore()

  onMounted(() => {
    // 建立 SSE 连接
    PushClient.connect()
  })

  onUnmounted(() => {
    // 断开连接
    PushClient.disconnect()
  })

  return {
    isConnected: () => pushStore.isConnected,
    onlineUsers: () => pushStore.onlineUsers,
    unreadCount: () => pushStore.unreadCount,
  }
}
```

### 4.3 UI 组件

#### 4.3.1 NotificationBell.vue - 消息铃铛组件

```vue
<template>
  <el-badge :value="unreadCount" :max="99" class="notification-badge">
    <el-button text @click="toggleDropdown">
      <el-icon><Bell /></el-icon>
    </el-button>
  </el-badge>

  <el-dropdown trigger="click" @command="handleCommand">
    <div class="notification-panel">
      <NotificationList :messages="messages" />
    </div>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { PushClient } from '@/utils/eventsource'
import { usePushStore } from '@/stores/push'

const pushStore = usePushStore()
const unreadCount = computed(() => pushStore.unreadCount)
const messages = computed(() => pushStore.messages)

// 监听新消息推送
PushClient.addEventListener('message.new', (data) => {
  pushStore.addMessage(data)
})

// 监听已读通知
PushClient.addEventListener('message.read', (data) => {
  pushStore.markAsRead(data.messageIds)
})
</script>
```

---

## 5. 消息推送触发

### 5.1 站内消息推送

当用户 A 发送消息给用户 B 时：

```java
// MessageApplicationService.java
@Transactional
public Message createMessage(Message message) {
    // 1. 保存消息到数据库
    messageMapper.insert(message);

    // 2. 发布消息发送事件
    MessageSentEvent event = new MessageSentEvent(this,
        message.getSenderId(),
        message.getRecipientId(),
        convertToPushVO(message)
    );
    applicationEventPublisher.publishEvent(event);

    return message;
}
```

### 5.2 消息已读通知

当用户阅读消息时：

```java
// MessageApplicationService.java
@Transactional
public void markMessagesAsRead(List<Long> messageIds, Long readerId) {
    // 1. 更新消息状态
    messageMapper.markAsRead(messageIds, readerId);

    // 2. 查询消息发送者
    List<Long> senderIds = messageMapper.findSenderIdsByMessageIds(messageIds);

    // 3. 发布消息已读事件
    MessageReadEvent event = new MessageReadEvent(this,
        readerId,
        messageIds,
        senderIds,
        LocalDateTime.now()
    );
    applicationEventPublisher.publishEvent(event);
}
```

### 5.3 扩展业务事件

可以通过发布自定义事件来触发推送：

```java
// 发布系统通知事件
SystemNotifyEvent notifyEvent = new SystemNotifyEvent(
    userId,
    "系统通知",
    "您的账户信息已更新"
);
applicationEventPublisher.publishEvent(notifyEvent);
```

---

## 6. 监控指标

### 6.1 连接数指标

```java
// SseEmitterManager.connect()
businessMetricsService.updateGauge("sse_connection_count", emitters.size());
```

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `sse_connection_count` | Gauge | 当前在线连接数 |

### 6.2 推送成功/失败指标

```java
// PushEventService.sendToUser()
if (success) {
    businessMetricsService.increment("sse_push_success_total");
} else {
    businessMetricsService.increment("sse_push_failed_total");
}
```

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `sse_push_success_total` | Counter | 推送成功次数累计 |
| `sse_push_failed_total` | Counter | 推送失败次数累计 |

### 6.3 Prometheus 配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'iboot-admin'
    static_configs:
      - targets: ['host.docker.internal:8080']
    metrics_path: '/actuator/prometheus'
```

### 6.4 Grafana 面板

可在 `iboot-monitoring-dashboard.json` 中添加 SSE 推送监控面板：

```json
{
  "panels": [
    {
      "title": "SSE 连接数",
      "targets": [
        {
          "expr": "sse_connection_count"
        }
      ]
    },
    {
      "title": "推送成功率",
      "targets": [
        {
          "expr": "rate(sse_push_success_total[5m]) / (rate(sse_push_success_total[5m]) + rate(sse_push_failed_total[5m]))"
        }
      ]
    }
  ]
}
```

---

## 7. API 接口

### 7.1 连接建立接口

**请求**

```
GET /api/push/connect?t=<JWT_TOKEN>
```

**响应**

`Content-Type: text/event-stream`

```
id: connected
event: connected
data: connected

id: 123456
event: message.new
data: {"id":"msg123","senderId":1,"content":"Hello","createTime":"2026-03-12T10:00:00"}

id: heartbeat-1
event: heartbeat
data: {"timestamp":1710237600}
```

### 7.2 在线状态接口

**请求**

```
GET /api/push/status
Authorization: Bearer <JWT_TOKEN>
```

**响应**

```json
{
  "code": 200,
  "data": {
    "onlineUsers": 15,
    "isOnline": true
  }
}
```

---

## 8. 使用示例

### 8.1 后端推送示例

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PushEventService pushEventService;

    /**
     * 发送审批通知
     */
    public void sendApprovalNotice(Long userId, String title, String content) {
        Map<String, String> noticeData = new HashMap<>();
        noticeData.put("title", title);
        noticeData.put("content", content);
        noticeData.put("type", "approval");

        PushEvent event = PushEvent.builder()
            .id(UUID.randomUUID().toString())
            .type("system.notify")
            .source("/notification")
            .time(Instant.now().toString())
            .data(noticeData)
            .targetUserId(userId)
            .build();

        pushEventService.sendToUser(userId, event);
    }
}
```

### 8.2 前端接入示例

```vue
<template>
  <div class="app">
    <NotificationBell />
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { PushClient } from '@/utils/eventsource'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(() => {
  // 登录后建立 SSE 连接
  if (userStore.token) {
    PushClient.connect()
  }
})
</script>
```

---

## 9. 最佳实践

### 9.1 性能优化

1. **连接超时设置**：建议设置为 1 小时，避免过长的连接占用资源
2. **心跳间隔**：30 秒较为合适，既能保持连接活跃，又不会造成过多网络流量
3. **断线重连**：客户端使用指数退避算法（3s → 6s → 12s → 30s）

### 9.2 错误处理

1. **服务端**：捕获 `IOException` 并清理断开的连接
2. **客户端**：监听 `onerror` 事件并重连
3. **降级方案**：SSE 不可用时，使用轮询作为备选方案

### 9.3 安全注意事项

1. **Token 传递**：使用 URL 参数传递 Token 时，确保使用 HTTPS
2. **连接认证**：`/api/push/**` 路径不在白名单中，必须经过 JWT 认证
3. **频率限制**：建议对推送频率进行限制，防止滥用

### 9.4 集群部署

1. **Redis 配置**：多实例部署时，设置 `broadcaster: redis`
2. **频道命名**：不同环境使用不同的 Redis 频道（如 `push:event:dev`、`push:event:prod`）
3. **粘性会话**：如条件允许，使用负载均衡器的粘性会话功能

---

## 10. 常见问题

### Q1: SSE 连接建立后立即断开？

**检查点**：
1. 确认 JWT Token 有效且未过期
2. 检查服务端 `iboot.push.sse.timeout` 配置
3. 查看浏览器控制台是否有 CORS 错误

### Q2: 多实例部署时消息推送不到？

**解决方案**：
1. 确认 `broadcaster` 配置为 `redis`
2. 检查 Redis 连接是否正常
3. 确认 Redis 频道配置一致

### Q3: 前端收不到特定类型的事件？

**检查点**：
1. 确认事件类型名称一致（如 `message.new` vs `message_new`）
2. 检查 `PushEvent.type` 是否正确设置
3. 使用浏览器开发工具查看 Network 中的 SSE 流

### Q4: 心跳事件和其他事件格式不一致？

**说明**：心跳事件由 `SseEmitterManager` 直接发送，格式为纯文本；业务事件使用 `PushEvent` 对象，格式为 JSON。前端需要根据 `event.type` 分别处理。

### Q5: 如何测试 SSE 推送功能？

**方法 1**：使用 curl 命令
```bash
curl http://localhost:8080/api/push/connect?t=<YOUR_TOKEN>
```

**方法 2**：使用浏览器控制台
```javascript
const token = localStorage.getItem('token')
const es = new EventSource(`http://localhost:8080/api/push/connect?t=${token}`)
es.onmessage = (e) => console.log('收到消息:', e.data)
es.addEventListener('message.new', (e) => console.log('新消息:', JSON.parse(e.data)))
```

---

## 附录：相关文件清单

### 后端核心文件

| 文件路径 | 说明 |
|---------|------|
| `infrastructure/push/PushEvent.java` | 推送事件定义 |
| `infrastructure/push/SseEmitterManager.java` | SSE 连接管理器 |
| `infrastructure/push/PushEndpoint.java` | SSE 端点 Controller |
| `infrastructure/push/PushEventService.java` | 推送事件服务 |
| `infrastructure/push/RedisPushEventBroadcaster.java` | Redis 集群事件广播 |
| `infrastructure/push/MessagePushListener.java` | 消息推送监听器 |
| `infrastructure/push/PushProperties.java` | 推送配置属性 |

### 后端修改文件

| 文件路径 | 修改内容 |
|---------|----------|
| `infrastructure/security/JwtAuthenticationFilter.java` | 支持 URL 参数传递 token |
| `infrastructure/security/SecurityUtils.java` | 支持从 principal 获取用户信息 |
| `interfaces/controller/MenuController.java` | 修改用户信息获取方式 |
| `infrastructure/aspect/LogAspect.java` | 修改用户信息获取方式 |
| `infrastructure/config/SecurityWhitelistConfig.java` | 移除 /api/push/** 白名单 |

### 前端文件

| 文件路径 | 说明 |
|---------|------|
| `iboot-portal/src/utils/eventsource.ts` | SSE 客户端封装 |
| `iboot-portal/src/stores/push.ts` | Push Store |
| `iboot-portal/src/composables/usePush.ts` | Push Composable |
| `iboot-portal/src/components/Notification/NotificationBell.vue` | 消息铃铛组件 |
| `iboot-portal/src/components/Notification/NotificationList.vue` | 消息列表组件 |

### 配置文件

| 文件路径 | 配置项 |
|---------|--------|
| `iboot-admin/src/main/resources/application.yml` | `iboot.push.*` 配置 |
