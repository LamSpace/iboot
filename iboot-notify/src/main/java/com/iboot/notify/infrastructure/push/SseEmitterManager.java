/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.notify.infrastructure.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * SSE 连接管理器。
 *
 * <p>维护用户与 {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter}
 * 的映射，提供连接、断开、单播、组播、广播和心跳能力。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class SseEmitterManager {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterManager.class);

    /**
     * 用户 SSE 连接池 key: 用户 ID, value: SseEmitter
     */
    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    private final PushMetrics pushMetrics;

    private final PushProperties pushProperties;

    /**
     * 创建 SSE 连接管理器。
     *
     * @param pushMetrics 推送指标
     * @param pushProperties 推送配置
     */
    public SseEmitterManager(PushMetrics pushMetrics, PushProperties pushProperties) {
        this.pushMetrics = pushMetrics;
        this.pushProperties = pushProperties;
    }

    /**
     * 默认超时时间（5 分钟）
     */
    private Long getTimeout() {
        return pushProperties != null && pushProperties.getSse() != null ? pushProperties.getSse().getTimeout()
                : 5 * 60 * 1000L;
    }

    /**
     * 心跳间隔（30 秒）
     */
    private Long getHeartbeatInterval() {
        return pushProperties != null && pushProperties.getSse() != null
                ? pushProperties.getSse().getHeartbeatInterval() : 30 * 1000L;
    }

    /**
     * 建立 SSE 连接
     *
     * @param userId 用户 ID
     *
     * @return SseEmitter 实例
     */
    public SseEmitter connect(Long userId) {
        // 如果已存在连接，先断开
        disconnect(userId);
        SseEmitter emitter = new SseEmitter(getTimeout());
        userEmitters.put(userId, emitter);
        // 注册完成回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成，userId: {}", userId);
            removeEmitter(userId, emitter);
            updateMetrics();
        });
        // 注册超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时，userId: {}，将在 {}ms 后断开", userId, getTimeout());
            removeEmitter(userId, emitter);
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 连接超时关闭失败，userId: {}", userId, e);
            }
            updateMetrics();
        });
        // 注册错误回调
        emitter.onError(throwable -> {
            log.error("SSE 连接错误，userId: {}, error: {}", userId, throwable.getMessage());
            removeEmitter(userId, emitter);
            updateMetrics();
        });
        // 发送心跳保持连接
        scheduleHeartbeat(userId, emitter);
        // 更新监控指标
        updateMetrics();
        log.info("SSE 连接建立成功，userId: {}, 当前连接数：{}", userId, userEmitters.size());
        return emitter;
    }

    /**
     * 断开 SSE 连接
     *
     * @param userId 用户 ID
     */
    public void disconnect(Long userId) {
        SseEmitter emitter = userEmitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("SSE 连接断开，userId: {}", userId);
            } catch (Exception e) {
                log.error("SSE 连接断开失败，userId: {}", userId, e);
            }
        }
        // 更新监控指标
        updateMetrics();
    }

    /**
     * 断开指定的连接实例。
     *
     * <p>只有该实例仍是用户的当前连接时才从连接池移除，避免旧连接的发送失败连带移除新连接。
     * 无论是否移除成功都完成该实例，释放容器资源。
     *
     * @param userId 用户 ID
     * @param emitter 要断开的连接；为 {@code null} 时只刷新指标
     */
    private void disconnect(Long userId, SseEmitter emitter) {
        if (emitter == null) {
            updateMetrics();
            return;
        }
        boolean removed = userEmitters.remove(userId, emitter);
        try {
            emitter.complete();
            if (removed) {
                log.info("SSE 连接断开，userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("SSE 连接断开失败，userId: {}", userId, e);
        }
        // 更新监控指标
        updateMetrics();
    }

    /**
     * 仅当连接实例仍是当前连接时移除它。
     *
     * @param userId 用户 ID
     * @param emitter 触发回调的连接实例
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        userEmitters.remove(userId, emitter);
    }

    /**
     * 更新监控指标
     */
    private void updateMetrics() {
        if (pushMetrics != null) {
            pushMetrics.updatePushConnectionCount(userEmitters.size());
        }
    }

    /**
     * 发送推送事件给指定用户
     *
     * @param userId 用户 ID
     * @param event  推送事件
     *
     * @return 是否发送成功
     */
    public boolean send(Long userId, PushEvent event) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter == null) {
            log.debug("用户不在线，userId: {}", userId);
            return false;
        }
        try {
            emitter.send(SseEmitter.event().id(event.getId()).name(event.getType()).reconnectTime(5000L).data(event));
            log.debug("推送事件发送成功，userId: {}, eventId: {}, type: {}", userId, event.getId(), event.getType());
            // 记录成功指标
            if (pushMetrics != null) {
                pushMetrics.recordPushEventSent();
            }
            return true;
        } catch (IOException e) {
            log.warn("推送事件发送失败，userId: {}, error: {}", userId, e.getMessage());
            // 记录失败指标
            if (pushMetrics != null) {
                pushMetrics.recordPushEventFailed();
            }
            // 发送失败时断开当前连接实例
            disconnect(userId, emitter);
            return false;
        } catch (Exception e) {
            log.error("推送事件发送异常，userId: {}", userId, e);
            // 记录失败指标
            if (pushMetrics != null) {
                pushMetrics.recordPushEventFailed();
            }
            disconnect(userId, emitter);
            return false;
        }
    }

    /**
     * 广播推送事件给所有在线用户
     *
     * @param event 推送事件
     *
     * @return 发送成功的用户数
     */
    public int broadcast(PushEvent event) {
        int successCount = 0;
        List<Map.Entry<Long, SseEmitter>> offlineEmitters = new ArrayList<>();
        for (Map.Entry<Long, SseEmitter> entry : userEmitters.entrySet()) {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            if (emitter == null) {
                offlineEmitters.add(entry);
                continue;
            }
            try {
                emitter
                        .send(SseEmitter.event().id(event.getId()).name(event.getType()).reconnectTime(5000L).data(event));
                successCount++;
            } catch (IOException e) {
                log.warn("广播推送失败，userId: {}, error: {}", userId, e.getMessage());
                offlineEmitters.add(entry);
            }
        }
        // 只清理发送失败的连接实例
        for (Map.Entry<Long, SseEmitter> entry : offlineEmitters) {
            disconnect(entry.getKey(), entry.getValue());
        }
        log.info("广播推送完成，成功：{}/{}, 离线：{}", successCount, userEmitters.size(), offlineEmitters.size());
        return successCount;
    }

    /**
     * 组播推送事件给指定用户列表
     *
     * @param userIds 用户 ID 列表
     * @param event   推送事件
     *
     * @return 发送成功的用户数
     */
    public int multicast(List<Long> userIds, PushEvent event) {
        int successCount = 0;
        for (Long userId : userIds) {
            if (send(userId, event)) {
                successCount++;
            }
        }
        log.info("组播推送完成，成功：{}/{}, 目标用户数：{}", successCount, userIds.size());
        return successCount;
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数
     */
    public int getOnlineCount() {
        return userEmitters.size();
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户 ID
     *
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        return userEmitters.containsKey(userId);
    }

    /**
     * 获取所有在线用户 ID
     *
     * @return 在线用户 ID 列表
     */
    public List<Long> getOnlineUserIds() {
        return new ArrayList<>(userEmitters.keySet());
    }

    /**
     * 发送心跳
     *
     * @param userId  用户 ID
     * @param emitter SseEmitter
     */
    private void scheduleHeartbeat(Long userId, SseEmitter emitter) {
        // 定时发送心跳
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors
                .newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "SSE-Heartbeat-" + userId);
                    t.setDaemon(true);
                    return t;
                });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (userEmitters.get(userId) == emitter) {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                    log.trace("发送心跳，userId: {}", userId);
                } else {
                    scheduler.shutdown();
                }
            } catch (IOException e) {
                log.debug("心跳发送失败，userId: {}, error: {}", userId, e.getMessage());
                disconnect(userId, emitter);
                scheduler.shutdown();
            }
        }, getHeartbeatInterval(), getHeartbeatInterval(), TimeUnit.MILLISECONDS);
    }

}
