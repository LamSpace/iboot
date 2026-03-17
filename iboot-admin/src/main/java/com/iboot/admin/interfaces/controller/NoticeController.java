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

import com.iboot.admin.application.service.NoticeApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.Notice;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import com.iboot.admin.interfaces.dto.export.NoticeExportVO;
import com.iboot.admin.interfaces.dto.request.NoticeRequest;
import com.iboot.admin.interfaces.dto.response.NoticeResponse;
import com.iboot.admin.interfaces.dto.response.PublishedNoticeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统公告控制器
 *
 * @author iBoot
 */
@Tag(name = "系统公告", description = "系统公告相关接口")
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeApplicationService noticeApplicationService;

    @SuppressWarnings("all")
    public NoticeController(final NoticeApplicationService noticeApplicationService) {
        this.noticeApplicationService = noticeApplicationService;
    }

    // ==================== 后台管理接口 ====================
    @Operation(summary = "查询公告列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('notice:list')")
    public Result<PageResult<NoticeResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String noticeTitle,
                                                   @RequestParam(required = false) String noticeType,
                                                   @RequestParam(required = false) String status) {
        List<Notice> notices = noticeApplicationService.getNoticePage(noticeTitle, noticeType, status, pageNum,
                pageSize);
        long total = noticeApplicationService.countNotices(noticeTitle, noticeType, status);
        List<NoticeResponse> responses = notices.stream().map(this::convertToResponse).collect(Collectors.toList());
        PageResult<NoticeResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "查询公告详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('notice:query')")
    public Result<NoticeResponse> getById(@PathVariable Long id) {
        Notice notice = noticeApplicationService.getNoticeById(id);
        return Result.success(convertToResponse(notice));
    }

    @Operation(summary = "新增公告")
    @PostMapping
    @PreAuthorize("hasAuthority('notice:add')")
    @Log(title = "系统公告", businessType = BusinessTypeEnum.INSERT)
    public Result<NoticeResponse> add(@Valid @RequestBody NoticeRequest request) {
        Notice notice = convertToEntity(request);
        Notice created = noticeApplicationService.createNotice(notice);
        return Result.success(convertToResponse(created));
    }

    @Operation(summary = "修改公告")
    @PutMapping
    @PreAuthorize("hasAuthority('notice:edit')")
    @Log(title = "系统公告", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody NoticeRequest request) {
        Notice notice = convertToEntity(request);
        notice.setId(request.getId());
        noticeApplicationService.updateNotice(notice);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('notice:remove')")
    @Log(title = "系统公告", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        noticeApplicationService.deleteNotice(id);
        return Result.success();
    }

    @Operation(summary = "发布公告")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('notice:publish')")
    @Log(title = "系统公告", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> publish(@PathVariable Long id) {
        noticeApplicationService.publishNotice(id);
        return Result.success();
    }

    @Operation(summary = "撤回公告")
    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('notice:publish')")
    @Log(title = "系统公告", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> revoke(@PathVariable Long id) {
        noticeApplicationService.revokeNotice(id);
        return Result.success();
    }

    @Operation(summary = "导出公告列表")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('notice:export')")
    @Log(title = "系统公告", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String noticeTitle,
                       @RequestParam(required = false) String noticeType, @RequestParam(required = false) String status)
            throws IOException {
        List<Notice> notices = noticeApplicationService.getAllNoticesByCondition(noticeTitle, noticeType, status);
        List<NoticeExportVO> exportList = notices.stream().map(this::convertToExportVO).collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, NoticeExportVO.class, "公告列表", "公告数据");
    }

    // ==================== 用户端接口 ====================
    @Operation(summary = "获取已发布公告列表")
    @GetMapping("/published")
    public Result<List<PublishedNoticeResponse>> published() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Notice> notices = noticeApplicationService.getPublishedNotices();
        List<PublishedNoticeResponse> responses = notices.stream()
                .map(notice -> convertToPublishedResponse(notice, userId))
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "获取未读公告数量")
    @GetMapping("/unread/count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        long count = noticeApplicationService.getUnreadCount(userId);
        return Result.success(count);
    }

    @Operation(summary = "标记公告已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        noticeApplicationService.markAsRead(id, userId);
        return Result.success();
    }

    // ==================== 转换方法 ====================
    private Notice convertToEntity(NoticeRequest request) {
        return Notice.builder()
                .noticeTitle(request.getNoticeTitle())
                .noticeType(request.getNoticeType())
                .noticeContent(request.getNoticeContent())
                .isTop(request.getIsTop())
                .remark(request.getRemark())
                .build();
    }

    private NoticeResponse convertToResponse(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .noticeType(notice.getNoticeType())
                .noticeContent(notice.getNoticeContent())
                .status(notice.getStatus())
                .isTop(notice.getIsTop())
                .createBy(notice.getCreateBy())
                .createTime(notice.getCreateTime())
                .remark(notice.getRemark())
                .build();
    }

    private PublishedNoticeResponse convertToPublishedResponse(Notice notice, Long userId) {
        return PublishedNoticeResponse.builder()
                .id(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .noticeType(notice.getNoticeType())
                .noticeContent(notice.getNoticeContent())
                .isTop(notice.getIsTop())
                .createBy(notice.getCreateBy())
                .createTime(notice.getCreateTime())
                .isRead(noticeApplicationService.isRead(notice.getId(), userId))
                .build();
    }

    private NoticeExportVO convertToExportVO(Notice notice) {
        return NoticeExportVO.builder()
                .id(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .noticeType(notice.getNoticeType())
                .noticeContent(notice.getNoticeContent())
                .status(notice.getStatus())
                .isTop(notice.getIsTop())
                .createBy(notice.getCreateBy())
                .remark(notice.getRemark())
                .createTime(notice.getCreateTime())
                .build();
    }

}
