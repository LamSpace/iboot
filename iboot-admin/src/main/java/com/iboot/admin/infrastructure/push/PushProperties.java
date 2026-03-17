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

    @SuppressWarnings("all")
    public PushProperties() {
    }

    /**
     * 是否启用推送功能
     */
    @SuppressWarnings("all")
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * 是否启用推送功能
     */
    @SuppressWarnings("all")
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 广播器类型：local, redis
     */
    @SuppressWarnings("all")
    public BroadcasterType getBroadcaster() {
        return this.broadcaster;
    }

    /**
     * 广播器类型：local, redis
     */
    @SuppressWarnings("all")
    public void setBroadcaster(final BroadcasterType broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * Redis 配置
     */
    @SuppressWarnings("all")
    public RedisConfig getRedis() {
        return this.redis;
    }

    /**
     * Redis 配置
     */
    @SuppressWarnings("all")
    public void setRedis(final RedisConfig redis) {
        this.redis = redis;
    }

    /**
     * SSE 配置
     */
    @SuppressWarnings("all")
    public SseConfig getSse() {
        return this.sse;
    }

    /**
     * SSE 配置
     */
    @SuppressWarnings("all")
    public void setSse(final SseConfig sse) {
        this.sse = sse;
    }

    /**
     * 通知配置
     */
    @SuppressWarnings("all")
    public NotificationConfig getNotification() {
        return this.notification;
    }

    /**
     * 通知配置
     */
    @SuppressWarnings("all")
    public void setNotification(final NotificationConfig notification) {
        this.notification = notification;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PushProperties))
            return false;
        final PushProperties other = (PushProperties) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.isEnabled() != other.isEnabled())
            return false;
        final java.lang.Object this$broadcaster = this.getBroadcaster();
        final java.lang.Object other$broadcaster = other.getBroadcaster();
        if (this$broadcaster == null ? other$broadcaster != null : !this$broadcaster.equals(other$broadcaster))
            return false;
        final java.lang.Object this$redis = this.getRedis();
        final java.lang.Object other$redis = other.getRedis();
        if (this$redis == null ? other$redis != null : !this$redis.equals(other$redis))
            return false;
        final java.lang.Object this$sse = this.getSse();
        final java.lang.Object other$sse = other.getSse();
        if (this$sse == null ? other$sse != null : !this$sse.equals(other$sse))
            return false;
        final java.lang.Object this$notification = this.getNotification();
        final java.lang.Object other$notification = other.getNotification();
        if (this$notification == null ? other$notification != null : !this$notification.equals(other$notification))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PushProperties;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isEnabled() ? 79 : 97);
        final java.lang.Object $broadcaster = this.getBroadcaster();
        result = result * PRIME + ($broadcaster == null ? 43 : $broadcaster.hashCode());
        final java.lang.Object $redis = this.getRedis();
        result = result * PRIME + ($redis == null ? 43 : $redis.hashCode());
        final java.lang.Object $sse = this.getSse();
        result = result * PRIME + ($sse == null ? 43 : $sse.hashCode());
        final java.lang.Object $notification = this.getNotification();
        result = result * PRIME + ($notification == null ? 43 : $notification.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "PushProperties(enabled=" + this.isEnabled() + ", broadcaster=" + this.getBroadcaster() + ", redis="
                + this.getRedis() + ", sse=" + this.getSse() + ", notification=" + this.getNotification() + ")";
    }

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
    public static class RedisConfig {

        /**
         * Redis 频道名称
         */
        private String channel = "iboot:push:events";

        @SuppressWarnings("all")
        public RedisConfig() {
        }

        /**
         * Redis 频道名称
         */
        @SuppressWarnings("all")
        public String getChannel() {
            return this.channel;
        }

        /**
         * Redis 频道名称
         */
        @SuppressWarnings("all")
        public void setChannel(final String channel) {
            this.channel = channel;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof PushProperties.RedisConfig))
                return false;
            final PushProperties.RedisConfig other = (PushProperties.RedisConfig) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$channel = this.getChannel();
            final java.lang.Object other$channel = other.getChannel();
            if (this$channel == null ? other$channel != null : !this$channel.equals(other$channel))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof PushProperties.RedisConfig;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $channel = this.getChannel();
            result = result * PRIME + ($channel == null ? 43 : $channel.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "PushProperties.RedisConfig(channel=" + this.getChannel() + ")";
        }

    }

    /**
     * SSE 配置
     */
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

        @SuppressWarnings("all")
        public SseConfig() {
        }

        /**
         * 连接超时时间（毫秒）
         */
        @SuppressWarnings("all")
        public Long getTimeout() {
            return this.timeout;
        }

        /**
         * 连接超时时间（毫秒）
         */
        @SuppressWarnings("all")
        public void setTimeout(final Long timeout) {
            this.timeout = timeout;
        }

        /**
         * 心跳间隔（毫秒）
         */
        @SuppressWarnings("all")
        public Long getHeartbeatInterval() {
            return this.heartbeatInterval;
        }

        /**
         * 心跳间隔（毫秒）
         */
        @SuppressWarnings("all")
        public void setHeartbeatInterval(final Long heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
        }

        /**
         * 重连间隔（毫秒）
         */
        @SuppressWarnings("all")
        public Long getReconnectInterval() {
            return this.reconnectInterval;
        }

        /**
         * 重连间隔（毫秒）
         */
        @SuppressWarnings("all")
        public void setReconnectInterval(final Long reconnectInterval) {
            this.reconnectInterval = reconnectInterval;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof PushProperties.SseConfig))
                return false;
            final PushProperties.SseConfig other = (PushProperties.SseConfig) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$timeout = this.getTimeout();
            final java.lang.Object other$timeout = other.getTimeout();
            if (this$timeout == null ? other$timeout != null : !this$timeout.equals(other$timeout))
                return false;
            final java.lang.Object this$heartbeatInterval = this.getHeartbeatInterval();
            final java.lang.Object other$heartbeatInterval = other.getHeartbeatInterval();
            if (this$heartbeatInterval == null ? other$heartbeatInterval != null
                    : !this$heartbeatInterval.equals(other$heartbeatInterval))
                return false;
            final java.lang.Object this$reconnectInterval = this.getReconnectInterval();
            final java.lang.Object other$reconnectInterval = other.getReconnectInterval();
            if (this$reconnectInterval == null ? other$reconnectInterval != null
                    : !this$reconnectInterval.equals(other$reconnectInterval))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof PushProperties.SseConfig;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $timeout = this.getTimeout();
            result = result * PRIME + ($timeout == null ? 43 : $timeout.hashCode());
            final java.lang.Object $heartbeatInterval = this.getHeartbeatInterval();
            result = result * PRIME + ($heartbeatInterval == null ? 43 : $heartbeatInterval.hashCode());
            final java.lang.Object $reconnectInterval = this.getReconnectInterval();
            result = result * PRIME + ($reconnectInterval == null ? 43 : $reconnectInterval.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "PushProperties.SseConfig(timeout=" + this.getTimeout() + ", heartbeatInterval="
                    + this.getHeartbeatInterval() + ", reconnectInterval=" + this.getReconnectInterval() + ")";
        }

    }

    /**
     * 通知配置
     */
    public static class NotificationConfig {

        /**
         * 最大未读消息数
         */
        private Integer maxUnread = 50;

        /**
         * 批量推送大小
         */
        private Integer batchSize = 20;

        @SuppressWarnings("all")
        public NotificationConfig() {
        }

        /**
         * 最大未读消息数
         */
        @SuppressWarnings("all")
        public Integer getMaxUnread() {
            return this.maxUnread;
        }

        /**
         * 最大未读消息数
         */
        @SuppressWarnings("all")
        public void setMaxUnread(final Integer maxUnread) {
            this.maxUnread = maxUnread;
        }

        /**
         * 批量推送大小
         */
        @SuppressWarnings("all")
        public Integer getBatchSize() {
            return this.batchSize;
        }

        /**
         * 批量推送大小
         */
        @SuppressWarnings("all")
        public void setBatchSize(final Integer batchSize) {
            this.batchSize = batchSize;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof PushProperties.NotificationConfig))
                return false;
            final PushProperties.NotificationConfig other = (PushProperties.NotificationConfig) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$maxUnread = this.getMaxUnread();
            final java.lang.Object other$maxUnread = other.getMaxUnread();
            if (this$maxUnread == null ? other$maxUnread != null : !this$maxUnread.equals(other$maxUnread))
                return false;
            final java.lang.Object this$batchSize = this.getBatchSize();
            final java.lang.Object other$batchSize = other.getBatchSize();
            if (this$batchSize == null ? other$batchSize != null : !this$batchSize.equals(other$batchSize))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof PushProperties.NotificationConfig;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $maxUnread = this.getMaxUnread();
            result = result * PRIME + ($maxUnread == null ? 43 : $maxUnread.hashCode());
            final java.lang.Object $batchSize = this.getBatchSize();
            result = result * PRIME + ($batchSize == null ? 43 : $batchSize.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "PushProperties.NotificationConfig(maxUnread=" + this.getMaxUnread() + ", batchSize="
                    + this.getBatchSize() + ")";
        }

    }

}
