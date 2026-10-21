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

import com.iboot.admin.application.service.ApprovalApplicationService;
import com.iboot.admin.application.service.ApprovalDetail;
import com.iboot.admin.application.service.ApprovalItem;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.LeaveStatus;
import com.iboot.admin.domain.workflow.model.WorkflowHistoryItem;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import com.iboot.admin.interfaces.dto.request.ApprovalDecisionRequest;
import com.iboot.admin.interfaces.dto.request.LeaveApplyRequest;
import com.iboot.admin.interfaces.dto.response.ApprovalDetailResponse;
import com.iboot.admin.interfaces.dto.response.ApprovalHistoryResponse;
import com.iboot.admin.interfaces.dto.response.ApprovalItemResponse;
import com.iboot.admin.interfaces.dto.response.LeaveApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

/**
 * 审批中心控制器。
 *
 * <p>提供请假发起、待办/已办/我的申请查询、详情与通过/驳回接口。发起与自助查询使用
 * {@code approval:apply}，审批操作使用 {@code approval:approve}，管理员通过
 * {@code approval:list} 或 {@code approval:query} 查看全部数据。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see ApprovalApplicationService
 */
@Tag(name = "审批中心", description = "请假审批相关接口")
@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private static final String PERMISSION_VIEW_ALL = "approval:list";

    private static final String PERMISSION_QUERY_ALL = "approval:query";

    private final ApprovalApplicationService approvalApplicationService;

    @SuppressWarnings("all")
    public ApprovalController(final ApprovalApplicationService approvalApplicationService) {
        this.approvalApplicationService = approvalApplicationService;
    }

    /**
     * 发起请假申请。
     *
     * @param request 请假请求
     * @return 创建后的申请
     */
    @Operation(summary = "发起请假")
    @PostMapping(version = "1", value = "/leave")
    @PreAuthorize("hasAuthority('approval:apply')")
    @Log(title = "审批中心", businessType = BusinessTypeEnum.INSERT)
    public Result<LeaveApplicationResponse> apply(@Valid @RequestBody LeaveApplyRequest request) {
        LeaveApplication application = approvalApplicationService.submit(
                SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentUsername(), request.getApproverId(),
                request.getLeaveType(), request.getStartTime(), request.getEndTime(), request.getReason());
        LeaveApplicationResponse response = new LeaveApplicationResponse();
        fillApplication(response, application, null, null);
        return Result.success(response);
    }

    /**
     * 查询当前用户的待办。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 待办分页结果
     */
    @Operation(summary = "查询我的待办")
    @GetMapping(version = "1", value = "/todo")
    @PreAuthorize("hasAuthority('approval:approve')")
    public Result<PageResult<ApprovalItemResponse>> todo(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ApprovalItem> page = approvalApplicationService.findTodo(
                SecurityUtils.getCurrentUserId(), pageNum, pageSize);
        return Result.success(toItemPage(page));
    }

    /**
     * 查询当前用户的已办。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 已办分页结果
     */
    @Operation(summary = "查询我的已办")
    @GetMapping(version = "1", value = "/done")
    @PreAuthorize("hasAuthority('approval:approve')")
    public Result<PageResult<ApprovalItemResponse>> done(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ApprovalItem> page = approvalApplicationService.findDone(
                SecurityUtils.getCurrentUserId(), pageNum, pageSize);
        return Result.success(toItemPage(page));
    }

    /**
     * 查询当前用户提交的申请。
     *
     * @param status   状态筛选，可为 {@code null}
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 申请分页结果
     */
    @Operation(summary = "查询我的申请")
    @GetMapping(version = "1", value = "/mine")
    @PreAuthorize("hasAuthority('approval:apply')")
    public Result<PageResult<ApprovalItemResponse>> mine(@RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ApprovalItem> page = approvalApplicationService.findMine(
                SecurityUtils.getCurrentUserId(), parseStatus(status), pageNum, pageSize);
        return Result.success(toItemPage(page));
    }

    /**
     * 查询申请详情与审批历史。
     *
     * @param id 申请 ID
     * @return 详情
     */
    @Operation(summary = "查询申请详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<ApprovalDetailResponse> detail(@PathVariable Long id) {
        ApprovalDetail detail = approvalApplicationService.detail(id, SecurityUtils.getCurrentUserId(),
                canViewAll());
        ApprovalDetailResponse response = new ApprovalDetailResponse();
        LeaveApplicationResponse applicationResponse = new LeaveApplicationResponse();
        fillApplication(applicationResponse, detail.getApplication(), detail.getApplicantName(),
                detail.getApproverName());
        response.setApplication(applicationResponse);
        response.setHistory(detail.getHistory().stream()
                .map(item -> toHistoryResponse(item, detail))
                .toList());
        return Result.success(response);
    }

    /**
     * 审批通过。
     *
     * @param id      申请 ID
     * @param request 审批意见
     * @return 空结果
     */
    @Operation(summary = "审批通过")
    @PostMapping(version = "1", value = "/{id}/approve")
    @PreAuthorize("hasAuthority('approval:approve')")
    @Log(title = "审批中心", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> approve(@PathVariable Long id,
                                @Valid @RequestBody ApprovalDecisionRequest request) {
        approvalApplicationService.approve(id, SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(), request.getComment());
        return Result.success();
    }

    /**
     * 审批驳回。
     *
     * @param id      申请 ID
     * @param request 驳回原因
     * @return 空结果
     */
    @Operation(summary = "审批驳回")
    @PostMapping(version = "1", value = "/{id}/reject")
    @PreAuthorize("hasAuthority('approval:approve')")
    @Log(title = "审批中心", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> reject(@PathVariable Long id,
                               @Valid @RequestBody ApprovalDecisionRequest request) {
        approvalApplicationService.reject(id, SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(), request.getComment());
        return Result.success();
    }

    private PageResult<ApprovalItemResponse> toItemPage(final PageResult<ApprovalItem> page) {
        return new PageResult<>(page.getData().stream().map(this::toItemResponse).toList(),
                page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    private ApprovalItemResponse toItemResponse(final ApprovalItem item) {
        ApprovalItemResponse response = new ApprovalItemResponse();
        fillApplication(response, item.getApplication(), item.getApplicantName(), item.getApproverName());
        if (item.getTask() != null) {
            response.setTaskId(item.getTask().getTaskId());
            response.setTaskName(item.getTask().getTaskName());
            response.setTaskCreateTime(item.getTask().getCreateTime());
        }
        if (item.getHistory() != null) {
            response.setCompletedTime(item.getHistory().getTime());
        }
        return response;
    }

    private ApprovalHistoryResponse toHistoryResponse(final WorkflowHistoryItem item,
                                                      final ApprovalDetail detail) {
        ApprovalHistoryResponse response = new ApprovalHistoryResponse();
        response.setActivityName(item.getActivityName());
        response.setActorId(item.getActorId());
        response.setApproved(item.getApproved());
        response.setComment(item.getComment());
        response.setTime(item.getTime());
        response.setActorName("提交申请".equals(item.getActivityName())
                ? detail.getApplicantName() : detail.getApproverName());
        return response;
    }

    private void fillApplication(final LeaveApplicationResponse response, final LeaveApplication application,
                                 final String applicantName, final String approverName) {
        response.setId(application.getId());
        response.setApplicantId(application.getApplicantId());
        response.setApplicantName(applicantName);
        response.setApproverId(application.getApproverId());
        response.setApproverName(approverName);
        response.setLeaveType(application.getLeaveType());
        response.setStartTime(application.getStartTime());
        response.setEndTime(application.getEndTime());
        response.setReason(application.getReason());
        response.setStatus(application.getStatus() == null ? null : application.getStatus().name());
        response.setApproveComment(application.getApproveComment());
        response.setCreateTime(application.getCreateTime());
    }

    private LeaveStatus parseStatus(final String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return LeaveStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "非法的申请状态");
        }
    }

    private boolean canViewAll() {
        Authentication authentication = SecurityUtils.getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> PERMISSION_VIEW_ALL.equals(authority)
                        || PERMISSION_QUERY_ALL.equals(authority));
    }
}
