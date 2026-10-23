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

package com.iboot.admin.infrastructure.persistence.po;

import java.time.LocalDateTime;

/**
 * 通知投递记录持久化对象。
 *
 * <p>与 {@code sys_notification_delivery} 表一一对应；枚举以字符串形式存储，
 * 由仓储实现负责与领域枚举转换。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationDeliveryPO {

    /**
     * 投递记录 ID。
     */
    private Long id;

    /**
     * 业务事件 ID。
     */
    private String eventId;

    /**
     * 业务事件类型。
     */
    private String eventType;

    /**
     * 渠道。
     */
    private String channel;

    /**
     * 接收人唯一键。
     */
    private String recipientKey;

    /**
     * 接收用户 ID。
     */
    private Long recipientId;

    /**
     * 接收地址。
     */
    private String recipientAddress;

    /**
     * 模板编码。
     */
    private String templateCode;

    /**
     * 消息标题。
     */
    private String title;

    /**
     * 渲染后的消息内容。
     */
    private String content;

    /**
     * 模板参数 JSON。
     */
    private String paramsJson;

    /**
     * 投递状态。
     */
    private String status;

    /**
     * 已尝试次数。
     */
    private Integer attempts;

    /**
     * 最大尝试次数。
     */
    private Integer maxAttempts;

    /**
     * 下次自动重试时间。
     */
    private LocalDateTime nextRetryTime;

    /**
     * 最后一次脱敏错误。
     */
    private String lastError;

    /**
     * 站内消息 ID。
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
    public String getEventType() {
        return eventType;
    }

    /**
     * 设置业务事件类型。
     *
     * @param eventType 事件类型
     */
    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    /**
     * 渠道。
     *
     * @return 渠道
     */
    public String getChannel() {
        return channel;
    }

    /**
     * 设置渠道。
     *
     * @param channel 渠道
     */
    public void setChannel(final String channel) {
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
     * @return 用户 ID
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
     * @return 接收地址
     */
    public String getRecipientAddress() {
        return recipientAddress;
    }

    /**
     * 设置接收地址。
     *
     * @param recipientAddress 接收地址
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
     * 投递状态。
     *
     * @return 状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置投递状态。
     *
     * @param status 状态
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * 已尝试次数。
     *
     * @return 尝试次数
     */
    public Integer getAttempts() {
        return attempts;
    }

    /**
     * 设置已尝试次数。
     *
     * @param attempts 尝试次数
     */
    public void setAttempts(final Integer attempts) {
        this.attempts = attempts;
    }

    /**
     * 最大尝试次数。
     *
     * @return 最大尝试次数
     */
    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 设置最大尝试次数。
     *
     * @param maxAttempts 最大尝试次数
     */
    public void setMaxAttempts(final Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * 下次自动重试时间。
     *
     * @return 重试时间
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
     * 最后一次脱敏错误。
     *
     * @return 错误摘要
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * 设置最后一次脱敏错误。
     *
     * @param lastError 错误摘要
     */
    public void setLastError(final String lastError) {
        this.lastError = lastError;
    }

    /**
     * 站内消息 ID。
     *
     * @return 消息 ID
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
     * @return 发送时间
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
