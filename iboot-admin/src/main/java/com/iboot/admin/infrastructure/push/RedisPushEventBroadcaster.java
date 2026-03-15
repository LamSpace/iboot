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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Redis 推送事件广播器
 * <p>
 * 基于 Redis Pub/Sub 实现多实例间的推送事件广播
 * 用于在集群环境下将推送事件广播到所有应用实例
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPushEventBroadcaster implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final SseEmitterManager sseEmitterManager;
    private final ObjectMapper objectMapper;
    private final PushProperties pushProperties;

    /**
     * 获取 Redis 频道名称
     */
    private String getChannel() {
        return pushProperties != null && pushProperties.getRedis() != null
            ? pushProperties.getRedis().getChannel()
            : "iboot:push:events";
    }

    /**
     * 初始化 Redis Pub/Sub 订阅
     */
    @PostConstruct
    public void init() {
        log.info("初始化 Redis Push Pub/Sub 订阅，频道：{}", getChannel());

        // 创建消息监听器适配器
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this);
        listenerAdapter.setSerializer(redisTemplate.getStringSerializer());

        // 订阅频道
        redisMessageListenerContainer.addMessageListener(listenerAdapter, new PatternTopic(getChannel()));

        log.info("Redis Push Pub/Sub 订阅初始化完成");
    }

    /**
     * 广播推送事件到 Redis 频道
     *
     * @param event 推送事件
     */
    public void broadcast(PushEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(getChannel(), message);
            log.debug("推送事件已发布到 Redis，channel: {}, eventId: {}", getChannel(), event.getId());
        } catch (JsonProcessingException e) {
            log.error("序列化推送事件失败，eventId: {}", event.getId(), e);
        }
    }

    /**
     * 广播推送事件到指定 Redis 频道
     *
     * @param channel 频道名称
     * @param event   推送事件
     */
    public void broadcast(String channel, PushEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, message);
            log.debug("推送事件已发布到 Redis，channel: {}, eventId: {}", channel, event.getId());
        } catch (JsonProcessingException e) {
            log.error("序列化推送事件失败，eventId: {}", event.getId(), e);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            byte[] body = message.getBody();
            if (body == null || body.length == 0) {
                return;
            }

            String messageBody = new String(body);
            log.debug("收到 Redis 推送事件，channel: {}, message: {}",
                    new String(message.getChannel()), messageBody);

            // 反序列化推送事件
            PushEvent event = objectMapper.readValue(messageBody, PushEvent.class);

            // 检查事件来源，避免循环广播
            String sourceInstance = event.getSource();
            String currentInstance = getCurrentInstanceId();
            if (sourceInstance != null && sourceInstance.startsWith(currentInstance)) {
                log.debug("忽略本实例发布的事件，eventId: {}", event.getId());
                return;
            }

            // 推送给本地用户
            if (event.getTargetUserId() != null) {
                // 单播
                boolean success = sseEmitterManager.send(event.getTargetUserId(), event);
                if (success) {
                    log.debug("已向本地用户推送消息，userId: {}, eventId: {}",
                            event.getTargetUserId(), event.getId());
                }
            } else {
                // 广播/组播逻辑可根据需要扩展
                log.debug("收到广播事件，eventId: {}, type: {}", event.getId(), event.getType());
            }

        } catch (Exception e) {
            log.error("处理 Redis 推送事件失败", e);
        }
    }

    /**
     * 获取当前实例 ID
     *
     * @return 实例 ID
     */
    private String getCurrentInstanceId() {
        // 使用服务器地址和端口作为实例标识
        return System.getenv("HOSTNAME") != null ?
                System.getenv("HOSTNAME") : "instance-" + System.nanoTime();
    }
}
