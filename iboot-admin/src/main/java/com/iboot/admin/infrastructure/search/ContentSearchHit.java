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

import java.util.List;

/**
 * 业务内容检索命中项。
 *
 * <p>只承载索引中可直接展示的字段与高亮片段，不包含正文全文，避免无前端场景下的过大响应。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ContentSearchHit {

    /**
     * 文档类型编码。
     */
    private final String docType;

    /**
     * 业务主键。
     */
    private final Long sourceId;

    /**
     * 标题。
     */
    private final String title;

    /**
     * 高亮片段，按标题与正文出现顺序拼接。
     */
    private final List<String> highlights;

    /**
     * 更新时间（ISO-8601 字符串）。
     */
    private final String updatedAt;

    /**
     * 创建检索命中项。
     *
     * @param docType    文档类型编码
     * @param sourceId   业务主键
     * @param title      标题
     * @param highlights 高亮片段
     * @param updatedAt  更新时间
     */
    public ContentSearchHit(final String docType, final Long sourceId, final String title, final List<String> highlights,
                            final String updatedAt) {
        this.docType = docType;
        this.sourceId = sourceId;
        this.title = title;
        this.highlights = highlights;
        this.updatedAt = updatedAt;
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
     * @return 业务主键
     */
    public Long getSourceId() {
        return sourceId;
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
     * 获取高亮片段。
     *
     * @return 高亮片段列表
     */
    public List<String> getHighlights() {
        return highlights;
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
