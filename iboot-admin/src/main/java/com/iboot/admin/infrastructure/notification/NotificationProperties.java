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

package com.iboot.admin.infrastructure.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 多渠道通知配置。
 *
 * <p>绑定 {@code application.yml} 中的 {@code iboot.notification.*}，覆盖站内、
 * 邮件、钉钉、短信与重试五组参数。密钥只从环境变量注入，不写入配置文件。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "iboot.notification")
public class NotificationProperties {

    /**
     * 是否启用站内通知渠道。
     */
    private boolean inAppEnabled = true;

    /**
     * 邮件渠道配置。
     */
    private Email email = new Email();

    /**
     * 钉钉渠道配置。
     */
    private DingTalk dingtalk = new DingTalk();

    /**
     * 短信渠道配置。
     */
    private Sms sms = new Sms();

    /**
     * 重试配置。
     */
    private Retry retry = new Retry();

    /**
     * 是否启用站内通知渠道。
     *
     * @return 启用返回 {@code true}
     */
    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    /**
     * 设置是否启用站内通知渠道。
     *
     * @param inAppEnabled 是否启用
     */
    public void setInAppEnabled(final boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    /**
     * 邮件渠道配置。
     *
     * @return 邮件配置
     */
    public Email getEmail() {
        return email;
    }

    /**
     * 设置邮件渠道配置。
     *
     * @param email 邮件配置
     */
    public void setEmail(final Email email) {
        this.email = email;
    }

    /**
     * 钉钉渠道配置。
     *
     * @return 钉钉配置
     */
    public DingTalk getDingtalk() {
        return dingtalk;
    }

    /**
     * 设置钉钉渠道配置。
     *
     * @param dingtalk 钉钉配置
     */
    public void setDingtalk(final DingTalk dingtalk) {
        this.dingtalk = dingtalk;
    }

    /**
     * 短信渠道配置。
     *
     * @return 短信配置
     */
    public Sms getSms() {
        return sms;
    }

    /**
     * 设置短信渠道配置。
     *
     * @param sms 短信配置
     */
    public void setSms(final Sms sms) {
        this.sms = sms;
    }

    /**
     * 重试配置。
     *
     * @return 重试配置
     */
    public Retry getRetry() {
        return retry;
    }

    /**
     * 设置重试配置。
     *
     * @param retry 重试配置
     */
    public void setRetry(final Retry retry) {
        this.retry = retry;
    }

    /**
     * 邮件渠道配置。
     *
     * <p>SMTP 连接参数仍由 Spring Boot 的 {@code spring.mail.*} 承载；
     * 本组只控制渠道开关与发件人覆盖值。
     *
     * @author iBoot Team
     * @since 1.0.0
     */
    public static class Email {

        /**
         * 是否启用邮件渠道。
         */
        private boolean enabled = true;

        /**
         * 发件人地址；为空时使用 {@code spring.mail.username}。
         */
        private String from;

        /**
         * 是否启用邮件渠道。
         *
         * @return 启用返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用邮件渠道。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 发件人地址。
         *
         * @return 发件人地址，可能为 {@code null}
         */
        public String getFrom() {
            return from;
        }

        /**
         * 设置发件人地址。
         *
         * @param from 发件人地址
         */
        public void setFrom(final String from) {
            this.from = from;
        }
    }

    /**
     * 钉钉群机器人配置。
     *
     * @author iBoot Team
     * @since 1.0.0
     */
    public static class DingTalk {

        /**
         * 是否启用钉钉渠道。
         */
        private boolean enabled = false;

        /**
         * 群机器人 Webhook 地址，包含 access_token。
         */
        private String webhook;

        /**
         * 加签 Secret。
         */
        private String secret;

        /**
         * 消息正文前缀，兼容机器人“自定义关键词”安全设置。
         */
        private String keywordPrefix = "【iBoot】";

        /**
         * 每分钟最大发送条数。
         */
        private int ratePerMinute = 20;

        /**
         * 是否启用钉钉渠道。
         *
         * @return 启用返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用钉钉渠道。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Webhook 地址。
         *
         * @return Webhook 地址，可能为 {@code null}
         */
        public String getWebhook() {
            return webhook;
        }

        /**
         * 设置 Webhook 地址。
         *
         * @param webhook Webhook 地址
         */
        public void setWebhook(final String webhook) {
            this.webhook = webhook;
        }

        /**
         * 加签 Secret。
         *
         * @return 加签 Secret，可能为 {@code null}
         */
        public String getSecret() {
            return secret;
        }

        /**
         * 设置加签 Secret。
         *
         * @param secret 加签 Secret
         */
        public void setSecret(final String secret) {
            this.secret = secret;
        }

        /**
         * 消息正文前缀。
         *
         * @return 关键词前缀
         */
        public String getKeywordPrefix() {
            return keywordPrefix;
        }

        /**
         * 设置消息正文前缀。
         *
         * @param keywordPrefix 关键词前缀
         */
        public void setKeywordPrefix(final String keywordPrefix) {
            this.keywordPrefix = keywordPrefix;
        }

        /**
         * 每分钟最大发送条数。
         *
         * @return 每分钟条数
         */
        public int getRatePerMinute() {
            return ratePerMinute;
        }

        /**
         * 设置每分钟最大发送条数。
         *
         * @param ratePerMinute 每分钟条数
         */
        public void setRatePerMinute(final int ratePerMinute) {
            this.ratePerMinute = ratePerMinute;
        }
    }

    /**
     * 阿里云短信配置。
     *
     * <p>本期默认关闭；启用前需要企业资质、签名与模板审核。
     *
     * @author iBoot Team
     * @since 1.0.0
     */
    public static class Sms {

        /**
         * 是否启用短信渠道。
         */
        private boolean enabled = false;

        /**
         * RAM 用户 AccessKey ID。
         */
        private String accessKeyId;

        /**
         * RAM 用户 AccessKey Secret。
         */
        private String accessKeySecret;

        /**
         * 审核通过的短信签名。
         */
        private String signName;

        /**
         * 审核通过的短信模板 Code。
         */
        private String templateCode;

        /**
         * 短信服务地域。
         */
        private String region = "cn-hangzhou";

        /**
         * 是否启用短信渠道。
         *
         * @return 启用返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用短信渠道。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * AccessKey ID。
         *
         * @return AccessKey ID，可能为 {@code null}
         */
        public String getAccessKeyId() {
            return accessKeyId;
        }

        /**
         * 设置 AccessKey ID。
         *
         * @param accessKeyId AccessKey ID
         */
        public void setAccessKeyId(final String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        /**
         * AccessKey Secret。
         *
         * @return AccessKey Secret，可能为 {@code null}
         */
        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        /**
         * 设置 AccessKey Secret。
         *
         * @param accessKeySecret AccessKey Secret
         */
        public void setAccessKeySecret(final String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        /**
         * 短信签名。
         *
         * @return 签名名称，可能为 {@code null}
         */
        public String getSignName() {
            return signName;
        }

        /**
         * 设置短信签名。
         *
         * @param signName 签名名称
         */
        public void setSignName(final String signName) {
            this.signName = signName;
        }

        /**
         * 短信模板 Code。
         *
         * @return 模板 Code，可能为 {@code null}
         */
        public String getTemplateCode() {
            return templateCode;
        }

        /**
         * 设置短信模板 Code。
         *
         * @param templateCode 模板 Code
         */
        public void setTemplateCode(final String templateCode) {
            this.templateCode = templateCode;
        }

        /**
         * 短信服务地域。
         *
         * @return 地域标识
         */
        public String getRegion() {
            return region;
        }

        /**
         * 设置短信服务地域。
         *
         * @param region 地域标识
         */
        public void setRegion(final String region) {
            this.region = region;
        }
    }

    /**
     * 投递重试配置。
     *
     * @author iBoot Team
     * @since 1.0.0
     */
    public static class Retry {

        /**
         * 最大尝试次数。
         */
        private int maxAttempts = 5;

        /**
         * 指数退避的基础秒数。
         */
        private int backoffSeconds = 60;

        /**
         * 单轮扫描的最大记录数。
         */
        private int batchSize = 100;

        /**
         * 最大尝试次数；非法值回退为 5。
         *
         * @return 归一化后的最大尝试次数
         */
        public int normalizedMaxAttempts() {
            return maxAttempts > 0 ? maxAttempts : 5;
        }

        /**
         * 基础退避秒数；非法值回退为 60。
         *
         * @return 归一化后的退避秒数
         */
        public int normalizedBackoffSeconds() {
            return backoffSeconds > 0 ? backoffSeconds : 60;
        }

        /**
         * 单轮批量大小；非法值回退为 100。
         *
         * @return 归一化后的批量大小
         */
        public int normalizedBatchSize() {
            return batchSize > 0 ? batchSize : 100;
        }

        /**
         * 最大尝试次数原始值。
         *
         * @return 配置值
         */
        public int getMaxAttempts() {
            return maxAttempts;
        }

        /**
         * 设置最大尝试次数。
         *
         * @param maxAttempts 最大尝试次数
         */
        public void setMaxAttempts(final int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        /**
         * 基础退避秒数原始值。
         *
         * @return 配置值
         */
        public int getBackoffSeconds() {
            return backoffSeconds;
        }

        /**
         * 设置基础退避秒数。
         *
         * @param backoffSeconds 基础退避秒数
         */
        public void setBackoffSeconds(final int backoffSeconds) {
            this.backoffSeconds = backoffSeconds;
        }

        /**
         * 单轮批量大小原始值。
         *
         * @return 配置值
         */
        public int getBatchSize() {
            return batchSize;
        }

        /**
         * 设置单轮批量大小。
         *
         * @param batchSize 批量大小
         */
        public void setBatchSize(final int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
