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
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ContentDocumentFactory} 的单元测试。
 *
 * <p>验证公告与消息在不同状态下是否产出索引文档，以及确定性文档 ID 的生成规则。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ContentDocumentFactoryTest {

    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 10, 21, 8, 30);

    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2026, 10, 21, 9, 0);

    /**
     * 已发布公告应产出公告文档。
     */
    @Test
    void shouldBuildNoticeDocumentWhenPublished() {
        Notice notice = notice("1", 0);

        ContentDocument document = ContentDocumentFactory.fromNotice(notice);

        assertThat(document).isNotNull();
        assertThat(document.getDocType()).isEqualTo("notice");
        assertThat(document.getSourceId()).isEqualTo(1L);
        assertThat(document.getReceiverId()).isNull();
        assertThat(document.getTitle()).isEqualTo("年度公告");
        assertThat(document.getContent()).isEqualTo("年度绩效评估安排");
        assertThat(document.getCategory()).isEqualTo("1");
        assertThat(document.getCreatedAt()).isEqualTo("2026-10-21T08:30:00");
        assertThat(document.getUpdatedAt()).isEqualTo("2026-10-21T09:00:00");
        assertThat(document.documentId()).isEqualTo("notice-1");
    }

    /**
     * 草稿、已撤回与已删除公告不应产出文档。
     */
    @Test
    void shouldSkipNoticeWhenNotVisible() {
        assertThat(ContentDocumentFactory.fromNotice(notice("0", 0))).isNull();
        assertThat(ContentDocumentFactory.fromNotice(notice("2", 0))).isNull();
        assertThat(ContentDocumentFactory.fromNotice(notice("1", 1))).isNull();
    }

    /**
     * 已发送消息应按接收人展开为独立文档。
     */
    @Test
    void shouldBuildMessageDocumentPerReceiver() {
        Message message = message("1", 0);

        ContentDocument document = ContentDocumentFactory.fromMessage(message, 7L);

        assertThat(document).isNotNull();
        assertThat(document.getDocType()).isEqualTo("message");
        assertThat(document.getSourceId()).isEqualTo(5L);
        assertThat(document.getReceiverId()).isEqualTo(7L);
        assertThat(document.getTitle()).isEqualTo("系统升级");
        assertThat(document.getContent()).isEqualTo("今晚升级 Redis");
        assertThat(document.documentId()).isEqualTo("message-5-7");
    }

    /**
     * 草稿、已撤回与已删除消息不应产出文档。
     */
    @Test
    void shouldSkipMessageWhenNotVisible() {
        assertThat(ContentDocumentFactory.fromMessage(message("0", 0), 7L)).isNull();
        assertThat(ContentDocumentFactory.fromMessage(message("2", 0), 7L)).isNull();
        assertThat(ContentDocumentFactory.fromMessage(message("1", 1), 7L)).isNull();
    }

    /**
     * 消息文档缺少接收人时应拒绝生成 ID。
     */
    @Test
    void shouldRejectMessageDocumentWithoutReceiver() {
        assertThatThrownBy(() -> ContentDocument.documentId(ContentDocType.MESSAGE, 5L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 创建测试用公告。
     *
     * @param status  状态
     * @param deleted 逻辑删除标记
     *
     * @return 公告实体
     */
    private Notice notice(final String status, final int deleted) {
        return Notice.builder()
                .id(1L)
                .noticeTitle("年度公告")
                .noticeType("1")
                .noticeContent("年度绩效评估安排")
                .status(status)
                .isTop(0)
                .createBy("admin")
                .createTime(CREATE_TIME)
                .updateTime(UPDATE_TIME)
                .deleted(deleted)
                .build();
    }

    /**
     * 创建测试用消息。
     *
     * @param status  状态
     * @param deleted 逻辑删除标记
     *
     * @return 消息实体
     */
    private Message message(final String status, final int deleted) {
        return Message.builder()
                .id(5L)
                .title("系统升级")
                .content("今晚升级 Redis")
                .messageType("1")
                .status(status)
                .createBy("admin")
                .createTime(CREATE_TIME)
                .updateTime(UPDATE_TIME)
                .deleted(deleted)
                .build();
    }

}
