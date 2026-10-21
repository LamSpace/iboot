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

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.iboot.admin.infrastructure.search.ContentDocType;
import com.iboot.admin.infrastructure.search.ContentSearchResult;
import com.iboot.admin.infrastructure.search.ElasticsearchContentIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 业务内容检索应用服务。
 *
 * <p>负责检索参数校验、类型解析与降级：空白关键字直接返回空结果，不执行全量查询；非法类型抛出
 * 参数异常；检索后端不可用时返回空结果，不向调用方抛出。消息的接收人过滤由调用方传入当前用户 ID，
 * 在 {@link ElasticsearchContentIndex} 的查询中强制施加。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class ContentSearchApplicationService {

    /**
     * 单页最大返回数量。
     */
    public static final int MAX_PAGE_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(ContentSearchApplicationService.class);

    private final ElasticsearchContentIndex contentIndex;

    @SuppressWarnings("all")
    public ContentSearchApplicationService(final ElasticsearchContentIndex contentIndex) {
        this.contentIndex = contentIndex;
    }

    /**
     * 检索公告与站内消息。
     *
     * @param keyword    检索关键字，空白时返回空结果
     * @param type       文档类型：{@code notice}、{@code message}，为空时检索全部
     * @param receiverId 当前用户 ID，用于过滤消息
     * @param pageNum    页码，从 1 开始
     * @param pageSize   每页数量
     *
     * @return 检索结果
     * @throws IllegalArgumentException 类型参数不受支持时抛出
     */
    public ContentSearchResult search(final String keyword, final String type, final Long receiverId, final int pageNum,
                                      final int pageSize) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return new ContentSearchResult(List.of(), 0);
        }
        Set<ContentDocType> types = resolveTypes(type);
        int page = normalizePageNum(pageNum);
        int size = normalizePageSize(pageSize);
        try {
            return contentIndex.search(types, normalizedKeyword, receiverId, (page - 1) * size, size);
        } catch (IOException | ElasticsearchException e) {
            log.error("业务内容检索失败，keyword: {}, type: {}", normalizedKeyword, type, e);
            return new ContentSearchResult(List.of(), 0);
        }
    }

    /**
     * 将页码规范到合法范围。
     *
     * @param pageNum 原始页码
     *
     * @return 不小于 1 的页码
     */
    public static int normalizePageNum(final int pageNum) {
        return Math.max(pageNum, 1);
    }

    /**
     * 将每页数量规范到合法范围。
     *
     * @param pageSize 原始每页数量
     *
     * @return 1 到 {@value #MAX_PAGE_SIZE} 之间的数量
     */
    public static int normalizePageSize(final int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    /**
     * 解析类型参数。
     *
     * @param type 类型参数，为空时表示全部类型
     *
     * @return 文档类型集合
     * @throws IllegalArgumentException 类型参数不受支持时抛出
     */
    private Set<ContentDocType> resolveTypes(final String type) {
        if (type == null || type.isBlank()) {
            return Set.of(ContentDocType.values());
        }
        String normalized = type.trim();
        for (ContentDocType docType : ContentDocType.values()) {
            if (docType.getCode().equalsIgnoreCase(normalized)) {
                return Set.of(docType);
            }
        }
        throw new IllegalArgumentException("不支持的检索类型: " + type);
    }

}
