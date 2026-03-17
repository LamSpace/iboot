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

package com.iboot.admin.infrastructure.push;

import com.iboot.admin.application.service.BusinessMetricsService;
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
 * SSE 连接管理器
 * <p>
 * 维护用户与 SseEmitter 的映射关系，提供连接、断开、发送、心跳等方法 支持广播、组播、单播功能
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

    private final BusinessMetricsService businessMetricsService;

    private final PushProperties pushProperties;

    public SseEmitterManager(BusinessMetricsService businessMetricsService, PushProperties pushProperties) {
        this.businessMetricsService = businessMetricsService;
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
            userEmitters.remove(userId);
            updateMetrics();
        });
        // 注册超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时，userId: {}，将在 {}ms 后断开", userId, getTimeout());
            userEmitters.remove(userId);
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
            userEmitters.remove(userId);
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
     * 更新监控指标
     */
    private void updateMetrics() {
        if (businessMetricsService != null) {
            businessMetricsService.updatePushConnectionCount(userEmitters.size());
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
            if (businessMetricsService != null) {
                businessMetricsService.recordPushEventSent();
            }
            return true;
        } catch (IOException e) {
            log.warn("推送事件发送失败，userId: {}, error: {}", userId, e.getMessage());
            // 记录失败指标
            if (businessMetricsService != null) {
                businessMetricsService.recordPushEventFailed();
            }
            // 发送失败时断开连接
            disconnect(userId);
            return false;
        } catch (Exception e) {
            log.error("推送事件发送异常，userId: {}", userId, e);
            // 记录失败指标
            if (businessMetricsService != null) {
                businessMetricsService.recordPushEventFailed();
            }
            disconnect(userId);
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
        List<Long> offlineUsers = new ArrayList<>();
        for (Map.Entry<Long, SseEmitter> entry : userEmitters.entrySet()) {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            if (emitter == null) {
                offlineUsers.add(userId);
                continue;
            }
            try {
                emitter
                        .send(SseEmitter.event().id(event.getId()).name(event.getType()).reconnectTime(5000L).data(event));
                successCount++;
            } catch (IOException e) {
                log.warn("广播推送失败，userId: {}, error: {}", userId, e.getMessage());
                offlineUsers.add(userId);
            }
        }
        // 清理离线用户
        for (Long userId : offlineUsers) {
            disconnect(userId);
        }
        log.info("广播推送完成，成功：{}/{}, 离线：{}", successCount, userEmitters.size(), offlineUsers.size());
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
                if (userEmitters.containsKey(userId)) {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                    log.trace("发送心跳，userId: {}", userId);
                } else {
                    scheduler.shutdown();
                }
            } catch (IOException e) {
                log.debug("心跳发送失败，userId: {}, error: {}", userId, e.getMessage());
                disconnect(userId);
                scheduler.shutdown();
            }
        }, getHeartbeatInterval(), getHeartbeatInterval(), TimeUnit.MILLISECONDS);
    }

}
