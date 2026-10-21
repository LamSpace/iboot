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

package com.iboot.admin.application.service;

import com.iboot.admin.infrastructure.search.ContentDocType;
import com.iboot.admin.infrastructure.search.ContentSearchHit;
import com.iboot.admin.infrastructure.search.ContentSearchResult;
import com.iboot.admin.infrastructure.search.ElasticsearchContentIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ContentSearchApplicationService} 的单元测试。
 *
 * <p>验证空白关键字、类型校验、参数归一化与检索后端故障降级。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ContentSearchApplicationServiceTest {

    @Mock
    private ElasticsearchContentIndex contentIndex;

    private ContentSearchApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ContentSearchApplicationService(contentIndex);
    }

    /**
     * 空白关键字不执行检索。
     */
    @Test
    void shouldReturnEmptyWhenKeywordBlank() {
        ContentSearchResult result = service.search("   ", null, 7L, 1, 10);

        assertThat(result.getHits()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verifyNoInteractions(contentIndex);
    }

    /**
     * 不支持的检索类型应抛出参数异常。
     */
    @Test
    void shouldRejectUnsupportedType() {
        assertThatThrownBy(() -> service.search("绩效", "file", 7L, 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("file");
    }

    /**
     * 合法类型应转换为索引类型并透传分页参数。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldSearchWithResolvedType() throws IOException {
        ContentSearchHit hit = new ContentSearchHit("notice", 1L, "年度公告", List.of("<em>绩效</em>"), null);
        when(contentIndex.search(Set.of(ContentDocType.NOTICE), "绩效", 7L, 10, 10))
                .thenReturn(new ContentSearchResult(List.of(hit), 1));

        ContentSearchResult result = service.search(" 绩效 ", "notice", 7L, 2, 10);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getHits()).containsExactly(hit);
        verify(contentIndex).search(Set.of(ContentDocType.NOTICE), "绩效", 7L, 10, 10);
    }

    /**
     * 未指定类型时应检索公告与消息。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldSearchAllTypesWhenTypeMissing() throws IOException {
        when(contentIndex.search(Set.of(ContentDocType.NOTICE, ContentDocType.MESSAGE), "绩效", 7L, 0, 10))
                .thenReturn(new ContentSearchResult(List.of(), 0));

        ContentSearchResult result = service.search("绩效", null, 7L, 1, 10);

        assertThat(result.getTotal()).isZero();
        verify(contentIndex).search(Set.of(ContentDocType.NOTICE, ContentDocType.MESSAGE), "绩效", 7L, 0, 10);
    }

    /**
     * 检索后端故障时应降级为空结果。
     *
     * @throws IOException 模拟故障时抛出
     */
    @Test
    void shouldFallbackWhenIndexUnavailable() throws IOException {
        when(contentIndex.search(Set.of(ContentDocType.NOTICE), "绩效", 7L, 0, 10))
                .thenThrow(new IOException("es down"));

        ContentSearchResult result = service.search("绩效", "notice", 7L, 1, 10);

        assertThat(result.getHits()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    /**
     * 分页参数应归一化到合法范围。
     */
    @Test
    void shouldNormalizePaging() {
        assertThat(ContentSearchApplicationService.normalizePageNum(0)).isEqualTo(1);
        assertThat(ContentSearchApplicationService.normalizePageSize(0)).isEqualTo(1);
        assertThat(ContentSearchApplicationService.normalizePageSize(1000))
                .isEqualTo(ContentSearchApplicationService.MAX_PAGE_SIZE);
    }

}
