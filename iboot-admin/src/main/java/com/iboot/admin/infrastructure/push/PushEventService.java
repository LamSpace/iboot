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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 推送事件服务
 * <p>
 * 提供统一的推送事件发送接口，支持单播、组播、广播 支持本地模式和 Redis Pub/Sub 集群模式
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class PushEventService {

    private static final Logger log = LoggerFactory.getLogger(PushEventService.class);

    private final SseEmitterManager sseEmitterManager;

    private final RedisPushEventBroadcaster redisPushEventBroadcaster;

    private final PushProperties pushProperties;

    @SuppressWarnings("all")
    public PushEventService(final SseEmitterManager sseEmitterManager,
                            final RedisPushEventBroadcaster redisPushEventBroadcaster, final PushProperties pushProperties) {
        this.sseEmitterManager = sseEmitterManager;
        this.redisPushEventBroadcaster = redisPushEventBroadcaster;
        this.pushProperties = pushProperties;
    }

    /**
     * 发送推送事件给指定用户（单播）
     *
     * @param userId 用户 ID
     * @param event  推送事件
     */
    public void sendToUser(Long userId, PushEvent event) {
        if (event.getTargetUserId() == null) {
            event.setTargetUserId(userId);
        }
        // 根据配置选择广播模式
        if (pushProperties.getBroadcaster() == PushProperties.BroadcasterType.REDIS) {
            // Redis 集群模式：发布到 Redis，由所有实例接收
            redisPushEventBroadcaster.broadcast(event);
        } else {
            // 本地模式：仅发送到本地连接
            boolean success = sseEmitterManager.send(userId, event);
            if (success) {
                log.debug("推送事件已发送，userId: {}, eventId: {}", userId, event.getId());
            } else {
                log.warn("推送事件发送失败（用户不在线），userId: {}, eventId: {}", userId, event.getId());
            }
        }
    }

    /**
     * 发送推送事件给多个用户（组播）
     *
     * @param userIds 用户 ID 列表
     * @param event   推送事件
     */
    public void sendToUsers(List<Long> userIds, PushEvent event) {
        if (pushProperties.getBroadcaster() == PushProperties.BroadcasterType.REDIS) {
            // Redis 集群模式：发布到 Redis
            redisPushEventBroadcaster.broadcast(event);
            log.info("组播推送已发布到 Redis，目标用户数：{}, eventId: {}", userIds.size(), event.getId());
        } else {
            // 本地模式
            int count = sseEmitterManager.multicast(userIds, event);
            log.info("组播推送完成，成功：{}/{}, eventId: {}", count, userIds.size(), event.getId());
        }
    }

    /**
     * 广播推送事件给所有在线用户
     *
     * @param event 推送事件
     */
    public void broadcast(PushEvent event) {
        if (pushProperties.getBroadcaster() == PushProperties.BroadcasterType.REDIS) {
            // Redis 集群模式：发布到 Redis
            redisPushEventBroadcaster.broadcast(event);
            log.info("广播推送已发布到 Redis，eventId: {}", event.getId());
        } else {
            // 本地模式
            int count = sseEmitterManager.broadcast(event);
            log.info("广播推送完成，成功：{}/{}, eventId: {}", count, sseEmitterManager.getOnlineCount(), event.getId());
        }
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户 ID
     *
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        return sseEmitterManager.isOnline(userId);
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数
     */
    public int getOnlineCount() {
        return sseEmitterManager.getOnlineCount();
    }

    /**
     * 获取所有在线用户 ID
     *
     * @return 在线用户 ID 列表
     */
    public List<Long> getOnlineUserIds() {
        return sseEmitterManager.getOnlineUserIds();
    }

}
