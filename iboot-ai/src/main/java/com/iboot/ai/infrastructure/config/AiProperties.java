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
     * 权限范围配置。
     */
    private Permission permission = new Permission();

    /**
     * 智能体编排配置。
     */
    private Agent agent = new Agent();

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

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    /**
     * 智能体编排配置。
     *
     * <p>控制智能体能力开关、运行边界与下游 admin 地址；工具结果裁剪与确认有效期也在此配置。
     */
    public static class Agent {

        /**
         * 是否启用智能体编排，默认关闭。
         */
        private boolean enabled = false;

        /**
         * 下游 admin 服务基地址，工具经此地址代理调用。
         */
        private String adminBaseUrl = "http://localhost:8080";

        /**
         * 单次运行的最大模型调用步数。
         */
        private int maxSteps = 6;

        /**
         * 单步工具执行超时秒数。
         */
        private long stepTimeoutSeconds = 30L;

        /**
         * 工具结果进入模型上下文前的最大字符数。
         */
        private int maxResultChars = 2_000;

        /**
         * 工具结果最多保留的行数（分页接口）。
         */
        private int maxResultRows = 20;

        /**
         * 写操作待确认动作的有效期秒数。
         */
        private long approvalTtlSeconds = 300L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAdminBaseUrl() {
            return adminBaseUrl;
        }

        public void setAdminBaseUrl(String adminBaseUrl) {
            this.adminBaseUrl = adminBaseUrl;
        }

        public int getMaxSteps() {
            return maxSteps;
        }

        public void setMaxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
        }

        public long getStepTimeoutSeconds() {
            return stepTimeoutSeconds;
        }

        public void setStepTimeoutSeconds(long stepTimeoutSeconds) {
            this.stepTimeoutSeconds = stepTimeoutSeconds;
        }

        public int getMaxResultChars() {
            return maxResultChars;
        }

        public void setMaxResultChars(int maxResultChars) {
            this.maxResultChars = maxResultChars;
        }

        public int getMaxResultRows() {
            return maxResultRows;
        }

        public void setMaxResultRows(int maxResultRows) {
            this.maxResultRows = maxResultRows;
        }

        public long getApprovalTtlSeconds() {
            return approvalTtlSeconds;
        }

        public void setApprovalTtlSeconds(long approvalTtlSeconds) {
            this.approvalTtlSeconds = approvalTtlSeconds;
        }
    }

    /**
     * 权限范围配置。
     */
    public static class Permission {

        /**
         * admin 只读 scope 接口地址。
         */
        private String scopeUrl = "http://localhost:8080/api/auth/scope";

        /**
         * 权限范围本地缓存秒数。
         */
        private long cacheSeconds = 60L;

        public String getScopeUrl() {
            return scopeUrl;
        }

        public void setScopeUrl(String scopeUrl) {
            this.scopeUrl = scopeUrl;
        }

        public long getCacheSeconds() {
            return cacheSeconds;
        }

        public void setCacheSeconds(long cacheSeconds) {
            this.cacheSeconds = cacheSeconds;
        }
    }
}
