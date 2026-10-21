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

/**
 * 业务内容索引文档。
 *
 * <p>是公告与消息写入 Elasticsearch 时的统一形态。文档 ID 由业务主键确定性生成：
 * 公告为 {@code notice-{id}}，消息为 {@code message-{messageId}-{receiverId}}，
 * 因此同一份数据被重复处理时只会覆盖而不会产生重复文档。
 *
 * <p>时间字段以 ISO-8601 字符串写入，由索引 mapping 的 {@code date} 类型解析，避免客户端
 * 序列化配置对日期格式的影响。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ContentDocument {

    /**
     * 文档类型编码。
     */
    private final String docType;

    /**
     * 业务主键：公告 ID 或消息 ID。
     */
    private final Long sourceId;

    /**
     * 接收人用户 ID，仅消息文档使用。
     */
    private final Long receiverId;

    /**
     * 标题。
     */
    private final String title;

    /**
     * 正文。
     */
    private final String content;

    /**
     * 分类：公告类型或消息类型。
     */
    private final String category;

    /**
     * 创建人。
     */
    private final String createdBy;

    /**
     * 创建时间（ISO-8601 字符串）。
     */
    private final String createdAt;

    /**
     * 更新时间（ISO-8601 字符串）。
     */
    private final String updatedAt;

    /**
     * 创建索引文档。
     *
     * @param docType    文档类型
     * @param sourceId   公告 ID 或消息 ID
     * @param receiverId 接收人用户 ID，公告文档为 {@code null}
     * @param title      标题
     * @param content    正文
     * @param category   分类
     * @param createdBy  创建人
     * @param createdAt  创建时间（ISO-8601 字符串）
     * @param updatedAt  更新时间（ISO-8601 字符串）
     */
    public ContentDocument(final ContentDocType docType, final Long sourceId, final Long receiverId,
                           final String title, final String content, final String category, final String createdBy,
                           final String createdAt, final String updatedAt) {
        this.docType = docType.getCode();
        this.sourceId = sourceId;
        this.receiverId = receiverId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 生成确定性文档 ID。
     *
     * @param docType    文档类型
     * @param sourceId   公告 ID 或消息 ID
     * @param receiverId 接收人用户 ID，消息文档必填
     *
     * @return Elasticsearch 文档 ID
     * @throws IllegalArgumentException 消息文档缺少接收人时抛出
     */
    public static String documentId(final ContentDocType docType, final Long sourceId, final Long receiverId) {
        if (docType == ContentDocType.MESSAGE) {
            if (receiverId == null) {
                throw new IllegalArgumentException("消息索引文档必须指定接收人");
            }
            return "message-" + sourceId + "-" + receiverId;
        }
        return "notice-" + sourceId;
    }

    /**
     * 获取当前文档的确定性 ID。
     *
     * <p>刻意不使用 {@code getXxx} 命名，避免被 JSON 序列化当作文档字段写入索引。
     *
     * @return Elasticsearch 文档 ID
     */
    public String documentId() {
        return receiverId == null ? "notice-" + sourceId : "message-" + sourceId + "-" + receiverId;
    }

    /**
     * 获取文档类型编码。
     *
     * @return 文档类型编码
     */
    public String getDocType() {
        return docType;
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
     * @return 接收人用户 ID，公告文档为 {@code null}
     */
    public Long getReceiverId() {
        return receiverId;
    }

    /**
     * 获取标题。
     *
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取正文。
     *
     * @return 正文
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取分类。
     *
     * @return 分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 获取创建人。
     *
     * @return 创建人
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 获取创建时间。
     *
     * @return ISO-8601 字符串
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return ISO-8601 字符串
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

}
