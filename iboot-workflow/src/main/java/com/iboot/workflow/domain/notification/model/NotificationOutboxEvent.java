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

package com.iboot.workflow.domain.notification.model;

import java.time.LocalDateTime;

/**
 * 通知事件 outbox 记录。
 *
 * <p>审批事务内写入，事务提交后由中继异步投递到 RocketMQ。{@code eventId} 是
 * 幂等键，同一业务事件只应存在一条记录；{@code payload} 保存通知命令 JSON。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationOutboxEvent {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 业务事件 ID，唯一。
     */
    private String eventId;

    /**
     * 事件类型，与消息标签一致。
     */
    private String eventType;

    /**
     * 通知命令 JSON。
     */
    private String payload;

    /**
     * 当前状态。
     */
    private NotificationOutboxStatus status;

    /**
     * 已尝试投递次数。
     */
    private int attempts;

    /**
     * 最大尝试次数。
     */
    private int maxAttempts;

    /**
     * 下次可投递时间；为空表示立即可投。
     */
    private LocalDateTime nextRetryTime;

    /**
     * 最后一次失败的错误摘要。
     */
    private String lastError;

    /**
     * broker 返回的消息 ID。
     */
    private String messageId;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public NotificationOutboxStatus getStatus() {
        return status;
    }

    public void setStatus(final NotificationOutboxStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(final int attempts) {
        this.attempts = attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(final int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(final LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(final String lastError) {
        this.lastError = lastError;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
