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

import com.iboot.admin.domain.system.model.Message;
import com.iboot.admin.domain.system.model.Notice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 将公告与消息转换为索引文档。
 *
 * <p>转换同时承担可见性判定：未发布或已删除的公告、未发送或已删除的消息不产出文档，
 * 调用方据此决定写入还是移除索引。消息按接收人逐条展开，公告每条一个文档。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ContentDocumentFactory {

    /**
     * 写入索引的时间格式，Elasticsearch 的 {@code strict_date_optional_time} 可直接解析。
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ContentDocumentFactory() {
    }

    /**
     * 将公告转换为索引文档。
     *
     * @param notice 公告实体
     *
     * @return 索引文档；公告未发布或已删除时返回 {@code null}
     */
    public static ContentDocument fromNotice(final Notice notice) {
        if (notice == null || isDeleted(notice.getDeleted()) || !notice.isPublished()) {
            return null;
        }
        return new ContentDocument(ContentDocType.NOTICE, notice.getId(), null, notice.getNoticeTitle(),
                notice.getNoticeContent(), notice.getNoticeType(), notice.getCreateBy(),
                format(notice.getCreateTime()), format(latest(notice.getCreateTime(), notice.getUpdateTime())));
    }

    /**
     * 将消息转换为指定接收人的索引文档。
     *
     * @param message    消息实体
     * @param receiverId 接收人用户 ID
     *
     * @return 索引文档；消息未发送或已删除时返回 {@code null}
     */
    public static ContentDocument fromMessage(final Message message, final Long receiverId) {
        if (message == null || isDeleted(message.getDeleted()) || !message.isSent()) {
            return null;
        }
        return new ContentDocument(ContentDocType.MESSAGE, message.getId(), receiverId, message.getTitle(),
                message.getContent(), message.getMessageType(), message.getCreateBy(),
                format(message.getCreateTime()), format(latest(message.getCreateTime(), message.getUpdateTime())));
    }

    /**
     * 判断逻辑删除标记是否为已删除。
     *
     * @param deleted 逻辑删除标记
     *
     * @return 已删除返回 {@code true}
     */
    private static boolean isDeleted(final Integer deleted) {
        return deleted != null && deleted == 1;
    }

    /**
     * 取较晚的时间，更新时间为空时回退到创建时间。
     *
     * @param createTime 创建时间
     * @param updateTime 更新时间
     *
     * @return 较晚的时间
     */
    private static LocalDateTime latest(final LocalDateTime createTime, final LocalDateTime updateTime) {
        return updateTime != null ? updateTime : createTime;
    }

    /**
     * 将时间格式化为 ISO-8601 字符串。
     *
     * @param time 时间
     *
     * @return ISO-8601 字符串；入参为 {@code null} 时返回 {@code null}
     */
    private static String format(final LocalDateTime time) {
        return time == null ? null : TIME_FORMATTER.format(time);
    }

}
