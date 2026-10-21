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

package com.iboot.admin.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 业务内容检索命中项响应。
 *
 * <p>只返回展示所需的标题、类型、业务 ID、高亮片段与更新时间，不返回正文全文。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "业务内容检索命中项")
public class SearchHitResponse {

    /**
     * 文档类型：notice 或 message。
     */
    @Schema(description = "文档类型：notice 或 message")
    private String docType;

    /**
     * 业务主键：公告 ID 或消息 ID。
     */
    @Schema(description = "业务主键：公告 ID 或消息 ID")
    private Long sourceId;

    /**
     * 标题。
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 高亮片段，标题片段排在正文片段之前。
     */
    @Schema(description = "高亮片段")
    private List<String> highlights;

    /**
     * 更新时间（ISO-8601 字符串）。
     */
    @Schema(description = "更新时间")
    private String updatedAt;

    /**
     * 创建检索命中项响应。
     *
     * @param docType    文档类型
     * @param sourceId   业务主键
     * @param title      标题
     * @param highlights 高亮片段
     * @param updatedAt  更新时间
     */
    public SearchHitResponse(final String docType, final Long sourceId, final String title,
                             final List<String> highlights, final String updatedAt) {
        this.docType = docType;
        this.sourceId = sourceId;
        this.title = title;
        this.highlights = highlights;
        this.updatedAt = updatedAt;
    }

    /**
     * 获取文档类型。
     *
     * @return 文档类型
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
