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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * {@link ContentIndexListener} 的单元测试。
 *
 * <p>验证三类动作的分派，以及 Elasticsearch 故障不外抛给业务调用方。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ContentIndexListenerTest {

    @Mock
    private ContentIndexUpdater contentIndexUpdater;

    @Mock
    private ElasticsearchContentIndex contentIndex;

    private ContentIndexListener listener;

    @BeforeEach
    void setUp() {
        listener = new ContentIndexListener(contentIndexUpdater, contentIndex);
    }

    /**
     * 公告刷新事件应刷新公告索引。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldRefreshNotice() throws IOException {
        listener.handle(ContentIndexEvent.refreshNotice(this, 1L));

        verify(contentIndexUpdater).refreshNotice(1L);
    }

    /**
     * 消息刷新事件应刷新消息索引。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldRefreshMessage() throws IOException {
        listener.handle(ContentIndexEvent.refreshMessage(this, 5L));

        verify(contentIndexUpdater).refreshMessage(5L);
    }

    /**
     * 整条移除事件应删除源数据对应的全部文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldDeleteSource() throws IOException {
        listener.handle(ContentIndexEvent.deleteMessage(this, 5L));

        verify(contentIndex).deleteBySource(ContentDocType.MESSAGE, 5L);
    }

    /**
     * 接收人移除事件应删除单个文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldDeleteReceiver() throws IOException {
        listener.handle(ContentIndexEvent.deleteReceiver(this, 5L, 7L));

        verify(contentIndex).delete(ContentDocType.MESSAGE, 5L, 7L);
    }

    /**
     * 索引故障不应向调用方传播。
     *
     * @throws IOException 模拟故障时抛出
     */
    @Test
    void shouldSwallowIndexFailure() throws IOException {
        doThrow(new IOException("es down")).when(contentIndexUpdater).refreshNotice(1L);

        assertThatCode(() -> listener.handle(ContentIndexEvent.refreshNotice(this, 1L)))
                .doesNotThrowAnyException();
    }

}
