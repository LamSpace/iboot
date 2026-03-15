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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 推送配置属性
 * <p>
 * 读取 application.yml 中的 iboot.push 配置
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "iboot.push")
public class PushProperties {

    /**
     * 是否启用推送功能
     */
    private boolean enabled = true;

    /**
     * 广播器类型：local, redis
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

    /**
     * 广播器类型枚举
     */
    public enum BroadcasterType {
        /**
         * 本地模式，单机部署
         */
        LOCAL,
        /**
         * Redis Pub/Sub 模式，集群部署
         */
        REDIS
    }

    /**
     * Redis 配置
     */
    @Data
    public static class RedisConfig {
        /**
         * Redis 频道名称
         */
        private String channel = "iboot:push:events";
    }

    /**
     * SSE 配置
     */
    @Data
    public static class SseConfig {
        /**
         * 连接超时时间（毫秒）
         */
        private Long timeout = 300000L;

        /**
         * 心跳间隔（毫秒）
         */
        private Long heartbeatInterval = 30000L;

        /**
         * 重连间隔（毫秒）
         */
        private Long reconnectInterval = 5000L;
    }

    /**
     * 通知配置
     */
    @Data
    public static class NotificationConfig {
        /**
         * 最大未读消息数
         */
        private Integer maxUnread = 50;

        /**
         * 批量推送大小
         */
        private Integer batchSize = 20;
    }
}
