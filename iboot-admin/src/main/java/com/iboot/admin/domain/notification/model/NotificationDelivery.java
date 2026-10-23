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

package com.iboot.admin.domain.notification.model;

import java.time.LocalDateTime;

/**
 * 通知投递记录。
 *
 * <p>一条记录代表“某个业务事件 × 某个渠道 × 某个接收人”的一次投递。记录同时充当
 * outbox：业务事务内登记，事务提交后尝试发送；失败由 Quartz 按指数退避重试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationDelivery {

    /**
     * 投递记录 ID。
     */
    private Long id;

    /**
     * 业务事件 ID，同一事件的所有渠道共享。
     */
    private String eventId;

    /**
     * 业务事件类型。
     */
    private NotificationEventType eventType;

    /**
     * 投递渠道。
     */
    private NotificationChannelType channel;

    /**
     * 接收人唯一键，例如 {@code USER:123} 或 {@code GROUP:default}。
     */
    private String recipientKey;

    /**
     * 接收用户 ID；群渠道为空。
     */
    private Long recipientId;

    /**
     * 接收地址（邮箱或手机号）；站内与群渠道为空。
     */
    private String recipientAddress;

    /**
     * 模板编码。
     */
    private String templateCode;

    /**
     * 消息标题，同时作为邮件主题与站内信标题。
     */
    private String title;

    /**
     * 渲染后的消息内容。
     */
    private String content;

    /**
     * 模板参数 JSON，短信启用后使用。
     */
    private String paramsJson;

    /**
     * 当前状态。
     */
    private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

    /**
     * 已尝试次数。
     */
    private int attempts;

    /**
     * 最大尝试次数。
     */
    private int maxAttempts = 5;

    /**
     * 下次自动重试时间；为空表示不自动重试。
     */
    private LocalDateTime nextRetryTime;

    /**
     * 最后一次失败或跳过的脱敏原因。
     */
    private String lastError;

    /**
     * 站内消息 ID，重试时复用，避免重复创建消息。
     */
    private Long messageId;

    /**
     * 发送成功时间。
     */
    private LocalDateTime sentTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 判断当前是否可以被执行者领取。
     *
     * @param now 当前时间
     * @return 状态为待发送或失败且已到重试时间时返回 {@code true}
     */
    public boolean canClaim(final LocalDateTime now) {
        if (status != NotificationDeliveryStatus.PENDING && status != NotificationDeliveryStatus.FAILED) {
            return false;
        }
        return nextRetryTime == null || !nextRetryTime.isAfter(now);
    }

    /**
     * 标记为发送中。
     */
    public void markSending() {
        this.status = NotificationDeliveryStatus.SENDING;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记发送成功。
     *
     * @param messageId 站内消息 ID，非站内渠道可为 {@code null}
     * @param now       发送时间
     */
    public void markSuccess(final Long messageId, final LocalDateTime now) {
        this.status = NotificationDeliveryStatus.SUCCESS;
        if (messageId != null) {
            this.messageId = messageId;
        }
        this.sentTime = now;
        this.nextRetryTime = null;
        this.lastError = null;
        this.updateTime = now;
    }

    /**
     * 标记跳过，不再重试。
     *
     * @param reason 跳过原因
     */
    public void markSkipped(final String reason) {
        this.status = NotificationDeliveryStatus.SKIPPED;
        this.lastError = reason;
        this.nextRetryTime = null;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记本次失败，并按指数退避安排下一次重试或进入终止状态。
     *
     * @param error              脱敏后的错误摘要
     * @param maxAttempts        最大尝试次数
     * @param baseBackoffSeconds 基础退避秒数
     * @param now                当前时间
     */
    public void markFailed(final String error, final int maxAttempts, final int baseBackoffSeconds,
                           final LocalDateTime now) {
        this.attempts++;
        this.lastError = error;
        this.updateTime = now;
        if (this.attempts >= maxAttempts) {
            this.status = NotificationDeliveryStatus.DEAD;
            this.nextRetryTime = null;
            return;
        }
        this.status = NotificationDeliveryStatus.FAILED;
        this.nextRetryTime = now.plusSeconds(nextBackoffSeconds(baseBackoffSeconds));
    }

    /**
     * 标记不可重试的终止失败。
     *
     * @param error 脱敏后的错误摘要
     * @param now   当前时间
     */
    public void markDead(final String error, final LocalDateTime now) {
        this.attempts++;
        this.status = NotificationDeliveryStatus.DEAD;
        this.lastError = error;
        this.nextRetryTime = null;
        this.updateTime = now;
    }

    /**
     * 管理员人工重试：重置次数并立即可领取。
     *
     * @param now 当前时间
     */
    public void resetForRetry(final LocalDateTime now) {
        this.status = NotificationDeliveryStatus.PENDING;
        this.attempts = 0;
        this.nextRetryTime = now;
        this.lastError = null;
        this.updateTime = now;
    }

    /**
     * 是否处于终态。
     *
     * @return 终态返回 {@code true}
     */
    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

    private long nextBackoffSeconds(final int baseBackoffSeconds) {
        int exponent = Math.min(Math.max(attempts, 1), 10);
        return (long) baseBackoffSeconds * (1L << (exponent - 1));
    }

    /**
     * 投递记录 ID。
     *
     * @return 记录 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置投递记录 ID。
     *
     * @param id 记录 ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 业务事件 ID。
     *
     * @return 事件 ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * 设置业务事件 ID。
     *
     * @param eventId 事件 ID
     */
    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    /**
     * 业务事件类型。
     *
     * @return 事件类型
     */
    public NotificationEventType getEventType() {
        return eventType;
    }

    /**
     * 设置业务事件类型。
     *
     * @param eventType 事件类型
     */
    public void setEventType(final NotificationEventType eventType) {
        this.eventType = eventType;
    }

    /**
     * 投递渠道。
     *
     * @return 渠道类型
     */
    public NotificationChannelType getChannel() {
        return channel;
    }

    /**
     * 设置投递渠道。
     *
     * @param channel 渠道类型
     */
    public void setChannel(final NotificationChannelType channel) {
        this.channel = channel;
    }

    /**
     * 接收人唯一键。
     *
     * @return 接收人键
     */
    public String getRecipientKey() {
        return recipientKey;
    }

    /**
     * 设置接收人唯一键。
     *
     * @param recipientKey 接收人键
     */
    public void setRecipientKey(final String recipientKey) {
        this.recipientKey = recipientKey;
    }

    /**
     * 接收用户 ID。
     *
     * @return 用户 ID，群渠道为 {@code null}
     */
    public Long getRecipientId() {
        return recipientId;
    }

    /**
     * 设置接收用户 ID。
     *
     * @param recipientId 用户 ID
     */
    public void setRecipientId(final Long recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * 接收地址。
     *
     * @return 邮箱或手机号
     */
    public String getRecipientAddress() {
        return recipientAddress;
    }

    /**
     * 设置接收地址。
     *
     * @param recipientAddress 邮箱或手机号
     */
    public void setRecipientAddress(final String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    /**
     * 模板编码。
     *
     * @return 模板编码
     */
    public String getTemplateCode() {
        return templateCode;
    }

    /**
     * 设置模板编码。
     *
     * @param templateCode 模板编码
     */
    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    /**
     * 消息标题。
     *
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置消息标题。
     *
     * @param title 标题
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * 渲染后的消息内容。
     *
     * @return 内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置渲染后的消息内容。
     *
     * @param content 内容
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * 模板参数 JSON。
     *
     * @return 参数 JSON
     */
    public String getParamsJson() {
        return paramsJson;
    }

    /**
     * 设置模板参数 JSON。
     *
     * @param paramsJson 参数 JSON
     */
    public void setParamsJson(final String paramsJson) {
        this.paramsJson = paramsJson;
    }

    /**
     * 当前状态。
     *
     * @return 投递状态
     */
    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    /**
     * 设置当前状态。
     *
     * @param status 投递状态
     */
    public void setStatus(final NotificationDeliveryStatus status) {
        this.status = status;
    }

    /**
     * 已尝试次数。
     *
     * @return 尝试次数
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * 设置已尝试次数。
     *
     * @param attempts 尝试次数
     */
    public void setAttempts(final int attempts) {
        this.attempts = attempts;
    }

    /**
     * 最大尝试次数。
     *
     * @return 最大尝试次数
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
     * 下次自动重试时间。
     *
     * @return 重试时间，可能为 {@code null}
     */
    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    /**
     * 设置下次自动重试时间。
     *
     * @param nextRetryTime 重试时间
     */
    public void setNextRetryTime(final LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    /**
     * 最后一次错误或跳过原因。
     *
     * @return 脱敏原因
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * 设置最后一次错误或跳过原因。
     *
     * @param lastError 脱敏原因
     */
    public void setLastError(final String lastError) {
        this.lastError = lastError;
    }

    /**
     * 站内消息 ID。
     *
     * @return 消息 ID，可能为 {@code null}
     */
    public Long getMessageId() {
        return messageId;
    }

    /**
     * 设置站内消息 ID。
     *
     * @param messageId 消息 ID
     */
    public void setMessageId(final Long messageId) {
        this.messageId = messageId;
    }

    /**
     * 发送成功时间。
     *
     * @return 发送时间，可能为 {@code null}
     */
    public LocalDateTime getSentTime() {
        return sentTime;
    }

    /**
     * 设置发送成功时间。
     *
     * @param sentTime 发送时间
     */
    public void setSentTime(final LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }

    /**
     * 创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
