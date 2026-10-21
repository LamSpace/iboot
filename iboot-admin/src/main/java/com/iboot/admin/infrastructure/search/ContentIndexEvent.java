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

package com.iboot.admin.infrastructure.search;

import org.springframework.context.ApplicationEvent;

import java.util.Objects;

/**
 * 业务内容索引变更事件。
 *
 * <p>由公告与消息的应用服务在事务内发布，{@link ContentIndexListener} 在事务提交后异步消费。
 * 事件只携带定位信息（类型、业务 ID、接收人 ID）与动作，不携带正文快照，监听器会重新读取
 * 已提交的数据，避免把回滚前的数据写入索引。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see ContentIndexListener
 */
public class ContentIndexEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 变更的文档类型。
     */
    private final ContentDocType docType;

    /**
     * 变更动作。
     */
    private final ContentIndexAction action;

    /**
     * 业务主键：公告 ID 或消息 ID。
     */
    private final Long sourceId;

    /**
     * 接收人用户 ID，仅 {@link ContentDocType#MESSAGE} 使用；整条消息级操作为 {@code null}。
     */
    private final Long receiverId;

    /**
     * 创建内容索引事件。
     *
     * @param source     事件发布者
     * @param docType    文档类型
     * @param action     变更动作
     * @param sourceId   公告 ID 或消息 ID
     * @param receiverId 接收人用户 ID，仅移除单个接收人文档时使用
     * @throws NullPointerException 必填参数为 {@code null} 时抛出
     */
    public ContentIndexEvent(final Object source, final ContentDocType docType, final ContentIndexAction action,
                             final Long sourceId, final Long receiverId) {
        super(source);
        this.docType = Objects.requireNonNull(docType, "docType");
        this.action = Objects.requireNonNull(action, "action");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.receiverId = receiverId;
    }

    /**
     * 创建公告刷新事件。
     *
     * @param source   事件发布者
     * @param noticeId 公告 ID
     *
     * @return 索引刷新事件
     */
    public static ContentIndexEvent refreshNotice(final Object source, final Long noticeId) {
        return new ContentIndexEvent(source, ContentDocType.NOTICE, ContentIndexAction.REFRESH_SOURCE, noticeId, null);
    }

    /**
     * 创建消息刷新事件。
     *
     * @param source    事件发布者
     * @param messageId 消息 ID
     *
     * @return 索引刷新事件
     */
    public static ContentIndexEvent refreshMessage(final Object source, final Long messageId) {
        return new ContentIndexEvent(source, ContentDocType.MESSAGE, ContentIndexAction.REFRESH_SOURCE, messageId,
                null);
    }

    /**
     * 创建公告整条移除事件。
     *
     * @param source   事件发布者
     * @param noticeId 公告 ID
     *
     * @return 索引移除事件
     */
    public static ContentIndexEvent deleteNotice(final Object source, final Long noticeId) {
        return new ContentIndexEvent(source, ContentDocType.NOTICE, ContentIndexAction.DELETE_SOURCE, noticeId, null);
    }

    /**
     * 创建消息整条移除事件。
     *
     * @param source    事件发布者
     * @param messageId 消息 ID
     *
     * @return 索引移除事件
     */
    public static ContentIndexEvent deleteMessage(final Object source, final Long messageId) {
        return new ContentIndexEvent(source, ContentDocType.MESSAGE, ContentIndexAction.DELETE_SOURCE, messageId, null);
    }

    /**
     * 创建单个接收人文档移除事件。
     *
     * @param source     事件发布者
     * @param messageId  消息 ID
     * @param receiverId 接收人用户 ID
     *
     * @return 索引移除事件
     */
    public static ContentIndexEvent deleteReceiver(final Object source, final Long messageId, final Long receiverId) {
        return new ContentIndexEvent(source, ContentDocType.MESSAGE, ContentIndexAction.DELETE_RECEIVER, messageId,
                Objects.requireNonNull(receiverId, "receiverId"));
    }

    /**
     * 获取文档类型。
     *
     * @return 文档类型
     */
    public ContentDocType getDocType() {
        return docType;
    }

    /**
     * 获取变更动作。
     *
     * @return 变更动作
     */
    public ContentIndexAction getAction() {
        return action;
    }

    /**
     * 获取业务主键。
     *
     * @return 公告 ID 或消息 ID
     */
    public Long getSourceId() {
        return sourceId;
    }

    /**
     * 获取接收人用户 ID。
     *
     * @return 接收人用户 ID，整条消息级操作为 {@code null}
     */
    public Long getReceiverId() {
        return receiverId;
    }

}
