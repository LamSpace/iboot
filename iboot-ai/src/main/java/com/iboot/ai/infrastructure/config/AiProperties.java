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

package com.iboot.ai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务自有配置。
 *
 * <p>绑定 {@code iboot.ai.*}，包括每日配额与对话窗口参数；模型端点相关配置仍由 Spring AI 的
 * {@code spring.ai.openai.*} 管理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "iboot.ai")
public class AiProperties {

    /**
     * 配额配置。
     */
    private Quota quota = new Quota();

    /**
     * 对话配置。
     */
    private Chat chat = new Chat();

    /**
     * 配额配置。
     */
    public static class Quota {

        /**
         * 全局每日 token 上限，默认 2000 万。
         */
        private long globalDailyTokens = 20_000_000L;

        /**
         * 单用户每日 token 上限，默认 20 万。
         */
        private long userDailyTokens = 200_000L;

        public long getGlobalDailyTokens() {
            return globalDailyTokens;
        }

        public void setGlobalDailyTokens(long globalDailyTokens) {
            this.globalDailyTokens = globalDailyTokens;
        }

        public long getUserDailyTokens() {
            return userDailyTokens;
        }

        public void setUserDailyTokens(long userDailyTokens) {
            this.userDailyTokens = userDailyTokens;
        }
    }

    /**
     * 对话配置。
     */
    public static class Chat {

        /**
         * 会话记忆保留的消息条数。
         */
        private int maxMessages = 20;

        /**
         * 会话与消息的保留小时数。
         */
        private long conversationTtlHours = 24L;

        /**
         * SSE 连接超时毫秒数。
         */
        private long sseTimeoutMs = 300_000L;

        /**
         * SSE 心跳间隔毫秒数。
         */
        private long heartbeatMs = 20_000L;

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        public long getConversationTtlHours() {
            return conversationTtlHours;
        }

        public void setConversationTtlHours(long conversationTtlHours) {
            this.conversationTtlHours = conversationTtlHours;
        }

        public long getSseTimeoutMs() {
            return sseTimeoutMs;
        }

        public void setSseTimeoutMs(long sseTimeoutMs) {
            this.sseTimeoutMs = sseTimeoutMs;
        }

        public long getHeartbeatMs() {
            return heartbeatMs;
        }

        public void setHeartbeatMs(long heartbeatMs) {
            this.heartbeatMs = heartbeatMs;
        }
    }

    public Quota getQuota() {
        return quota;
    }

    public void setQuota(Quota quota) {
        this.quota = quota;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }
}
