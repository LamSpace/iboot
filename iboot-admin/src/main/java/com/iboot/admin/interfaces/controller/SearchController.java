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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.ContentSearchApplicationService;
import com.iboot.kernel.web.PageResult;
import com.iboot.kernel.web.Result;
import com.iboot.admin.infrastructure.search.ContentSearchHit;
import com.iboot.admin.infrastructure.search.ContentSearchResult;
import com.iboot.kernel.security.SecurityUtils;
import com.iboot.admin.interfaces.dto.response.SearchHitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务内容检索接口。
 *
 * <p>面向所有已登录用户，检索已发布公告与当前用户收到的站内消息；不单独设置权限码，
 * 与公告的已发布列表、消息收件箱接口的可见性保持一致。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Tag(name = "业务检索")
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ContentSearchApplicationService contentSearchApplicationService;

    @SuppressWarnings("all")
    public SearchController(final ContentSearchApplicationService contentSearchApplicationService) {
        this.contentSearchApplicationService = contentSearchApplicationService;
    }

    /**
     * 检索公告与站内消息。
     *
     * @param keyword  检索关键字，为空时返回空结果
     * @param type     文档类型：{@code notice} 或 {@code message}，缺省为全部
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量，最大 100
     *
     * @return 分页检索结果
     */
    @Operation(summary = "检索公告与站内消息")
    @GetMapping(version = "1")
    public Result<PageResult<SearchHitResponse>> search(
            @Parameter(description = "检索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "文档类型：notice 或 message") @RequestParam(required = false) String type,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量，最大 100") @RequestParam(defaultValue = "10") Integer pageSize) {
        int page = ContentSearchApplicationService.normalizePageNum(pageNum);
        int size = ContentSearchApplicationService.normalizePageSize(pageSize);
        ContentSearchResult result = contentSearchApplicationService.search(keyword, type,
                SecurityUtils.getCurrentUserId(), page, size);
        List<SearchHitResponse> rows = result.getHits().stream().map(this::convertToResponse).toList();
        return Result.success(new PageResult<>(rows, result.getTotal(), page, size));
    }

    /**
     * 将检索命中项转换为响应对象。
     *
     * @param hit 检索命中项
     *
     * @return 检索命中项响应
     */
    private SearchHitResponse convertToResponse(final ContentSearchHit hit) {
        return new SearchHitResponse(hit.getDocType(), hit.getSourceId(), hit.getTitle(), hit.getHighlights(),
                hit.getUpdatedAt());
    }

}
